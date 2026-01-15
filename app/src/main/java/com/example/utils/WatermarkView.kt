package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.withTranslation
import kotlin.math.sqrt

class WatermarkView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var watermarkText: String = ""

    init {
        // 设置水印样式
        paint.color = Color.parseColor("#1A000000") // 黑色，10%透明度，根据背景调整
        paint.textSize = spToPx(14f)
        paint.textAlign = Paint.Align.CENTER
        
        // 关键：必须设置不可点击，否则会拦截底层页面的触摸事件
        isClickable = false
        isFocusable = false
    }

    fun setText(name: String, phoneTail: String) {
        this.watermarkText = "$name  $phoneTail"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (watermarkText.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        
        // 旋转角度
        val degrees = -25f
        
        // 计算对角线长度作为绘制范围，确保旋转后能覆盖全屏
        val diagonal = sqrt((width * width + height * height).toDouble()).toFloat()

        canvas.withTranslation(width / 2, height / 2) {
            // 将画布中心移动到 View 中心
            rotate(degrees)

            // 间距配置
            val gapX = 500f
            val gapY = 500f

            // 计算循环绘制的起始点（覆盖整个旋转后的区域）
            var y = -diagonal / 2
            while (y <= diagonal / 2) {
                var x = -diagonal / 2
                while (x <= diagonal / 2) {
                    drawText(watermarkText, x, y, paint)
                    x += gapX
                }
                y += gapY
            }

        }
    }

    private fun spToPx(sp: Float): Float {
        return sp * context.resources.displayMetrics.scaledDensity
    }
}