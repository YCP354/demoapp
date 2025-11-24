import android.content.Context
import android.hardware.usb.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

object UsbPrinterHelper {

    // PJL (Printer Job Language) 指令，用于唤醒 HP 打印机并告知 PCLM 模式
    private val PJL_HEADER = "\u001B%-12345X@PJL ENTER LANGUAGE=PCLM\r\n".toByteArray(Charsets.US_ASCII)
    private val PJL_FOOTER = "\u001B%-12345X".toByteArray(Charsets.US_ASCII)

    private const val CHUNK_SIZE = 16384
    private const val TIMEOUT_MS = 5000

    fun getConnectedPrinters(context: Context): List<UsbDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val printers = mutableListOf<UsbDevice>()
        for (device in usbManager.deviceList.values) {
            // 简单筛选：类为 Printer (7) 或者 Interface 类为 Printer
            if (isPrinterDevice(device)) {
                printers.add(device)
            }
        }
        return printers
    }

    private fun isPrinterDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true
        for (i in 0 until device.interfaceCount) {
            if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
        }
        return false
    }

    suspend fun printPclmFile(context: Context, usbDevice: UsbDevice, pclmFile: File): Result<String> {
        return withContext(Dispatchers.IO) {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            if (!usbManager.hasPermission(usbDevice)) {
                return@withContext Result.failure(Exception("没有 USB 权限"))
            }

            var connection: UsbDeviceConnection? = null
            var iface: UsbInterface? = null

            try {
                connection = usbManager.openDevice(usbDevice)
                    ?: return@withContext Result.failure(Exception("连接打开失败"))

                // 寻找 OUT 端点
                val pair = findPrinterInterface(usbDevice)
                    ?: return@withContext Result.failure(Exception("未找到打印端口"))

                iface = pair.first
                val endpoint = pair.second

                connection.claimInterface(iface, true)

                // 1. 发送 PJL 头 (唤醒)
                connection.bulkTransfer(endpoint, PJL_HEADER, PJL_HEADER.size, TIMEOUT_MS)

                // 2. 发送 PCLm 文件内容
                val fis = FileInputStream(pclmFile)
                val buffer = ByteArray(CHUNK_SIZE)
                var len: Int
                while (fis.read(buffer).also { len = it } != -1) {
                    connection.bulkTransfer(endpoint, buffer, len, TIMEOUT_MS)
                }
                fis.close()

                // 3. 发送 PJL 尾 (结束)
                connection.bulkTransfer(endpoint, PJL_FOOTER, PJL_FOOTER.size, TIMEOUT_MS)

                return@withContext Result.success("发送完成")
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            } finally {
                iface?.let { connection?.releaseInterface(it) }
                connection?.close()
            }
        }
    }

    internal fun findPrinterInterface(device: UsbDevice): Pair<UsbInterface, UsbEndpoint>? {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            for (j in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(j)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    ep.direction == UsbConstants.USB_DIR_OUT) {
                    return Pair(iface, ep)
                }
            }
        }
        return null
    }
}