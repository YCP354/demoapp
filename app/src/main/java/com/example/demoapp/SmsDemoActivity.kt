package com.example.demoapp

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class SmsDemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_demo)

        val btnSetDefault = findViewById<Button>(R.id.btnSetDefault)
        val btnInsertSms = findViewById<Button>(R.id.btnInsertSms)
        val btnResetDefault = findViewById<Button>(R.id.btnResetDefault)

        // 1. 设置为默认短信 App
        btnSetDefault.setOnClickListener {
            if (isDefaultSmsApp()) {
                Toast.makeText(this, "当前已是默认短信应用", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    startActivity(intent)
                    Toast.makeText(this, "正在申请设为默认短信应用…", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // 国产手机兜底处理
                    openAppSettingForSms()
                }
            }
        }


        // 2. 写入短信
        btnInsertSms.setOnClickListener {
            if (!isDefaultSmsApp()) {
                Toast.makeText(this, "请先将本应用设置为默认短信应用", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            insertMockSms()
        }

        // 3. 恢复默认短信应用
        btnResetDefault.setOnClickListener {
            resetDefaultSmsApp()
        }
    }

    // --------------------------------------
    // 功能方法
    // --------------------------------------

    /** 是否为默认短信应用 */
    private fun isDefaultSmsApp(): Boolean {
        val pkg = Telephony.Sms.getDefaultSmsPackage(this)
        return packageName == pkg
    }

    /** 申请成为默认短信应用 */
    private fun requestSetDefaultSmsApp() {
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        Toast.makeText(this, "正在申请设为默认短信应用…", Toast.LENGTH_SHORT).show()
    }

    private fun openAppSettingForSms() {
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        startActivity(intent)
    }



    /** 写入短信内容 */
    private fun insertMockSms() {

        // 目标短信内容（优化后的）
        val body = "【抖音通知】你有一条新的消息，请打开抖音APP查看详情。"

        // 指定时间：2025-11-27 10:35 AM
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val time = sdf.parse("2025-11-27 10:35")!!.time

        val values = ContentValues().apply {
            put("address", "1068294290007771")  // 发件人
            put("body", body)
            put("date", time)
            put("read", 0)
            put("type", 1)                      // 1 = 收件箱
        }

        val uri = Uri.parse("content://sms/inbox")
        contentResolver.insert(uri, values)

        Toast.makeText(this, "短信写入成功", Toast.LENGTH_LONG).show()
    }

    /** 恢复回系统默认短信应用 */
    private fun resetDefaultSmsApp() {
        val systemSmsApp = Telephony.Sms.getDefaultSmsPackage(this) ?: return

        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, systemSmsApp)
        startActivity(intent)
        Toast.makeText(this, "恢复默认短信应用", Toast.LENGTH_SHORT).show()
    }
}
