package com.example.utils

import android.util.Log
import java.io.File
import java.nio.charset.Charset

object PdfDiagnostics {

    private const val TAG = "[PDF_DIFF]"

    fun compareFiles(generatedFile: File, referenceFile: File) {
       Log.d("ycp"," ================= 开始对比诊断 =================")
       Log.d("ycp"," 生成文件: ${generatedFile.absolutePath} (Size: ${generatedFile.length()})")
       Log.d("ycp"," 参考文件: ${referenceFile.absolutePath} (Size: ${referenceFile.length()})")

        if (!referenceFile.exists()) {
           Log.d("ycp"," ❌ 参考文件不存在！请确保 usb_dump.pdf 位于指定路径。")
            return
        }

        // 读取文件内容 (为了防止文件过大 OOM，我们只读取前 20KB，通常 header 和 image object 都在前面)
        val genContent = readFileHead(generatedFile)
        val refContent = readFileHead(referenceFile)

        // 1. 对比 Header
        compareValue("Header", extractHeader(genContent), extractHeader(refContent))

        // 2. 寻找 Image XObject 的关键属性
        // PDF 中图片通常定义为: << /Type /XObject /Subtype /Image ... >>
        
        // 提取 Filter (编码/压缩格式)
        val genFilter = extractAttribute(genContent, "/Filter")
        val refFilter = extractAttribute(refContent, "/Filter")
        compareValue("Filter (压缩格式)", genFilter, refFilter)

        // 提取 ColorSpace (色彩空间)
        val genColor = extractAttribute(genContent, "/ColorSpace")
        val refColor = extractAttribute(refContent, "/ColorSpace")
        compareValue("ColorSpace", genColor, refColor)

        // 提取 BitsPerComponent (位深)
        val genBits = extractAttribute(genContent, "/BitsPerComponent")
        val refBits = extractAttribute(refContent, "/BitsPerComponent")
        compareValue("BitsPerComponent", genBits, refBits)

        // 提取 Width
        val genWidth = extractAttribute(genContent, "/Width")
        val refWidth = extractAttribute(refContent, "/Width")
        compareValue("Width", genWidth, refWidth)

        // 提取 Height
        val genHeight = extractAttribute(genContent, "/Height")
        val refHeight = extractAttribute(refContent, "/Height")
        compareValue("Height", genHeight, refHeight)

        // 3. 检查是否包含 PCLm 签名
        val genHasPclm = genContent.contains("PCLm")
        val refHasPclm = refContent.contains("PCLm")
        compareValue("Contains 'PCLm'", genHasPclm.toString(), refHasPclm.toString())

       Log.d("ycp"," ================= 诊断结束 =================")
    }

    private fun readFileHead(file: File): String {
        return try {
            // ISO-8859-1 保证单字节读取，不会因为 UTF-8 编码破坏二进制结构
            val buffer = ByteArray(20480.coerceAtMost(file.length().toInt()))
            file.inputStream().use { it.read(buffer) }
            String(buffer, Charset.forName("ISO-8859-1"))
        } catch (e: Exception) {
            "Read Error: ${e.message}"
        }
    }

    private fun extractHeader(content: String): String {
        // 获取前 50 个字符，去掉换行
        return content.take(50).replace("\n", " ").replace("\r", " ").trim()
    }

    private fun extractAttribute(content: String, key: String): String {
        // 简单的正则匹配 PDF 键值对，例如 /Width 1200 或 /Filter /RunLengthDecode
        // 匹配 key 后面的空格，然后捕获非空白、非 >、非 / 的内容
        // 注意：PDF 结构复杂，这里做简化假设，只找第一个匹配项
        val regex = Regex("$key\\s*[\\[\\/]?([ProcessSession-zA-Z0-9]+)")
        val match = regex.find(content)
        return if (match != null) {
            // 如果捕获到的是 Name 对象 (例如 DCTDecode)，补回前面的 / 以便阅读
            val raw = match.groupValues[1]
            // 判断原始文本里有没有 /
            val fullMatch = match.value
            if (fullMatch.contains("/")) "/$raw" else raw
        } else {
            "Not Found"
        }
    }

    private fun compareValue(label: String, genVal: String, refVal: String) {
        val match = if (genVal == refVal) "✅ MATCH" else "❌ DIFF"
       Log.d("ycp"," [$label]")
       Log.d("ycp","    Gen: $genVal")
       Log.d("ycp","    Ref: $refVal  --> $match")
    }
}