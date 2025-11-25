package com.example.utils

import android.content.Context
import java.io.File
import java.nio.charset.Charset

object HexDumpAnalyzer {

    fun analyze(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            println("❌ 文件未找到: $filePath")
            return
        }
        
        val bytes = file.readBytes()
        val content = String(bytes, Charset.forName("ISO-8859-1"))
        
        println("=== 开始分析 PrintHand 样本 ===")
        println("文件大小: ${bytes.size} bytes")

        // 1. 提取 Image 对象参数
        val imgStart = content.indexOf("/Subtype /Image")
        if (imgStart != -1) {
            val dictEnd = content.indexOf("stream", imgStart)
            println("\n[Image 参数字典]:")
            println(content.substring(imgStart, dictEnd).replace("\r", ""))
        }

        // 2. 定位 Stream 数据
        val streamKey = "stream"
        var dataStart = content.indexOf(streamKey)
        if (dataStart == -1) return
        
        // 跳过 stream 关键字和换行
        dataStart += streamKey.length
        while (bytes[dataStart] == 0x0D.toByte() || bytes[dataStart] == 0x0A.toByte()) {
            dataStart++
        }

        println("\n[Stream Hex 前 256 字节] (分析 RLE 规律):")
        val sb = StringBuilder()
        val ascii = StringBuilder()
        
        for (i in 0 until 256) {
            if (dataStart + i >= bytes.size) break
            val b = bytes[dataStart + i].toInt() and 0xFF
            
            val hex = String.format("%02X", b)
            sb.append(hex).append(" ")
            
            // 尝试翻译 RLE 指令含义
            // PackBits: 0~127 = Literal (n+1 bytes), 129~255 = Run (257-n times)
            // 128 (0x80) = EOD
            
            // 简单的 ASCII 预览
            if (b in 32..126) ascii.append(b.toChar()) else ascii.append(".")
            
            if ((i + 1) % 16 == 0) {
                sb.append("  |  ").append(ascii)
                println(sb.toString())
                sb.clear()
                ascii.clear()
            }
        }
        println(sb.toString())
        println("=====================================")
    }
}