package com.example.camera

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
            onRestoreClick = { file -> restoreFile(file) },
            onSaveClick = { file -> saveToGallery(file) }
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

    private fun restoreFile(file: File) {
        // 实现恢复逻辑
    }

    private fun saveToGallery(file: File) {
        // 实现保存到相册逻辑
    }
}