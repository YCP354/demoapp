package com.example.demoapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.demoapp.adapter.ImageAdapter

class PhotoPickerActivity : BaseActivity() {

    override val resId: Int
        get() = R.layout.activity_photo_picker

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val selectedUris = mutableListOf<Uri>()


    override fun initView() {
        super.initView()
        val pickSingleImageButton: Button = findViewById(R.id.pickSingleImageButton)
        val pickMultipleImagesButton: Button = findViewById(R.id.pickMultipleImagesButton)
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        imageAdapter = ImageAdapter(selectedUris)
        recyclerView.adapter = imageAdapter

        // 单张图片选择
        val pickSingleMediaLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
                if (uri != null) {
                    selectedUris.clear()
                    selectedUris.add(uri)
                    imageAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "未选择任何图片", Toast.LENGTH_SHORT).show()
                }
            }

        // 多张图片选择
        val pickMultipleMediaLauncher =
            registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(3)) { uris: List<Uri> ->
                if (uris.isNotEmpty()) {
                    selectedUris.clear()
                    selectedUris.addAll(uris)
                    imageAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "选择了 ${uris.size} 张图片", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "未选择任何图片", Toast.LENGTH_SHORT).show()
                }
            }

        // 单张图片按钮点击事件
        pickSingleImageButton.setOnClickListener {
            pickSingleMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

        }

        // 多张图片按钮点击事件
        pickMultipleImagesButton.setOnClickListener {
            pickMultipleMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}