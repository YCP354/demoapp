package com.example.socket

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.demoapp.R
import java.net.Inet4Address
import java.net.NetworkInterface


class SocketConnectActivity : AppCompatActivity() {

    private  val TAG = "SocketConnectActivity"
    private lateinit var server: WebSocketServerImpl
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actvity_socket_connect)
        initData()
    }

    override fun onBackPressed() {

    }

    @SuppressLint("SetTextI18n")
    private fun initData() {

        findViewById<TextView>(R.id.text).text = "当前IP地址：${getLocalIpAddress()}"
        findViewById<EditText>(R.id.edit_tv).apply {
            hint = "输入文字并回车发送"
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine()
            setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    event?.keyCode == KeyEvent.KEYCODE_ENTER
                ) {
                    val text = text.toString().trim()
                    if (text.isNotEmpty()) {
                        setText("")
                        server.broadcastMessage(text)
                    }
                    true
                } else false
            }
        }

        server = WebSocketServerImpl(8887, object : WebSocketServerImpl.ServerListener {
            override fun onClientConnected(conn: org.java_websocket.WebSocket?) {
                Log.d(TAG, "onClientConnected() called with: conn = $conn")
            }

            override fun onReceiverClientMessage(
                conn: org.java_websocket.WebSocket?,
                message: String?
            ) {
                Log.d(
                    TAG,
                    "onReceiverClientMessage() called with: conn = $conn, message = $message"
                )
            }

            override fun onClientDisconnected(conn: org.java_websocket.WebSocket?) {
                Log.d(TAG, "onClientDisconnected() called with: conn = $conn")            }
        })
        server.start()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 及以下
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}