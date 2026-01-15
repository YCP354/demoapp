package com.example.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

object WatermarkManager {

    private var watermarkText: String = ""

    fun init(application: Application, name: String, phoneTail: String) {
        this.watermarkText = "$name $phoneTail"
        
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                addWatermark(activity)
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                addWatermark(activity)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun addWatermark(activity: Activity) {
        val root = activity.window.decorView as? ViewGroup ?: return
        
        // 创建水印 View
        val watermarkView = WatermarkView(activity).apply {
            setText(watermarkText.split(" ")[0], watermarkText.split(" ")[1])
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 添加到 DecorView，这会覆盖在布局的最上层
        root.addView(watermarkView)
    }
}