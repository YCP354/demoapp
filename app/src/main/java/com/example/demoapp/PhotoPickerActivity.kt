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
import androidx.activity.result.contract.ActivityResultContracts

class PhotoPickerActivity : BaseActivity() {

    override val resId: Int
        get() = R.layout.activity_photo_picker
    private lateinit var pickSingleImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickMultipleImagesLauncher: ActivityResultLauncher<Intent>
    private lateinit var imageView: ImageView

    override fun initView() {
        super.initView()

        imageView = findViewById(R.id.imageView)
        val pickSingleImageButton: Button = findViewById(R.id.pickSingleImageButton)
        val pickMultipleImagesButton: Button = findViewById(R.id.pickMultipleImagesButton)

        // 注册单张图片选择器
        pickSingleImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    imageView.setImageURI(uri)
                }
            }
        }

        // 注册多张图片选择器
        pickMultipleImagesLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val clipData = result.data?.clipData
                if (clipData != null) {
                    // 多张图片选择
                    for (i in 0 until clipData.itemCount) {
                        val imageUri = clipData.getItemAt(i).uri
                        // 这里只是处理第一张图片，你可以根据需求加载更多图片
                        if (i == 0) {
                            imageView.setImageURI(imageUri)
                        }
                    }
                } else {
                    // 单张图片选择
                    val imageUri = result.data?.data
                    if (imageUri != null) {
                        imageView.setImageURI(imageUri)
                    }
                }
            }
        }

        // 单张图片选择按钮点击事件
        pickSingleImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickSingleImageLauncher.launch(intent)
        }

        // 多张图片选择按钮点击事件
        pickMultipleImagesButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                pickMultipleImagesLauncher.launch(intent)
        }
    }
}