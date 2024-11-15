package com.example.demoapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.widget.TextView
import androidx.core.app.ActivityCompat
import java.lang.StringBuilder


class SIMActivity : BaseActivity() {
    override val resId: Int
        get() = R.layout.activity_sim

    override fun initView() {
        printSIM()
    }

    @SuppressLint("HardwareIds")
    private fun printSIM() {
        val msg = StringBuilder().append("SIM信息：\n")
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        // 获取设备的SIM卡序列号
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            msg.append("无权限\n")
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 0)
        } else {
            msg.append(
                "printSIM: simSerialNumber:${
                    telephonyManager.simSerialNumber
                }\n"
            )
            // 获取SIM卡运营商的国家代码

            // 获取SIM卡运营商的国家代码
            val simCountryIso = telephonyManager.simCountryIso
            msg.append(
                "printSIM: simCountryIso:${
                    simCountryIso
                }\n"
            )
            // 获取SIM卡运营商的名字

            // 获取SIM卡运营商的名字
            val simOperatorName = telephonyManager.simOperatorName

            msg.append(
                "printSIM: simOperatorName:${
                    simOperatorName
                }\n"
            )
            // 获取SIM卡的状态
            // 获取SIM卡的状态
            val simState = telephonyManager.simState
            msg.append(
                "printSIM: simState:${
                    simState
                }\n"
            )
            msg.append("4g标识：${TelephonyManager.NETWORK_TYPE_LTE}\n")
            msg.append("当前网络状态标识:${telephonyManager.networkType}\n")
            msg.append("当前设备卡槽数量:${telephonyManager.phoneCount}\n")
            msg.append("当前插入sim卡数量:${SubscriptionManager.from(this).activeSubscriptionInfoCount}\n")
//            msg.append("可用feature:${packageManager.}\n")
        }
        findViewById<TextView>(R.id.tv_content).text = msg.toString()
    }

}