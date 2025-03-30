/**===========================================================================================
 * CameraActivity for userProfile picture
 * REF    : USE_BY -> .SignupActivity
 *          LAYOUT -> layout/activity_camera.xml
 * Purpose: Capturing photos using the device's camera.
 * Fun:  1. Handles camera permission requests,
 *       2. Camera initialization,
 *       3. Photo capture functionality.
============================================================================================*/
package com.washburn.habitguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.washburn.habitguard.databinding.ActivityCameraBinding
import java.io.File
import java.util.Locale
import java.text.SimpleDateFormat

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null // ImageCapture use case for taking photos

    // Activity result launcher for camera permission request
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request camera permission
        if (allPermissionsGranted()) {
            startCamera()
            binding.btnCapture.setOnClickListener { takePhoto() }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // From manifest, check if all required permissions are granted
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // Initializes and starts the camera preview
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.surfaceProvider = binding.previewView.surfaceProvider
                }

                imageCapture = ImageCapture.Builder().build()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Captures a photo and saves it to a file locally
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Create a timestamped file name
        val photoFile = createTimestampedFile()

        // Set up output options
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Take the picture
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    handleImageCaptureSuccess(photoFile)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraActivity", "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    private fun handleImageCaptureSuccess(photoFile: File) {
        val savedUri = Uri.fromFile(photoFile)
        showToast("Photo capture succeeded: $savedUri")

        // Return the photo URI to the calling activity
        val resultIntent = Intent().apply {
            putExtra("photoUri", savedUri.toString())
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun createTimestampedFile(): File {
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        return File(externalMediaDirs.firstOrNull(), "$timestamp.jpg")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}