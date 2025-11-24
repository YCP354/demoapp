import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

object PclmGenerator {

    // HP 105a 的物理分辨率宽度 (600 DPI)
    private const val PRINTER_WIDTH_PX = 4958
    // 分条高度 (每次处理 128 行，仅占用约 2.5MB 内存)
    private const val STRIP_HEIGHT_PX = 128

    fun generatePclmPdf(sourceBitmap: Bitmap, outputFile: File): File {
        val fos = FileOutputStream(outputFile)
        val writer = PdfWriter(fos)

        // 1. 计算目标尺寸
        val scale = PRINTER_WIDTH_PX.toFloat() / sourceBitmap.width
        val targetHeight = (sourceBitmap.height * scale).toInt()

        // 2. 写入 PDF 头
        writer.writeLine("%PDF-1.7")
        writer.writeLine("%PCLm 1.0")
        writer.writeLine("%Gen: Optimized Strip Generator")

        // 3. 基础对象
        writer.startObject() // Obj 1: Catalog
        writer.writeLine("<< /Type /Catalog /Pages 2 0 R >>")
        writer.endObject()

        writer.startObject() // Obj 2: Pages
        writer.writeLine("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        writer.endObject()

        writer.startObject() // Obj 3: Page
        writer.writeLine("<<")
        writer.writeLine("  /Type /Page")
        writer.writeLine("  /Parent 2 0 R")
        writer.writeLine("  /MediaBox [0 0 595 842]")
        writer.writeLine("  /Resources 4 0 R")
        writer.writeLine("  /Contents 5 0 R")
        writer.writeLine(">>")
        writer.endObject()

        // ---------------------------------------------------------
        // 4. 动态分条处理 (核心优化：边切边压缩，不占大内存)
        // ---------------------------------------------------------

        val totalStrips = (targetHeight + STRIP_HEIGHT_PX - 1) / STRIP_HEIGHT_PX
        val stripBlobs = mutableListOf<ByteArray>()

        // 准备一个小画布，用于处理切片
        // 只有 4958 * 128 * 4 bytes ≈ 2.5 MB，非常安全
        val stripBitmap = Bitmap.createBitmap(PRINTER_WIDTH_PX, STRIP_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(stripBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG) // 开启抗锯齿

        for (i in 0 until totalStrips) {
            val yOffset = i * STRIP_HEIGHT_PX
            // 计算当前条的高度 (最后一条可能不够 128)
            var currentStripH = STRIP_HEIGHT_PX
            if (yOffset + currentStripH > targetHeight) {
                currentStripH = targetHeight - yOffset
            }

            // --- 核心：使用 Matrix 从原图中“抠”出这一条并缩放 ---
            canvas.drawColor(0xFFFFFFFF.toInt()) //以此填充白色背景

            canvas.save()
            // 1. 先缩放
            canvas.scale(scale, scale)
            // 2. 再向上平移，把当前要画的区域移到 stripBitmap 的视口内
            // 注意：因为是先缩放后平移，平移量是基于原图坐标的负值
            canvas.translate(0f, -(yOffset / scale))

            canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)
            canvas.restore()

            // --- 提取像素并压缩 ---
            // 注意：如果最后一条不够高，我们需要只提取有效区域
            val pixels = IntArray(PRINTER_WIDTH_PX * currentStripH)
            stripBitmap.getPixels(pixels, 0, PRINTER_WIDTH_PX, 0, 0, PRINTER_WIDTH_PX, currentStripH)

            stripBlobs.add(compressRleGray(pixels, PRINTER_WIDTH_PX, currentStripH))
        }

        // 释放临时画布
        stripBitmap.recycle()
        // 注意：sourceBitmap 不要在这里 recycle，由调用者(Activity)决定

        // ---------------------------------------------------------
        // 5. 写入 PDF 结构 (Resources -> Content -> Images)
        // ---------------------------------------------------------

        // Obj 4: Resources
        writer.startObject()
        writer.writeLine("<< /XObject <<")
        val firstImageId = 6
        for (i in stripBlobs.indices) {
            val id = firstImageId + i
            writer.writeLine("  /Im$id $id 0 R")
        }
        writer.writeLine(">> >>")
        writer.endObject()

        // Obj 5: Content Stream (拼图)
        val sb = StringBuilder()
        sb.append("q ")
        val pdfPageHeight = 842.0
        val pdfPageWidth = 595.0

        for (i in stripBlobs.indices) {
            val h = if (i == stripBlobs.size - 1) targetHeight - (i * STRIP_HEIGHT_PX) else STRIP_HEIGHT_PX
            val drawH = (h.toDouble() / targetHeight) * pdfPageHeight
            val yOffsetPx = i * STRIP_HEIGHT_PX
            val drawY = pdfPageHeight - ((yOffsetPx.toDouble() / targetHeight) * pdfPageHeight) - drawH
            val id = firstImageId + i
            // 确保使用 Locale.US 防止生成逗号小数点
            sb.append(String.format(Locale.US, "q %.2f 0 0 %.2f 0 %.2f cm /Im%d Do Q\n", pdfPageWidth, drawH, drawY, id))
        }
        sb.append("Q")

        val contentBytes = sb.toString()
        writer.startObject()
        writer.writeLine("<< /Length ${contentBytes.length} >>")
        writer.writeLine("stream")
        writer.writeLine(contentBytes)
        writer.writeLine("endstream")
        writer.endObject()

        // Obj 6+: Images
        for (i in stripBlobs.indices) {
            val blob = stripBlobs[i]
            val h = if (i == stripBlobs.size - 1) targetHeight - (i * STRIP_HEIGHT_PX) else STRIP_HEIGHT_PX

            writer.startObject()
            writer.writeLine("<<")
            writer.writeLine("  /Type /XObject")
            writer.writeLine("  /Subtype /Image")
            writer.writeLine("  /Width $PRINTER_WIDTH_PX")
            writer.writeLine("  /Height $h")
            writer.writeLine("  /ColorSpace /DeviceGray")
            writer.writeLine("  /BitsPerComponent 8")
            writer.writeLine("  /Filter /RunLengthDecode")
            writer.writeLine("  /Length ${blob.size}")
            writer.writeLine(">>")
            writer.writeLine("stream")
            writer.flush()
            fos.write(blob)
            writer.writeLine("")
            writer.writeLine("endstream")
            writer.endObject()
        }

        // Trailer
        val xrefOffset = writer.bytesWritten
        writer.writeLine("xref")
        writer.writeLine("0 ${writer.objectOffsets.size + 1}")
        writer.writeLine("0000000000 65535 f ")
        for (offset in writer.objectOffsets) {
            writer.writeLine(String.format(Locale.US, "%010d 00000 n ", offset))
        }
        writer.writeLine("trailer")
        writer.writeLine("<< /Size ${writer.objectOffsets.size + 1} /Root 1 0 R >>")
        writer.writeLine("startxref")
        writer.writeLine("$xrefOffset")
        writer.writeLine("%%EOF")

        fos.close()
        return outputFile
    }

    // 优化的 RLE 压缩 (PackBits)
    private fun compressRleGray(pixels: IntArray, w: Int, h: Int): ByteArray {
        val output = ByteArrayOutputStream(w * h / 2) // 预估压缩率

        // 1. 转灰度 (Buffer)
        val grayData = ByteArray(w * h)
        for (i in pixels.indices) {
            val c = pixels[i]
            // Gray = 0.299R + 0.587G + 0.114B
            val gray = (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)).toInt()
            grayData[i] = gray.toByte()
        }

        // 2. 压缩
        var i = 0
        val len = grayData.size
        while (i < len) {
            var runLen = 0
            // 查找重复块 (最多 128字节)
            while (i + runLen + 1 < len && runLen < 127 &&
                grayData[i + runLen] == grayData[i + runLen + 1]) {
                runLen++
            }

            if (runLen > 0) { // 至少2个相同才算 Run
                output.write(257 - (runLen + 1))
                output.write(grayData[i].toInt())
                i += runLen + 1
            } else {
                // 查找非重复块
                var litLen = 0
                while (i + litLen < len && litLen < 128) {
                    if (i + litLen + 1 < len && grayData[i + litLen] == grayData[i + litLen + 1]) break
                    litLen++
                }
                output.write(litLen - 1)
                output.write(grayData, i, litLen)
                i += litLen
            }
        }
        output.write(128) // EOD
        return output.toByteArray()
    }

    private class PdfWriter(private val stream: FileOutputStream) {
        var bytesWritten: Long = 0
        val objectOffsets = mutableListOf<Long>()
        fun startObject() {
            objectOffsets.add(bytesWritten)
            writeLine("${objectOffsets.size} 0 obj")
        }
        fun endObject() = writeLine("endobj")
        fun writeLine(s: String) {
            val bytes = (s + "\n").toByteArray(StandardCharsets.US_ASCII)
            stream.write(bytes)
            bytesWritten += bytes.size
        }
        fun flush() = stream.flush()
    }
}