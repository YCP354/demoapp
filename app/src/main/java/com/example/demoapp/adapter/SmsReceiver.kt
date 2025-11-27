package com.example.demoapp.adapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_DELIVER_ACTION == intent.action ||
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {

            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val sb = StringBuilder()

                for (msg in messages) {
                    sb.append("来自：${msg.originatingAddress}, 内容：${msg.messageBody}\n")
                }

                Log.e(TAG, "收到短信：\n$sb")
            } catch (e: Exception) {
                Log.e(TAG, "解析短信失败: ${e.message}")
            }
        }
    }
}
