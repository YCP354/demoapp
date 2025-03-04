package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FileUtils {

    fun restoreFile(context: Context, file: File): Boolean {
        return try {
            val newFile = File(file.parent, "${file.nameWithoutExtension}.jpg")
            if (file.renameTo(newFile)) {
                scanFile(context, newFile)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun saveToGallery(context: Context, file: File): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, file)
        } else {
            saveLegacyWay(context, file)
        }
    }

    private fun saveViaMediaStore(context: Context, file: File): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                resolver.openOutputStream(uri)?.use { os ->
                    FileInputStream(file).use { fis ->
                        fis.copyTo(os)
                    }
                }
                true
            } ?: false
        } catch (e: IOException) {
            false
        }
    }

    private fun saveLegacyWay(context: Context, file: File): Boolean {
        return try {
            val targetDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES)
            val targetFile = File(targetDir, "${System.currentTimeMillis()}.jpg")
            
            FileInputStream(file).use { fis ->
                FileOutputStream(targetFile).use { fos ->
                    fis.copyTo(fos)
                }
            }
            scanFile(context, targetFile)
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun scanFile(context: Context, file: File) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("image/jpeg"),
            null
        )
    }
}