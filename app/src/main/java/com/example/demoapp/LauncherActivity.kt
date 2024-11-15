package com.example.demoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class LauncherActivity : BaseActivity() {
    override val resId: Int
        get() = R.layout.activity_launcher

    override fun initView() {
        super.initView()
        findViewById<Button>(R.id.btn_sim).setOnClickListener {
            navigate(Intent(this, SIMActivity::class.java))
        }

        findViewById<Button>(R.id.btn_photo).setOnClickListener {
            navigate(Intent(this, PhotoPickerActivity::class.java))
        }
    }
}