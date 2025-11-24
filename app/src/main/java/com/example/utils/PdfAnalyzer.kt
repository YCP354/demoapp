import android.content.Context
import java.io.File
import java.nio.charset.Charset

object DeepImageAnalyzer {

    fun analyzeImageStream(file: File) {
        if (!file.exists()) {
            println("❌ [DEEP_ANALYZER] Ref 文件不存在")
            return
        }

        try {
            val bytes = file.readBytes()
            val contentStr = String(bytes, Charset.forName("ISO-8859-1")) // 单字节映射

            println("✅ [DEEP_ANALYZER] 文件大小: ${bytes.size}")

            // 1. 获取完整的文件头 (前 500 字节)，我们需要看看 Job Ticket 到底写了啥
            println("📋 [DEEP_ANALYZER] 完整文件头 (Head 500 bytes):")
            println(String(bytes.copyOfRange(0, 500.coerceAtMost(bytes.size)), Charset.forName("ASCII")))
            println("------------------------------------------------")

            // 2. 定位图片对象
            // 寻找 "/Subtype /Image" 或 "/Subtype/Image"
            var imageIdx = contentStr.indexOf("/Subtype /Image")
            if (imageIdx == -1) imageIdx = contentStr.indexOf("/Subtype/Image")

            if (imageIdx == -1) {
                println("❌ [DEEP_ANALYZER] 未找到 Image 对象")
                return
            }

            println("📍 [DEEP_ANALYZER] 发现 Image 对象定义于索引: $imageIdx")

            // 3. 从 Image 定义处开始，往后找 "stream"
            val streamKeyword = "stream"
            val streamIdx = contentStr.indexOf(streamKeyword, startIndex = imageIdx)

            if (streamIdx == -1) {
                println("❌ [DEEP_ANALYZER] Image 对象后未找到 stream")
                return
            }

            // 4. 跳过 "stream" 关键字和随后的换行符 (CR, LF)
            var dataStart = streamIdx + streamKeyword.length
            while (dataStart < bytes.size && (bytes[dataStart] == 0x0D.toByte() || bytes[dataStart] == 0x0A.toByte())) {
                dataStart++
            }

            println("🔍 [DEEP_ANALYZER] 图片二进制数据起始于: $dataStart")
            println("🔍 [DEEP_ANALYZER] 下面是图片数据的前 128 个字节 (Hex):")

            val sb = StringBuilder()
            for (i in 0 until 128) {
                if (dataStart + i >= bytes.size) break
                val b = bytes[dataStart + i]
                val hex = String.format("%02X", b)
                sb.append(hex).append(" ")
                if ((i + 1) % 16 == 0) sb.append("\n")
            }
            println(sb.toString())
            println("------------------------------------------------")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}