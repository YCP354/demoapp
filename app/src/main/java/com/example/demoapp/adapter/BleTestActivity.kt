package com.example.demoapp.adapter

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.demoapp.R

class BleTestActivity : AppCompatActivity() {

    private val TAG = "BleTestActivity"

    private val PERMISSION_REQUEST_CODE = 2001

    private lateinit var listView: ListView
    private lateinit var checkButton: Button
    private lateinit var adapter: ArrayAdapter<String>

    private val devices = mutableListOf<BluetoothDevice>()

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_test)

        listView = findViewById(R.id.deviceList)
        checkButton = findViewById(R.id.checkButton)

        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = devices[position]
            Log.e(TAG, "用户点击设备，准备连接 → ${device.address}")

            connectToDevice(device)
        }

        checkButton.setOnClickListener {
            checkPermissionAndRun { checkSystemConnectionAndForceDisconnect() }
        }

        // 扫描也需要权限
        checkPermissionAndRun { startScan() }
    }
    private fun connectToDevice(device: BluetoothDevice) {

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermissionAndRun { connectToDevice(device) }
            return
        }

        Log.e(TAG, "connectToDevice() → 开始连接 ${device.address}")

        val gatt = device.connectGatt(this, false, object : BluetoothGattCallback() {

            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                Log.e(TAG, "连接回调：${device.address}  status=$status  newState=$newState")

                when (newState) {

                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.e(TAG, "连接成功 → ${device.address}")
                        Log.e(TAG, "开始发现服务")
                        gatt.discoverServices()
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.e(TAG, "连接断开 → ${device.address}")
                        gatt.close()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Log.e(TAG, "服务发现完成 → ${device.address}, status=$status")

                // 你可以在这里继续做 GATT 读写
            }
        })
    }



    // --------------------------
    // 权限处理（Android 12 必须动态申请）
    // --------------------------
    private fun checkPermissionAndRun(task: () -> Unit) {
        val requiredPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )

        val missing = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            task()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.e(TAG, "所有权限已授予")
                startScan()
            } else {
                Toast.makeText(this, "未授予 BLE 相关权限", Toast.LENGTH_LONG).show()
            }
        }
    }


    // --------------------------
    // 开始扫描
    // --------------------------
    private fun startScan() {

        Log.e(TAG, "开始扫描 BLE 设备")

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "请先打开蓝牙", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return

            if (!devices.contains(device)&&device.address.contains("14:5F:31")) {
                devices.add(device)
                adapter.add("${device.address}  |  ${device.name ?: "未知设备"}")
                adapter.notifyDataSetChanged()

                Log.e(TAG, "扫描到设备: ${device.address} name=${device.name}")
            }
        }
    }


    // --------------------------
    // 检测系统层连接情况
    // --------------------------
    private fun checkSystemConnectionAndForceDisconnect() {

        Log.e(TAG, "开始获取系统层已连接设备")

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermissionAndRun { checkSystemConnectionAndForceDisconnect() }
            return
        }

        val connected = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        Log.e(TAG, "系统层已连接设备数量: ${connected.size}")

        if (connected.isEmpty()) {
            Toast.makeText(this, "系统没有连接设备", Toast.LENGTH_SHORT).show()
            return
        }

        connected.forEach { device ->
            Log.e(TAG, "系统连接 → ${device.address}")
            forceDisconnectUsingTempGatt(device)
        }
    }


    // --------------------------
    // 使用临时 GATT 强制断开系统连接
    // --------------------------
    private fun forceDisconnectUsingTempGatt(device: BluetoothDevice) {

        Log.e(TAG, "创建临时 GATT 用于强制断开 ${device.address}")

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        device.connectGatt(this, false, object : BluetoothGattCallback() {

            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                Log.e(TAG, "临时 GATT 回调: ${device.address}, status=$status, newState=$newState")

                when (newState) {

                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.e(TAG, "临时 GATT 已连接，准备断开 → ${device.address}")
                        Handler(Looper.getMainLooper()).postDelayed({
                            gatt.disconnect()
                            Log.e(TAG, "调用 disconnect() → ${device.address}")
                        }, 200)
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.e(TAG, "临时 GATT 收到断开回调 (SUCCESS) → ${device.address}")
                        gatt.close()

                        // ⭐ 在断开回调后检查系统状态
                        checkSystemStateAfterDisconnect(device)
                    }
                }
            }
        })
    }
    private fun checkSystemStateAfterDisconnect(device: BluetoothDevice) {

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "无权限获取系统连接状态")
            return
        }

        // 稍微等一下系统刷新状态
        Handler(Looper.getMainLooper()).postDelayed({
            val connected = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)

            val stillConnected = connected.any { it.address == device.address }

            if (stillConnected) {
                Log.e(TAG, "断开后检查：系统仍占用连接 → ${device.address}")
                Toast.makeText(this, "系统仍占用连接，断开失败", Toast.LENGTH_LONG).show()
            } else {
                Log.e(TAG, "断开后检查：系统已不再连接 → ${device.address}")
                Toast.makeText(this, "断开成功（系统已释放）", Toast.LENGTH_SHORT).show()
            }
        }, 600)  // 600ms 比较稳
    }


    override fun onDestroy() {
        super.onDestroy()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }
    }
}
