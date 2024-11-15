package com.example.demoapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    open val resId = 0
    open fun initView() {}
    open fun initData() {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(resId)
        initView()
        initData()
    }

    fun navigate(intent: Intent) {
        startActivity(intent)
    }
}