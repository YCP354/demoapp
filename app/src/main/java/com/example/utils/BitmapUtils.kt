package com.example.utils

import android.graphics.Bitmap

object BitmapUtils {

    // 移植自 N0.java / RasterFile 的 600DPI 4x4 簇点抖动矩阵 (Cluster Template)
    // 商业驱动用这个来模拟灰度
    private val CLUSTER_TEMPLATE_600DPI = arrayOf(
        intArrayOf(180, 52, 20, 140),
        intArrayOf(108, 4, 100, 228),
        intArrayOf(236, 164, 84, 196),
        intArrayOf(148, 68, 36, 172)
    )

    // 矩阵大小
    private const val MATRIX_SIZE = 4

    /**
     * 将 Bitmap 转换为 1-bit 单色数据 (应用抖动算法)
     * @param bitmap 原始图片
     * @param width 目标宽度 (必须是 4958)
     * @param height 目标高度
     * @return 转换后的 byte 数组 (1 bit per pixel, packed)
     */
    fun ditherBitmapTo1Bit(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val pixels = IntArray(width * height)
        // 获取像素，注意：如果 bitmap 尺寸不对，这里可能会抛错，确保传入前已缩放
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 计算输出大小：每个像素 1 bit -> width * height / 8
        // 加上一些 padding 防止溢出
        val outputSize = (width * height + 7) / 8
        val output = ByteArray(outputSize)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = pixels[y * width + x]
                
                // 1. 提取亮度 (0-255)
                // Y = 0.299R + 0.587G + 0.114B
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val gray = (r * 30 + g * 59 + b * 11) / 100

                // 2. 获取抖动阈值
                // 移植逻辑：根据 x, y 坐标从矩阵中取值
                // 源码逻辑：255 - ((val * 255) / 255) ... 简化后直接用矩阵值对比
                val threshold = CLUSTER_TEMPLATE_600DPI[y % MATRIX_SIZE][x % MATRIX_SIZE]

                // 3. 比较并决定黑白
                // 如果当前像素比阈值暗，则是黑色(打点)；否则白色。
                // 注意：在 PDF DeviceGray 中，0=黑，1=白。
                // 但在 1-bit 存储中，通常 1=White, 0=Black。
                // 让我们参考 N0.java: i19 = (i19 == true ? 1 : 0) | i21; 看起来是在拼位
                
                val isBlack = gray < threshold

                if (isBlack) {
                    // 计算 byte 位置
                    val bitIndex = y * width + x
                    val byteIdx = bitIndex / 8
                    val bitPos = 7 - (bitIndex % 8)
                    
                    // 将对应位置 0 (黑色) ? 
                    // 不，通常初始化是 0，我们要设 1？
                    // 假设 output 初始全 0。
                    // 如果 1 代表黑 (Ink On)，则：
                    // output[byteIdx] = (output[byteIdx].toInt() or (1 shl bitPos)).toByte()
                    
                    // 如果 PCLm 遵循 PDF 标准：1=White, 0=Black。
                    // 那么全黑应该是 0x00，全白 0xFF。
                    // 抖动产生的 "点" 是墨水(黑)。
                } else {
                    // 是白色
                    val bitIndex = y * width + x
                    val byteIdx = bitIndex / 8
                    val bitPos = 7 - (bitIndex % 8)
                    
                    // 置 1 (White)
                    output[byteIdx] = (output[byteIdx].toInt() or (1 shl bitPos)).toByte()
                }
            }
        }
        return output
    }
}