package com.example.demoapp

import UsbPrinterHelper
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import com.example.utils.PrinterDataGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.roundToInt

class PrinterActivity : AppCompatActivity() {


    private lateinit var btnSelectPhoto: Button
    private lateinit var btnPrint: Button
    private lateinit var ivPreview: ImageView
    private lateinit var tvStatus: TextView

    private var selectedUri: Uri? = null

    // 相册选择器
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedUri = it
                ivPreview.setImageURI(it) // 显示预览
                tvStatus.text = "图片已选择，准备打印"
                btnPrint.isEnabled = true
            }
        }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer)


        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnPrint = findViewById(R.id.btnPrint)
        ivPreview = findViewById(R.id.ivPreview)
        tvStatus = findViewById(R.id.tvStatus)
        btnPrint.isEnabled = false


        // 1. 点击选择图片
        btnSelectPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
//            val pdfFile = getPdfFromAssets("usb_dump.pdf")

//            DeepImageAnalyzer.analyzeImageStream(pdfFile)
        }

        // 2. 点击打印
        btnPrint.setOnClickListener {
            selectedUri?.let { uri ->
                processAndPrint(uri)
            }
//            val outFile = File(filesDir, "qr_usb_dump.pdf")    // 或 pdf / pcl / 任意文件名
//            copyAssetToFile(this, "qr_usb_dump.pdf", outFile)
//            HexDumpAnalyzer.analyze(this, outFile.absolutePath)
//            StreamHunter.hunt(this,outFile)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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

    fun copyAssetToFile(context: Context, assetName: String, outFile: File): File {
        if (!outFile.exists()) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile
    }

    private fun processAndPrint(uri: Uri) {
        btnPrint.isEnabled = false
        tvStatus.text = "处理中..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 读取并缩放图片 (防止 OOM)
                val bitmap = loadScaledBitmap(uri) ?: throw Exception("图片加载失败")

                // 2. 将图片画在白色背景上 (处理透明 PNG 变黑问题)
                val finalBitmap =
                    createBitmap(bitmap.width, bitmap.height)
                val canvas = Canvas(finalBitmap)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                bitmap.recycle() // 释放原图

                // 3. 生成 PCLm 文件
                var outputFile = File(cacheDir, "temp_print.pdf")
//                PclmGenerator.generatePclmPdf(finalBitmap, outputFile)
                copyAssetToFileDir(this@PrinterActivity,"libhplip.so","libhplip.so",true)
                outputFile = PrinterDataGenerator.generateFromUri(this@PrinterActivity,uri,"/data/data/com.example.demoapp/files/libhplip.so")
                finalBitmap.recycle() // 释放合成图

// --- 插入诊断代码 ---
                // 1. 定位参考文件 (请确保你已经 push 进去了)
//                val refFile = getPdfFromAssets("usb_dump.pdf")

                // 2. 执行对比
//                PdfDiagnostics.compareFiles(outputFile, refFile)

                withContext(Dispatchers.Main) {
                    tvStatus.text = "正在连接打印机..."
                }

                // 4. 查找打印机
                val printers = UsbPrinterHelper.getConnectedPrinters(this@PrinterActivity)
                if (printers.isEmpty()) {
                    throw Exception("未找到 USB 打印机，请检查连接")
                }
                val device = printers[0]

//                outputFile = getPdfFromAssets("qr_usb_dump.pdf")
                // 5. 发送打印
                val result =
                    UsbPrinterHelper.printPclmFile(this@PrinterActivity, device, outputFile)

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        tvStatus.text = "✅ 指令已发送！"
                        Toast.makeText(this@PrinterActivity, "发送成功", Toast.LENGTH_LONG).show()
                    } else {
                        tvStatus.text = "❌ 失败: ${result.exceptionOrNull()?.message}"
                    }
                    btnPrint.isEnabled = true
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "错误: ${e.message}"
                    e.printStackTrace()
                    btnPrint.isEnabled = true
                }
            }
        }
    }

    // 辅助：加载缩略图，避免加载原图导致内存溢出
    private fun loadScaledBitmap(uri: Uri): Bitmap? {
        val input: InputStream = contentResolver.openInputStream(uri) ?: return null
        // 这里的目标宽度设为 1200，大约是 A4 纸 150DPI 的精度，足够清晰且快
        val targetWidth = 1200

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(input, null, options)
        input.close()

        var inSampleSize = 1
        if (options.outWidth > targetWidth) {
            inSampleSize = (options.outWidth.toFloat() / targetWidth).roundToInt()
        }

        val input2 = contentResolver.openInputStream(uri)
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        return BitmapFactory.decodeStream(input2, null, options)
    }

    fun copyAssetToFileDir(
        context: Context,
        assetFileName: String,
        targetFileName: String = assetFileName,
        overwrite: Boolean = true
    ): File? {
        return try {
            val targetFile = File(context.filesDir, targetFileName)

            // 文件已存在且不覆盖
            if (targetFile.exists() && !overwrite) {
                return targetFile
            }

            // 打开 assets 输入流
            val inputStream = context.assets.open(assetFileName)

            // 创建输出流
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(4096)
            var length: Int

            // 拷贝数据
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            targetFile

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun printRawTextTest(context: Context) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val printers = UsbPrinterHelper.getConnectedPrinters(context)
        if (printers.isEmpty()) return
        val device = printers[0]

        // 1. 准备纯文本数据
        // \u001B%-12345X: 通用退出指令 (UEL)
        // @PJL ENTER LANGUAGE=PCL: 尝试切换到 PCL 模式 (而不是 PCLm)
        val content = """
        %-12345X@PJL ENTER LANGUAGE=PCL
        @PJL RESET
        
        Hello World!
        This is ProcessSession Raw Text Test.
        1234567890
        
        
        %-12345X
    """.trimIndent()

        // 注意： 是换页符 (Form Feed, 0x0C)，告诉打印机“这页打完了，吐纸”

        // 2. 发送
        withContext(Dispatchers.IO) {
            val connection = usbManager.openDevice(device) ?: return@withContext
            val ifacePair = UsbPrinterHelper.findPrinterInterface(device) ?: return@withContext
            val iface = ifacePair.first
            val endpoint = ifacePair.second

            connection.claimInterface(iface, true)

            val bytes = content.toByteArray(Charsets.US_ASCII)
            connection.bulkTransfer(endpoint, bytes, bytes.size, 5000)

            connection.releaseInterface(iface)
            connection.close()
        }
    }
}