package com.example.demoapp

import android.content.Intent
import android.os.Build.VERSION_CODES.P
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.example.camera.CameraActivity

class LauncherActivity : BaseActivity() {
    var flag = false
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
        findViewById<Button>(R.id.btn_camera).setOnClickListener {
            navigate(Intent(this, CameraActivity::class.java))
            flag = false
        }

        findViewById<Button>(R.id.btn_sim).setOnLongClickListener {
            flag = true
            true
        }

        findViewById<Button>(R.id.btn_photo).setOnLongClickListener {
            if(flag){
                findViewById<Button>(R.id.btn_camera).visibility = View.VISIBLE
            }
            false
        }
    }
}