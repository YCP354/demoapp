package com.example.demoapp

import android.content.Intent
import android.widget.Button
import com.example.demoapp.adapter.BleTestActivity
import kotlin.jvm.java

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
        findViewById<Button>(R.id.btn_printer).setOnClickListener {
            navigate(Intent(this, PrinterActivity::class.java))
        }
        findViewById<Button>(R.id.btn_ble).setOnClickListener {
            navigate(Intent(this, BleTestActivity::class.java))
        }
        findViewById<Button>(R.id.btn_sms).setOnClickListener {
            navigate(Intent(this, SmsDemoActivity::class.java))
        }
    }
}