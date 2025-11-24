import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PclmGenerator {

    // 严格遵守金样文件宽度
    private const val PRINTER_WIDTH_PX = 4958
    // 严格遵守金样文件分条高度
    private const val STRIP_HEIGHT_PX = 116

    fun generatePclmPdf(sourceBitmap: Bitmap, outputFile: File): File {
        val fos = FileOutputStream(outputFile)
        val writer = PdfWriter(fos)

        val scale = PRINTER_WIDTH_PX.toFloat() / sourceBitmap.width
        val targetHeight = (sourceBitmap.height * scale).toInt()

        // --- 1. Header (像素级复刻) ---
        // 注意：根据你的诊断日志，Ref 的 genPCLm 前面有两个空格
        // 我们这里使用 StringBuilder 精确构建，确保每一个字节都对上
        // 且统一使用 \n (0x0A) 还是 \r\n (0x0D 0x0A)?
        // 你的诊断日志里显示 Header 是一行的，说明 Analyzer 替换了换行符。
        // 通常 PDF 标准是 %PDF-1.7 后面紧跟换行。
        // 我们这里使用标准 PDF 换行，并严格补齐空格。

        writer.writeRaw("%PDF-1.7\n")
        writer.writeRaw("%PCLm 1.0\n")
        writer.writeRaw("%  genPCLm (Ver: 0.930000)\n") // 补上两个空格
        writer.writeRaw("%============= Job Ticket =============\n")
        writer.writeRaw("% PCLmS-Job-Ticket\n")
        writer.writeRaw("%      job-ticket-version: 0.1\n")
        writer.writeRaw("%      epcl-version: 1.01\n")
        writer.writeRaw("%    JobSection\n")
        writer.writeRaw("%      job-id: 1\n")

        val dateStr = SimpleDateFormat("EEE MMM dd HH:mm:ss:SSS yyyy", Locale.US).format(Date())
        writer.writeRaw("%      job-start-time: $dateStr\n")

        writer.writeRaw("%    MediaHandlingSection\n")
        writer.writeRaw("%      media-size-name: iso_a4_210x297mm\n")
        writer.writeRaw("%      media-type: Stationery\n")
        writer.writeRaw("%      media-source: tray_1\n")
        writer.writeRaw("%      sides: one-sided\n")
        writer.writeRaw("%      output-bin: top_output\n")
        writer.writeRaw("%    RenderingSection\n")
        writer.writeRaw("%      pclm-compression-method: RLE\n")
        writer.writeRaw("%      strip-height: $STRIP_HEIGHT_PX\n")
        writer.writeRaw("%End\n")

        // --- 2. Objects ---

        // Obj 1: Catalog
        writer.startObject()
        writer.writeRaw("<< /Type /Catalog /Pages 2 0 R >>\n")
        writer.endObject()

        // Obj 2: Pages
        writer.startObject()
        writer.writeRaw("<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n")
        writer.endObject()

        // Obj 3: Page
        writer.startObject()
        writer.writeRaw("<<\n")
        writer.writeRaw("  /Type /Page\n")
        writer.writeRaw("  /Parent 2 0 R\n")
        writer.writeRaw("  /MediaBox [0 0 595 842]\n")
        writer.writeRaw("  /Resources 4 0 R\n")
        writer.writeRaw("  /Contents 5 0 R\n")
        writer.writeRaw(">>\n")
        writer.endObject()

        // --- 3. Strip Processing ---
        val totalStrips = (targetHeight + STRIP_HEIGHT_PX - 1) / STRIP_HEIGHT_PX
        val stripBlobs = mutableListOf<ByteArray>()

        val stripBitmap = Bitmap.createBitmap(PRINTER_WIDTH_PX, STRIP_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(stripBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        for (i in 0 until totalStrips) {
            val yOffset = i * STRIP_HEIGHT_PX
            var currentStripH = STRIP_HEIGHT_PX
            if (yOffset + currentStripH > targetHeight) {
                currentStripH = targetHeight - yOffset
            }

            canvas.drawColor(Color.WHITE)
            canvas.save()
            canvas.scale(scale, scale)
            canvas.translate(0f, -(yOffset / scale))
            canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)
            canvas.restore()

            // 提取像素
            val pixels = IntArray(PRINTER_WIDTH_PX * currentStripH)
            stripBitmap.getPixels(pixels, 0, PRINTER_WIDTH_PX, 0, 0, PRINTER_WIDTH_PX, currentStripH)

            // 编码 (二值化 + 标准 RLE)
            stripBlobs.add(encodeBinaryRle(pixels, PRINTER_WIDTH_PX, currentStripH))
        }
        stripBitmap.recycle()

        // --- Obj 4: Resources ---
        writer.startObject()
        writer.writeRaw("<< /XObject <<\n")
        val firstImageId = 6
        for (i in stripBlobs.indices) {
            val id = firstImageId + i
            writer.writeRaw("  /Im$id $id 0 R\n")
        }
        writer.writeRaw(">> >>\n")
        writer.endObject()

        // --- Obj 5: Content ---
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
            sb.append(String.format(Locale.US, "q %.2f 0 0 %.2f 0 %.2f cm /Im%d Do Q\n", pdfPageWidth, drawH, drawY, id))
        }
        sb.append("Q")

        val contentBytes = sb.toString()
        writer.startObject()
        writer.writeRaw("<< /Length ${contentBytes.length} >>\n")
        writer.writeRaw("stream\n")
        writer.writeRaw(contentBytes)
        writer.writeRaw("\nendstream\n") // 确保 endstream 前有换行
        writer.endObject()

        // --- Obj 6+: Images ---
        for (i in stripBlobs.indices) {
            val blob = stripBlobs[i]
            val h = if (i == stripBlobs.size - 1) targetHeight - (i * STRIP_HEIGHT_PX) else STRIP_HEIGHT_PX

            writer.startObject()
            writer.writeRaw("<<\n")
            writer.writeRaw("  /Type /XObject\n")
            writer.writeRaw("  /Subtype /Image\n")
            writer.writeRaw("  /Width $PRINTER_WIDTH_PX\n")
            writer.writeRaw("  /Height $h\n")
            writer.writeRaw("  /ColorSpace /DeviceGray\n")
            writer.writeRaw("  /BitsPerComponent 8\n")
            writer.writeRaw("  /Filter /RunLengthDecode\n")
            writer.writeRaw("  /Length ${blob.size}\n")
            writer.writeRaw(">>\n")
            writer.writeRaw("stream\n")
            writer.flush()
            fos.write(blob)
            writer.writeRaw("\nendstream\n")
            writer.endObject()
        }

        // --- Trailer ---
        val xrefOffset = writer.bytesWritten
        writer.writeRaw("xref\n")
        writer.writeRaw("0 ${writer.objectOffsets.size + 1}\n")
        writer.writeRaw("0000000000 65535 f \n")
        for (offset in writer.objectOffsets) {
            writer.writeRaw(String.format(Locale.US, "%010d 00000 n \n", offset))
        }
        writer.writeRaw("trailer\n")
        writer.writeRaw("<< /Size ${writer.objectOffsets.size + 1} /Root 1 0 R >>\n")
        writer.writeRaw("startxref\n")
        writer.writeRaw("$xrefOffset\n")
        writer.writeRaw("%%EOF\n")

        fos.close()
        return outputFile
    }

    /**
     * 二值化 + PackBits RLE
     * 确保数据极其干净，只包含 00 和 FF
     */
    private fun encodeBinaryRle(pixels: IntArray, w: Int, h: Int): ByteArray {
        val output = ByteArrayOutputStream(w * h / 2)
        val rowGray = ByteArray(w)

        for (row in 0 until h) {
            val startPixel = row * w
            for (col in 0 until w) {
                val c = pixels[startPixel + col]
                // 简单的二值化：亮度 > 128 变白(FF)，否则黑(00)
                // HP 105a PCLm 应该是: 00=Black, FF=White
                val grayVal = (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)).toInt()
                rowGray[col] = if (grayVal > 128) -1 else 0
            }

            // PackBits
            var i = 0
            while (i < w) {
                var runLen = 0
                while (i + runLen + 1 < w && runLen < 127 &&
                    rowGray[i + runLen] == rowGray[i + runLen + 1]) {
                    runLen++
                }

                if (runLen > 0) {
                    // Run
                    val count = runLen + 1
                    output.write(257 - count)
                    output.write(rowGray[i].toInt())
                    i += count
                } else {
                    // Literal
                    var litLen = 0
                    while (i + litLen < w && litLen < 128) {
                        if (i + litLen + 1 < w && rowGray[i + litLen] == rowGray[i + litLen + 1]) break
                        litLen++
                    }
                    output.write(litLen - 1)
                    output.write(rowGray, i, litLen)
                    i += litLen
                }
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
            writeRaw("${objectOffsets.size} 0 obj\n")
        }
        fun endObject() = writeRaw("endobj\n")

        // 直接写入 String 字节，不自动加任何东西，确保控制权在我们手里
        fun writeRaw(s: String) {
            val bytes = s.toByteArray(StandardCharsets.US_ASCII)
            stream.write(bytes)
            bytesWritten += bytes.size
        }
        fun flush() = stream.flush()
    }
}