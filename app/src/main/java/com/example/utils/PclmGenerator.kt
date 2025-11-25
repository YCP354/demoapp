import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PclmGenerator {

    // ✅ 实锤参数：来自 qr_usb_dump.pdf
    private const val PRINTER_WIDTH_PX = 4958
    private const val STRIP_HEIGHT_PX = 128 // 🔥 修正：必须是 128！

    // 4x4 簇点抖动矩阵 (移植自商业驱动)
    private val CLUSTER_TEMPLATE = arrayOf(
        intArrayOf(180, 52, 20, 140),
        intArrayOf(108, 4, 100, 228),
        intArrayOf(236, 164, 84, 196),
        intArrayOf(148, 68, 36, 172)
    )

    fun generatePclmPdf(sourceBitmap: Bitmap, outputFile: File): File {
        val fos = FileOutputStream(outputFile)
        val writer = PdfWriter(fos)

        // 1. 预计算尺寸和分条数
        val scale = PRINTER_WIDTH_PX.toFloat() / sourceBitmap.width
        val targetHeight = (sourceBitmap.height * scale).toInt()
        val totalStrips = (targetHeight + STRIP_HEIGHT_PX - 1) / STRIP_HEIGHT_PX

        // 2. 缩放图片 (准备数据源)
        // 注意：这里为了代码简洁直接缩放，生产环境建议分块处理防止OOM
        val scaledBitmap = Bitmap.createScaledBitmap(sourceBitmap, PRINTER_WIDTH_PX, targetHeight, true)

        // --- Header (完全照抄 xxd 输出) ---
        writer.writeRaw("%PDF-1.7\n")
        writer.writeRaw("%PCLm 1.0\n")
        writer.writeRaw("%  genPCLm (Ver: 0.930000)\n")
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
        writer.writeRaw("%      strip-height: $STRIP_HEIGHT_PX\n") // 128
        // 补全 xxd 中发现的所有缺失字段
        writer.writeRaw("%      print-color-mode: monochrome\n")
        writer.writeRaw("%      print-rendering-intent: none\n")
        writer.writeRaw("%      print-quality: normal\n")
        writer.writeRaw("%      printer-resolution: 600\n")
        writer.writeRaw("%      orientation-requested: 0\n")
        writer.writeRaw("%      copies: 1\n")
        writer.writeRaw("%      pclm-raster-back-side: xxx\n")
        writer.writeRaw("%      margins-pre-applied: TRUE\n")
        writer.writeRaw("% PCLmS-Job-Ticket-End\n")

        // --- Body Objects ---

        // Obj 1: Catalog
        writer.startObject()
        writer.writeRaw("<< /Type /Catalog /Pages 2 0 R >>\n")
        writer.endObject()

        // Obj 2: Pages
        writer.startObject()
        writer.writeRaw("<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n")
        writer.endObject()

        // Obj 3: Page (关键：在此处列出所有 Image 引用，模仿金样)
        // 假设 Content 是 Obj 4，Images 从 Obj 5 开始
        writer.startObject()
        writer.writeRaw("<<\n")
        writer.writeRaw("  /Type /Page\n")
        writer.writeRaw("  /Parent 2 0 R\n")
        writer.writeRaw("  /MediaBox [0 0 595 842]\n")

        // 构建 Resources 字典
        writer.writeRaw("  /Resources <<\n")
        writer.writeRaw("    /XObject <<\n")
        val firstImageId = 5
        for (i in 0 until totalStrips) {
            // /Image0 5 0 R, /Image1 6 0 R ...
            writer.writeRaw("      /Image$i ${firstImageId + i} 0 R\n")
        }
        writer.writeRaw("    >>\n")
        writer.writeRaw("  >>\n")

        writer.writeRaw("  /Contents 4 0 R\n")
        writer.writeRaw(">>\n")
        writer.endObject()

        // --- Obj 4: Content Stream (绘制指令) ---
        val sb = StringBuilder()
        sb.append("q ") // Save state
        val pdfPageHeight = 842.0
        val pdfPageWidth = 595.0

        for (i in 0 until totalStrips) {
            // 计算当前 strip 的实际高度
            val yOffset = i * STRIP_HEIGHT_PX
            var h = STRIP_HEIGHT_PX
            if (yOffset + h > targetHeight) h = targetHeight - yOffset

            // 坐标计算 (PDF 坐标原点在左下角)
            val drawH = (h.toDouble() / targetHeight) * pdfPageHeight
            val drawY = pdfPageHeight - ((yOffset.toDouble() / targetHeight) * pdfPageHeight) - drawH

            // 绘制指令: /Image0 Do, /Image1 Do ...
            // 参数: q width 0 0 drawH 0 drawY cm /ImageX Do Q
            sb.append(String.format(Locale.US, "q %.2f 0 0 %.2f 0 %.2f cm /Image%d Do Q\n", pdfPageWidth, drawH, drawY, i))
        }
        sb.append("Q") // Restore state

        val contentBytes = sb.toString()
        writer.startObject() // Obj 4
        writer.writeRaw("<< /Length ${contentBytes.length} >>\n")
        writer.writeRaw("stream\r\n") // 严格 CRLF
        writer.writeRaw(contentBytes)
        writer.writeRaw("\r\nendstream\n")
        writer.endObject()

        // --- Obj 5+: Images (Actual Data) ---
        for (i in 0 until totalStrips) {
            val yOffset = i * STRIP_HEIGHT_PX
            var h = STRIP_HEIGHT_PX
            if (yOffset + h > targetHeight) h = targetHeight - yOffset

            // 1. 提取像素
            val pixels = IntArray(PRINTER_WIDTH_PX * h)
            scaledBitmap.getPixels(pixels, 0, PRINTER_WIDTH_PX, 0, yOffset, PRINTER_WIDTH_PX, h)

            // 2. 编码 (抖动 + 8位映射 + RLE)
            // 既然 xxd 里有 RLE，且我们之前验证过 81 FF，我们继续使用这个逻辑
            val rleData = encodeDitheredRle8Bit(pixels, PRINTER_WIDTH_PX, h, yOffset)

            writer.startObject() // Obj 5, 6, 7...
            writer.writeRaw("<<\n")
            writer.writeRaw("  /Type /XObject\n")
            writer.writeRaw("  /Subtype /Image\n")
            writer.writeRaw("  /Width $PRINTER_WIDTH_PX\n")
            writer.writeRaw("  /Height $h\n")
            writer.writeRaw("  /ColorSpace /DeviceGray\n")
            writer.writeRaw("  /BitsPerComponent 8\n")
            writer.writeRaw("  /Filter /RunLengthDecode\n")
            writer.writeRaw("  /Length ${rleData.size}\n")
            writer.writeRaw(">>\n")
            writer.writeRaw("stream\r\n")
            writer.flush()
            fos.write(rleData)
            writer.writeRaw("\r\nendstream\n")
            writer.endObject()
        }

        if (scaledBitmap != sourceBitmap) scaledBitmap.recycle()

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
     * 编码：4x4 抖动 -> 8位映射 (00/FF) -> RLE 压缩
     */
    private fun encodeDitheredRle8Bit(pixels: IntArray, w: Int, h: Int, globalYOffset: Int): ByteArray {
        val output = ByteArrayOutputStream(w * h / 2)
        val rowGray = ByteArray(w)

        for (row in 0 until h) {
            val startPixel = row * w
            val matrixY = (globalYOffset + row) % 4

            for (col in 0 until w) {
                val c = pixels[startPixel + col]
                val grayVal = (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)).toInt()
                val threshold = CLUSTER_TEMPLATE[matrixY][col % 4]
                // 0=Black, FF=White
                rowGray[col] = if (grayVal < threshold) 0x00.toByte() else (-1).toByte()
            }

            // PackBits RLE
            var i = 0
            while (i < w) {
                var runLen = 0
                while (i + runLen + 1 < w && runLen < 127 &&
                    rowGray[i + runLen] == rowGray[i + runLen + 1]) {
                    runLen++
                }

                if (runLen > 0) {
                    val count = runLen + 1
                    output.write(257 - count)
                    output.write(rowGray[i].toInt())
                    i += count
                } else {
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
        fun writeRaw(s: String) {
            val bytes = s.toByteArray(StandardCharsets.US_ASCII)
            stream.write(bytes)
            bytesWritten += bytes.size
        }
        fun flush() = stream.flush()
    }
}