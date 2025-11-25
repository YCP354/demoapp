package com.example.utils

import android.content.Context
import java.io.File
import java.nio.charset.Charset

object StreamHunter {

    fun hunt(context: Context,file: File) {
        if (!file.exists()) {
            println("❌ 文件未找到")
            return
        }
        
        val bytes = file.readBytes()
        val contentStr = String(bytes, Charset.forName("ISO-8859-1"))
        
        println("=== 开启 Stream 狩猎模式 ===")
        
        // 查找所有 "stream" 的位置
        var index = contentStr.indexOf("stream")
        var count = 0
        
        while (index != -1) {
            // 跳过 "stream" 本身 (6 chars)
            var dataStart = index + 6
            // 跳过换行 (CR LF)
            while (dataStart < bytes.size && (bytes[dataStart] == 0x0D.toByte() || bytes[dataStart] == 0x0A.toByte())) {
                dataStart++
            }
            
            if (dataStart + 10 > bytes.size) break

            // 检查前 10 个字节是否包含“非文本”字符
            // 文本指令通常由字母数字空格组成 (ASCII 32-126)
            // RLE 数据通常包含大量 0x80 以上的控制符
            var isBinary = false
            for (i in 0 until 10) {
                val b = bytes[dataStart + i].toInt() and 0xFF
                if (b > 128 || b == 0) { // 0x80以上或0x00通常是二进制特征
                    isBinary = true
                    break
                }
            }

            if (isBinary) {
                count++
                println("\n🎯 [猎物 #$count] 发现二进制流 (Offset: $dataStart)")
                
                // 打印前 64 字节 Hex
                val sb = StringBuilder()
                for (i in 0 until 64) {
                    if (dataStart + i >= bytes.size) break
                    val b = bytes[dataStart + i]
                    val hex = String.format("%02X", b)
                    sb.append(hex).append(" ")
                }
                println(sb.toString())
                println("------------------------------------------------")
                
                // 如果找到了，基本就是它了，我们可以停止
                // 但为了保险，我们打印前 3 个发现的二进制流
                if (count >= 3) break
            }
            
            // 继续找下一个
            index = contentStr.indexOf("stream", index + 6)
        }
        
        if (count == 0) {
            println("❌ 未找到任何二进制流，这很奇怪！")
        }
    }
}