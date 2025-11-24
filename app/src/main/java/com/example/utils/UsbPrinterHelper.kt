package com.example.utils

import android.content.Context
import android.hardware.usb.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object UsbPrinterHelper {

    private const val TAG = "UsbPrinterHelper"
    // 16KB 是 USB 传输的标准安全块大小，也是你抓包看到的大小
    private const val CHUNK_SIZE = 16384 
    private const val TIMEOUT_MS = 5000

    /**
     * 步骤 1: 查找所有连接的 USB 打印机设备
     */
    fun getConnectedPrinters(context: Context): List<UsbDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        val printers = mutableListOf<UsbDevice>()

        for (device in deviceList.values) {
            // 方法 A: 直接检查设备类是否为 Printer (7)
            if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) {
                printers.add(device)
                continue
            }

            // 方法 B: 如果设备类是 0 (定义在接口层)，则遍历接口检查
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                    printers.add(device)
                    break
                }
            }
        }
        return printers
    }

    /**
     * 步骤 2, 3, 4, 5: 执行打印的核心挂起函数
     * @param context 上下文
     * @param usbDevice 目标 USB 设备
     * @param pdfFile 要发送的 PDF 文件
     * @return Result<String> 成功返回 Success，失败返回 Failure 包含错误信息
     */
    suspend fun printPdf(context: Context, usbDevice: UsbDevice, pdfFile: File): Result<String> {
        return withContext(Dispatchers.IO) {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            // --- 检查权限 ---
            if (!usbManager.hasPermission(usbDevice)) {
                return@withContext Result.failure(Exception("没有 USB 权限"))
            }

            var connection: UsbDeviceConnection? = null
            var iface: UsbInterface? = null

            try {
                // --- 打开连接 ---
                connection = usbManager.openDevice(usbDevice)
                    ?: return@withContext Result.failure(Exception("无法打开连接 (openDevice failed)"))

                // --- 寻找正确的接口和端点 ---
                val (foundInterface, foundEndpoint) = findPrinterInterface(usbDevice)
                    ?: return@withContext Result.failure(Exception("找不到打印机接口或输出端点"))

                iface = foundInterface

                // --- 独占接口 (Claim Interface) ---
                // true 表示强制断开系统内核驱动（如果有的话）
                if (!connection.claimInterface(iface, true)) {
                    return@withContext Result.failure(Exception("无法获取接口控制权 (claimInterface failed)"))
                }

                // --- 读取文件并分包发送 ---
                val fileInputStream = FileInputStream(pdfFile)
                val buffer = ByteArray(CHUNK_SIZE)
                var bytesRead: Int
                var totalBytesSent = 0

                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    // bulkTransfer 返回的是实际发送的字节数，负数表示失败
                    val result = connection.bulkTransfer(foundEndpoint, buffer, bytesRead, TIMEOUT_MS)
                    
                    if (result < 0) {
                        fileInputStream.close()
                        return@withContext Result.failure(Exception("传输数据失败，错误码: $result"))
                    }
                    totalBytesSent += result
                }

                fileInputStream.close()
                return@withContext Result.success("打印成功，已发送 $totalBytesSent 字节")

            } catch (e: Exception) {
                return@withContext Result.failure(e)
            } finally {
                // --- 释放资源 ---
                if (iface != null) {
                    connection?.releaseInterface(iface)
                }
                connection?.close()
            }
        }
    }

    /**
     * 辅助方法：寻找拥有 Bulk-Out 端点的接口
     */
    private fun findPrinterInterface(device: UsbDevice): Pair<UsbInterface, UsbEndpoint>? {
        // 优先寻找声明为打印机类的接口
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            // 检查接口下的端点
            for (j in 0 until iface.endpointCount) {
                val endpoint = iface.getEndpoint(j)
                // 我们需要：类型是 BULK 且 方向是 OUT (发给打印机)
                if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    endpoint.direction == UsbConstants.USB_DIR_OUT) {
                    return Pair(iface, endpoint)
                }
            }
        }
        return null
    }
}