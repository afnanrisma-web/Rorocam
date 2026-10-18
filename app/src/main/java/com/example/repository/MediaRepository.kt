package com.example.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MediaRepository(private val context: Context) {

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    private val mediaDir: File = File(context.filesDir, "retrocam_media").apply {
        if (!exists()) mkdirs()
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        // Pre-seed sample vintage photo if empty
        ensureSamplePhotoPresent()
        loadMediaFiles()
    }

    private fun ensureSamplePhotoPresent() {
        val sampleFile = File(mediaDir, "sample_retro_london_cafe.jpg")
        if (!sampleFile.exists()) {
            try {
                // Try to load from drawable resource
                val resId = context.resources.getIdentifier("sample_retro_photo", "drawable", context.packageName)
                if (resId != 0) {
                    val inputStream = context.resources.openRawResource(resId)
                    val outputStream = FileOutputStream(sampleFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadMediaFiles() = withContext(Dispatchers.IO) {
        val files = mediaDir.listFiles() ?: emptyArray()
        val items = files.mapNotNull { file ->
            val isVid = file.extension.lowercase() in listOf("mp4", "mkv", "webm")
            val isPic = file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
            if (isVid || isPic) {
                MediaItem(
                    id = file.name,
                    uri = file.absolutePath,
                    name = file.nameWithoutExtension,
                    isVideo = isVid,
                    timestamp = file.lastModified(),
                    filterUsed = if (file.name.contains("_")) file.name.substringBefore("_") else "Normal"
                )
            } else null
        }.sortedByDescending { it.timestamp }

        _mediaList.value = items
    }

    suspend fun savePhoto(bitmap: Bitmap, filterName: String): MediaItem = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanFilter = filterName.replace(" ", "_")
        val fileName = "${cleanFilter}_$timeStamp.jpg"
        val destFile = File(mediaDir, fileName)

        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.flush()
        }

        val item = MediaItem(
            id = fileName,
            uri = destFile.absolutePath,
            name = fileName,
            isVideo = false,
            timestamp = destFile.lastModified(),
            filterUsed = filterName,
            width = bitmap.width,
            height = bitmap.height
        )

        loadMediaFiles()
        item
    }

    suspend fun createVideoOutputFile(): File = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "DUAL_VIDEO_$timeStamp.mp4"
        File(mediaDir, fileName)
    }

    suspend fun deleteMedia(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val file = File(item.uri)
        val deleted = if (file.exists()) file.delete() else false
        if (deleted) {
            loadMediaFiles()
        }
        deleted
    }

    fun loadBitmap(uri: String): Bitmap? {
        return try {
            val file = File(uri)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
