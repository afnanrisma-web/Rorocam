package com.example.camera

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.core.content.ContextCompat

object CameraHelper {

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if the hardware supports concurrent camera streaming (front and back active simultaneously).
     */
    fun supportsConcurrentCameras(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val concurrentSets = cameraManager.concurrentCameraIds
            concurrentSets.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Finds front and back camera IDs.
     */
    fun getFrontAndBackCameraIds(context: Context): Pair<String?, String?> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var backId: String? = null
        var frontId: String? = null

        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK && backId == null) {
                    backId = id
                } else if (facing == CameraCharacteristics.LENS_FACING_FRONT && frontId == null) {
                    frontId = id
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(backId, frontId)
    }
}
