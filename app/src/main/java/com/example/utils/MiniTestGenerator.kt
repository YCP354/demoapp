package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import androidx.core.graphics.createBitmap

object MiniTestGenerator {

    // 宽度 512 (0x200)，这是计算机最喜欢的数字，绝对对齐
    private const val TEST_WIDTH = 512
    private const val TEST_HEIGHT = 128

    fun generateTestPdf(outputFile: File): File {
        // 1. 创建一个小 Bitmap
        val bitmap = createBitmap(TEST_WIDTH, TEST_HEIGHT)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // 白底
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 64f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        // 画一个居中的 "TEST"
        canvas.drawText("TEST", TEST_WIDTH / 2f, TEST_HEIGHT / 2f + 20, paint)
        
        // 2. 编码 RLE (使用 V7 的安全逻辑)
        val rleData = encodeSafeRle(bitmap)
        bitmap.recycle()

        // 3. 写入极简 PDF
        val fos = FileOutputStream(outputFile)
        val writer = PdfWriter(fos)

        writer.writeLine("%PDF-1.7")
        writer.writeLine("%PCLm 1.0")
        
        // Obj 1: Catalog
        writer.startObject()
        writer.writeLine("<< /Type /Catalog /Pages 2 0 R >>")
        writer.endObject()
        
        // Obj 2: Pages
        writer.startObject()
        writer.writeLine("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        writer.endObject()
        
        // Obj 3: Page (MediaBox 依然设为 A4，但内容我们只画在左上角一小块)
        writer.startObject()
        writer.writeLine("<<")
        writer.writeLine("  /Type /Page")
        writer.writeLine("  /Parent 2 0 R")
        writer.writeLine("  /MediaBox [0 0 595 842]") 
        writer.writeLine("  /Resources << /XObject << /ImTest 4 0 R >> >>")
        writer.writeLine("  /Contents 5 0 R")
        writer.writeLine(">>")
        writer.endObject()
        
        // Obj 4: Image XObject (小图)
        writer.startObject()
        writer.writeLine("<<")
        writer.writeLine("  /Type /XObject")
        writer.writeLine("  /Subtype /Image")
        writer.writeLine("  /Width $TEST_WIDTH")  // 512
        writer.writeLine("  /Height $TEST_HEIGHT") // 128
        writer.writeLine("  /ColorSpace /DeviceGray")
        writer.writeLine("  /BitsPerComponent 8")
        writer.writeLine("  /Filter /RunLengthDecode")
        writer.writeLine("  /Length ${rleData.size}")
        writer.writeLine(">>")
        writer.writeLine("stream")
        writer.flush()
        fos.write(rleData)
        writer.writeLine("")
        writer.writeLine("endstream")
        writer.endObject()
        
        // Obj 5: Content
        // 将 512x128 的图片画在 PDF 左上角
        // 512px / 600dpi * 72 = 61 points 宽
        // 128px / 600dpi * 72 = 15 points 高
        // 随便画大一点：宽 200pt，高 50pt
        // 坐标：x=50, y=700 (靠近顶部)
        val content = "q 200 0 0 50 50 700 cm /ImTest Do Q"
        writer.startObject()
        writer.writeLine("<< /Length ${content.length} >>")
        writer.writeLine("stream")
        writer.writeLine(content)
        writer.writeLine("endstream")
        writer.endObject()
        
        // Trailer
        val xrefOffset = writer.bytesWritten
        writer.writeLine("xref")
        writer.writeLine("0 6")
        writer.writeLine("0000000000 65535 f ")
        for (offset in writer.objectOffsets) {
            writer.writeLine(String.format(Locale.US, "%010d 00000 n ", offset))
        }
        writer.writeLine("trailer")
        writer.writeLine("<< /Size 6 /Root 1 0 R >>")
        writer.writeLine("startxref")
        writer.writeLine("$xrefOffset")
        writer.writeLine("%%EOF")

        fos.close()
        return outputFile
    }
    
    // 简单的 RLE 编码 (逐行 + Literal 模式)
    private fun encodeSafeRle(bitmap: Bitmap): ByteArray {
        val w = bitmap.width
        val h = bitmap.height
        val output = ByteArrayOutputStream()
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        
        val rowGray = ByteArray(w)
        
        for (row in 0 until h) {
            val start = row * w
            for (col in 0 until w) {
                val c = pixels[start + col]
                val gray = (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)).toInt()
                rowGray[col] = gray.toByte()
            }
            
            // 写入 Literal (每128字节)
            var i = 0
            while (i < w) {
                var len = w - i
                if (len > 128) len = 128
                output.write(len - 1)
                output.write(rowGray, i, len)
                i += len
            }
        }
        // 不加 EOD，测试一下
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
            val bytes = (s + "\r\n").toByteArray(StandardCharsets.US_ASCII)
            stream.write(bytes)
            bytesWritten += bytes.size
        }
        fun flush() = stream.flush()
    }
}