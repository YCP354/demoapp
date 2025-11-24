package com.example.demoapp

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.utils.UsbPrinterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.experimental.or
import kotlin.math.min

class PrinterActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer)


        // 3. 按钮点击：开始搜索并连接
        findViewById<Button>(R.id.btn_print).setOnClickListener {
            startPrintingProcess()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
    }

    fun startPrintingProcess() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        // 1. 获取打印机
        val printers = UsbPrinterHelper.getConnectedPrinters(this)
        if (printers.isEmpty()) {
            println("未找到打印机")
            return
        }

        val targetDevice = printers[0] // 简单起见，取第一个

        // 2. 检查权限
        if (!usbManager.hasPermission(targetDevice)) {
            // 请求权限 (需要你自己实现 PendingIntent 接收回调)
            // val permissionIntent = ...
            // usbManager.requestPermission(targetDevice, permissionIntent)
            println("请先授予 USB 权限")
            return
        }

        // 3. 准备文件 (这里假设你已经生成了 PDF)
//        val pdfFile = File(getExternalFilesDir(null), "usb_dump.pdf")
        val pdfFile = getPdfFromAssets("usb_dump.pdf")

        if (!pdfFile.exists()) {
            println("PDF 文件不存在")
            return
        }

        // 4. 执行打印 (协程中调用)
        lifecycleScope.launch {
            println("开始发送数据...")
            val result = UsbPrinterHelper.printPdf(this@PrinterActivity, targetDevice, pdfFile)

            if (result.isSuccess) {
                println("✅ ${result.getOrNull()}")
            } else {
                println("❌ 失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    // 从 assets 中读取 PDF 到临时文件
    private fun getPdfFromAssets(fileName: String): File {
        val inputStream = assets.open(fileName)

        // 创建一个临时文件
        val outFile = File(getExternalFilesDir(null), fileName)
        val outputStream = FileOutputStream(outFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return outFile
    }

}