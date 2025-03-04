package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.camera.FileUtils.restoreFile
import com.example.camera.FileUtils.saveToGallery
import com.example.demoapp.databinding.ActivityFileListBinding
import java.io.File

class FileListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFileListBinding
    private lateinit var adapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadFiles()
    }

    private fun setupRecyclerView() {
        adapter = FileAdapter(
            onRestoreClick = { file -> restoreFilesToGallery() },
            onSaveClick = { file -> restoreFilesToGallery() }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FileListActivity)
            adapter = this@FileListActivity.adapter
        }
    }

    private fun loadFiles() {
        val hiddenDir = File(cacheDir, "hidden")
        val files = hiddenDir.listFiles()?.filter { it.extension == "jpeg" } ?: emptyList()
        adapter.submitList(files)
    }

    private fun restoreFilesToGallery() {
        // 获取 cache/hidden 目录
        val hiddenDir = File(cacheDir, "hidden")

        if (!hiddenDir.exists() || !hiddenDir.isDirectory) {
            return // 目录不存在，直接返回
        }

        // 过滤出所有 .jpeg 文件
        val jpegFiles = hiddenDir.listFiles { _, name ->
            name.lowercase().endsWith(".jpeg")
        } ?: return

        // 依次恢复每个 JPEG 文件
        for (file in jpegFiles) {
            restoreFileToGallery(this, file)
        }
    }

    private fun restoreFileToGallery(context: Context, file: File) {
        val contentResolver = context.contentResolver
        val fileName = file.name
        val mimeType = "image/jpeg"

        // 定义要存储的位置（DCIM 或 PICTURES 目录）
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Restored")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        // 插入到 MediaStore
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it).use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream!!)
                }
            }
            // 更新状态为已完成
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(it, contentValues, null, null)
        }
        file.delete()
    }
    private fun saveToGallery(file: File) {
        // 实现保存到相册逻辑
    }

}

