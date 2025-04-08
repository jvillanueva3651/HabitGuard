/**===========================================================================================
 * CameraHelper for handling camera-related tasks
 * REF    : USE_BY -> .SignupActivity
 * Purpose: Modularize camera-related functionality for better code organization.
 * Fun:  1. Handles camera permission requests,
 *       2. Camera initialization,
 *       3. Photo capture functionality.
============================================================================================*/
package com.washburn.habitguard

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class CameraHelper(private val activity: AppCompatActivity) {
    var onPhotoTaken: ((Uri) -> Unit)? = null
    var onPermissionDenied: (() -> Unit)? = null

    private val cameraLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra("photoUri")?.let { uriString ->
                onPhotoTaken?.invoke(uriString.toUri())
            }
        }
    }

    private val cameraPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            onPermissionDenied?.invoke()
        }
    }

    fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val cameraIntent = Intent(activity, CameraActivity::class.java)
        cameraLauncher.launch(cameraIntent)
    }
}