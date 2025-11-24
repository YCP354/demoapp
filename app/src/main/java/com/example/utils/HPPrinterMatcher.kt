package com.example.utils

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.os.Bundle
import android.util.Log

/**
 * HP 打印机识别器，根据 IEEE1284 Device ID 匹配型号
 */
object HPPrinterMatcher {

    private const val TAG = "HPPrinterMatcher"

    // ====== 你的 HP 打印机数据库（可继续扩展）=======
    private val hpDb = listOf(
        "HP DeskJet 970C|90-156,67,157-160,72,161-177;0|;-1|0-7;2|0-2;2|DeviceModel=hp-dj_970c",
        "HP DeskJet 975C|90-156,67,157-160,72,161-177;0|;-1|0-7;2|0-2;2|DeviceModel=hp-dj_975c",
        "HP DeskJet 980C|90-156,67,157-160,72,161-177;0|;-1|0-7;2|0-2;2|DeviceModel=hp-dj_980c",
        "HP DeskJet 990C|90-156,67,157-160,72,161-177;0|;-1|0-7;2|0-2;2|DeviceModel=hp-dj_990c",
        "HP DeskJet 995C|90-156,67,157-160,72,161-177;0|;-1|0-7;2|0-2;2|DeviceModel=hp-dj_995c",
        "HP e-printer e20|0-89;0|;-1|0-7;2|;-1|DeviceModel=hp-e-printer_e20",
        "HP LaserJet 1010|178-187;0|0-10;10|12-13,15-16;1|0-2;2|DeviceModel=hp-lj_1010",
        "HP LaserJet 1012|178-187;0|0-10;10|12-13,15-16;1|0-2;2|DeviceModel=hp-lj_1012",
        "HP LaserJet 1015|178-187;0|0-10;10|12-13,15-16;1|0-2;2|DeviceModel=hp-lj_1015",
        "HP LaserJet 1022|178-187;0|0-10;10|12-13,15-16;1|0-2;2|DeviceModel=hp-lj_1022",
        "HP LaserJet 1100|178-187;0|0-10;10|12-13,15-16;1|0-2;2|DeviceModel=hp-lj_1100"
    )

    /**
     * 拆分数据库行，变成易处理的数据结构
     */
    data class HPDevice(
        val name: String,
        val signature: String,
        val deviceModel: String
    )

    private val parsedDb: List<HPDevice> = hpDb.map { line ->
        val parts = line.split("|")
        HPDevice(
            name = parts[0],
            signature = parts[1], // signature 用于匹配 IEEE1284 id
            deviceModel = parts.last().substringAfter("=")
        )
    }

    /**
     * 从 USB Device 读取 IEEE1284 Device ID
     */
    fun readDeviceId(connection: UsbDeviceConnection, device: UsbDevice): String {
        val buffer = ByteArray(1024)
        val len = connection.controlTransfer(
            0xA1, 0x00, 0x00, 0x00,
            buffer, buffer.size, 2000
        )
        return if (len > 0) {
            String(buffer, 0, len)
        } else {
            ""
        }
    }

    /**
     * 在数据库中查找匹配的 HP 打印机
     */
    fun matchPrinter(deviceId: String): HPDevice? {
        parsedDb.forEach { hp ->
            // 简单包含匹配（真正匹配逻辑更复杂）
            if (deviceId.contains(hp.deviceModel, ignoreCase = true) ||
                deviceId.contains(hp.name.replace(" ", ""), ignoreCase = true)
            ) {
                return hp
            }
        }
        return null
    }

    /**
     * 对外方法：识别并打印日志
     */
    fun detectAndLog(connection: UsbDeviceConnection, device: UsbDevice) {
        Log.d(TAG, "——————————————")
        Log.d(TAG, "开始识别打印机...")

        // Step 1：读取 IEEE1284 ID
        val rawId = readDeviceId(connection, device)

        if (rawId.isNullOrBlank()) {
            Log.e(TAG, "读取失败：未读到 IEEE1284 Device ID")
            Log.d(TAG, "——————————————")
            return
        }

        Log.d(TAG, "读取到的原始 IEEE1284 Device ID:")
        Log.d(TAG, rawId)

        // Step 2：格式化（去掉乱码）
        val id = rawId.replace(Regex("[^ -~]"), "")
        Log.d(TAG, "格式化后的 Device ID:")
        Log.d(TAG, id)
        Log.d(TAG, "——————————————")

        // Step 3：解析 ID → 字段 map
        val parsed = parseDeviceId(id)
        Log.d(TAG, "解析出的字段：")
        parsed.forEach { (k, v) -> Log.d(TAG, "$k = $v") }

        Log.d(TAG, "——————————————")

        // Step 4：匹配数据库
        val matched = matchPrinter(id)

        if (matched != null) {
            Log.d(TAG, "识别到 HP 打印机型号：${matched.name}")
            Log.d(TAG, "驱动标识：${matched.deviceModel}")
            Log.d(TAG, "特征码：${matched.signature}")
        } else {
            Log.d(TAG, "未在数据库中找到匹配的 HP 打印机")
        }

        Log.d(TAG, "——————————————")
        Log.d(TAG, "设备基本信息：")
        Log.d(TAG, "deviceName: ${device.deviceName}")
        Log.d(TAG, "productId: ${device.productId}")
        Log.d(TAG, "vendorId: ${device.vendorId}")
        Log.d(TAG, "serialNumber: ${device.serialNumber}")
        Log.d(TAG, "deviceId: ${device.deviceId}")
        Log.d(TAG, "deviceProtocol: ${device.deviceProtocol}")
        Log.d(TAG, "device(json): ${H5JsonDecoderUtils.toJson(device)}")
        Log.d(TAG, "——————————————")

        // Step 5：补全 Bundle（与你反编译看到的 bVar 完全对应）
        val bundle = Bundle().apply {
            putString("name", parsed["MDL"] ?: device.deviceName)
            putInt("product_id", device.productId)
            putInt("vendor_id", device.vendorId)
            putString("serial_number", device.serialNumber ?: "")

            // USB 接口信息（一般从 device.getInterface(0) 获取）
            val intf = device.getInterface(0)
            putInt("interface_index", 0)
            putInt("interface_protocol", intf.interfaceProtocol)

            // 找输出、输入端点
            var outIndex = -1
            var inIndex = -1
            for (i in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(i)
                if (ep.direction == UsbConstants.USB_DIR_OUT) outIndex = i
                if (ep.direction == UsbConstants.USB_DIR_IN) inIndex = i
            }
            putInt("output_endpoint_index", outIndex)
            putInt("input_endpoint_index", inIndex)

            // Device ID 字段
            putString("model", parsed["MDL"])
            putString("mfg", parsed["MFG"])
            putString("mdl", parsed["MDL"])
            putString("cmd", parsed["CMD"])
            putString("urf", parsed["URF"])
        }

        Log.d(TAG, "最终 Bundle 内容：${bundle.keySet().associateWith { bundle.get(it) }}")
        Log.d(TAG, "——————————————")
    }
    fun parseDeviceId(id: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        id.split(";").forEach { part ->
            val kv = part.split(":")
            if (kv.size == 2) {
                map[kv[0].trim()] = kv[1].trim()
            }
        }
        return map
    }
}