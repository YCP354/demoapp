package com.example.utils

import android.content.Context
import android.net.Uri
import com.dynamixsoftware.drv.DrvRuntime
import com.dynamixsoftware.drv.ProcessSessionKt
import java.io.File
import java.io.FileOutputStream

object PrinterDataGenerator {

    /**
     * 将输入 Uri 的图片生成打印机可识别的文件
     *
     * @param context 上下文
     * @param imageUri 图片 Uri（content:// 或 file://）
     * @param soPath 驱动 so 文件路径，例如 "/data/data/yourapp/lib/libescpr.so"
     * @return 返回生成的 File
     */
    fun generateFromUri(context: Context, imageUri: Uri, soPath: String): File {
        // 1. 将 Uri 转成临时文件
        val inputFile = uriToFile(context, imageUri)

        // 2. 创建临时输出文件
        val outFile = File.createTempFile("printer_data_", ".prn", context.cacheDir)
        outFile.deleteOnExit() // 可选

        // 3. 构建命令参数，so 驱动会处理图片生成打印机数据
        val cmd = arrayOf(soPath, "-i", inputFile.absolutePath, "-o", "-")

        // 4. 调用 drvRuntime 执行
        val processHandler: ProcessSessionKt = DrvRuntime.a(cmd, null)

        // 5. 输出到文件
        FileOutputStream(outFile).use { fos ->
            processHandler.startStreams(fos, false, false)
            processHandler.waitForProcess() // 等待线程完成
        }

        // 6. 检查结果
        if (!outFile.exists() || outFile.length() == 0L) {
            throw RuntimeException("打印机数据生成失败")
        }

        return outFile
    }

    /**
     * 将 Uri 转成 File（复制到 cacheDir）
     */
    private fun uriToFile(context: Context, uri: Uri): File {
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "temp_image"
        val tempFile = File(context.cacheDir, fileName)
        tempFile.outputStream().use { fos ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.copyTo(fos)
            } ?: throw IllegalArgumentException("无法打开 Uri: $uri")
        }
        return tempFile
    }
}
