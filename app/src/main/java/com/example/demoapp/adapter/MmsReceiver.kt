package com.example.demoapp.adapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {

        // WAP PUSH 即彩信的推送消息（如运营商推送的配置信息、MMS）
        if (intent.action == "android.provider.Telephony.WAP_PUSH_DELIVER" ||
            intent.action == "android.provider.Telephony.WAP_PUSH_RECEIVED") {

            Log.e(TAG, "收到彩信（WAP_PUSH），但未做处理")
        }
    }
}
