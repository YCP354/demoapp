package com.example.demoapp.adapter

import android.app.Application
import com.example.utils.WatermarkManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 假设从 UserInfo 获取到了信息
        val userName = "版权所有"
        val userPhone = "泄密必究"
        
        WatermarkManager.init(this, userName, userPhone)
    }
}