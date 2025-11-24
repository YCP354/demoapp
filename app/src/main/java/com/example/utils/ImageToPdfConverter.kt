package com.example.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageToPdfConverter {

    // A4 标准尺寸 (以 72 DPI 为基准的 PostScript Point 单位)
    // 宽 595 pixel, 高 842 pixel
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    /**
     * 将图片 URI 转换为适合 HP Laser 105a 的 A4 PDF
     */
    fun convertImageToPdf(context: Context, imageUri: Uri, outputDir: File): File? {
        return try {
            // 1. 获取输入流读取图片
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // 2. 创建 PDF 文档
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // 3. 计算缩放比例 (Fit Center)
            // 保持图片比例，使其能完整塞入 A4 页面
            val scale = Math.min(
                A4_WIDTH.toFloat() / originalBitmap.width,
                A4_HEIGHT.toFloat() / originalBitmap.height
            )
            
            val destWidth = originalBitmap.width * scale
            val destHeight = originalBitmap.height * scale
            
            // 居中显示
            val left = (A4_WIDTH - destWidth) / 2
            val top = (A4_HEIGHT - destHeight) / 2

            // 4. 配置画笔 (黑白优化)
            val paint = Paint().apply {
                isFilterBitmap = true // 开启抗锯齿
                
                // 可选：添加黑白滤镜 (ColorMatrix)，增强激光打印机的对比度
                val colorMatrix = ColorMatrix()
                colorMatrix.setSaturation(0f) // 饱和度设为0，变灰度
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }

            // 5. 绘制图片到 PDF Canvas
            // 注意：这里我们定义了一个 RectF 来指定图片在 PDF 页面的位置和大小
            val destRect = RectF(left, top, left + destWidth, top + destHeight)
            canvas.drawBitmap(originalBitmap, null, destRect, paint)

            document.finishPage(page)

            // 6. 保存文件
            val outputFile = File(outputDir, "print_job_${System.currentTimeMillis()}.pdf")
            val fos = FileOutputStream(outputFile)
            document.writeTo(fos)
            
            // 7. 资源清理
            document.close()
            fos.close()
            originalBitmap.recycle() // 释放大图内存

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}