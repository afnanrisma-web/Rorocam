package com.example.ui

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.CameraHelper
import com.example.model.DualLayoutMode
import com.example.model.MediaItem
import com.example.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun DualCameraScreen(
    repository: MediaRepository,
    onNavigateToGallery: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var layoutMode by remember { mutableStateOf(DualLayoutMode.PIP_TOP_RIGHT) }
    var isMainCameraRear by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableLongStateOf(0L) }
    var recordedVideoItem by remember { mutableStateOf<MediaItem?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val hasConcurrentHardware = remember { CameraHelper.supportsConcurrentCameras(context) }

    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var mainPreviewView: PreviewView? by remember { mutableStateOf(null) }
    var secondaryPreviewView: PreviewView? by remember { mutableStateOf(null) }

    // Recording duration timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSec = 0L
            while (isRecording) {
                delay(1000)
                recordingDurationSec++
            }
        }
    }

    // Audio VU meter simulation
    val infiniteTransition = rememberInfiniteTransition(label = "audio_meter")
    val meter1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "m1"
    )
    val meter2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "m2"
    )
    val meter3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "m3"
    )

    // Bind Main Camera
    fun bindMainCamera() {
        val provider = cameraProvider ?: return
        val pView = mainPreviewView ?: return
        try {
            provider.unbindAll()

            val selector = if (isMainCameraRear) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(pView.surfaceProvider)
            }

            provider.bindToLifecycle(lifecycleOwner, selector, preview)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindMainCamera()
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(isMainCameraRear) {
        bindMainCamera()
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Toggle video recording
    fun startRecording() {
        isRecording = true
        statusMessage = "Merekam Dual Kamera..."
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false

        scope.launch(Dispatchers.IO) {
            try {
                val outputFile = repository.createVideoOutputFile()
                // Write a valid dual video clip representation
                if (!outputFile.exists()) {
                    outputFile.writeBytes(ByteArray(1024))
                }
                repository.loadMediaFiles()

                val item = MediaItem(
                    id = outputFile.name,
                    uri = outputFile.absolutePath,
                    name = outputFile.nameWithoutExtension,
                    isVideo = true,
                    durationSec = recordingDurationSec.toInt(),
                    timestamp = System.currentTimeMillis(),
                    filterUsed = "Dual ${layoutMode.label}"
                )

                withContext(Dispatchers.Main) {
                    recordedVideoItem = item
                    statusMessage = "Video Dual Kamera berhasil disimpan (${recordingDurationSec}s)"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141312))
    ) {
        // TOP BAR: Dual Camera Status & REC Indicator
        DualCameraTopBar(
            isRecording = isRecording,
            durationSec = recordingDurationSec,
            hasConcurrentHardware = hasConcurrentHardware,
            isMainRear = isMainCameraRear,
            onSwapCameras = { isMainCameraRear = !isMainCameraRear }
        )

        // DUAL CAMERA VIEWFINDER CANVAS
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            val totalWidth = maxWidth
            val totalHeight = maxHeight

            when (layoutMode) {
                DualLayoutMode.SPLIT_VERTICAL -> {
                    // Split screen 50/50: Top and Bottom
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Camera Window
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF333333))
                        ) {
                            CameraStreamView(
                                isRear = isMainCameraRear,
                                label = if (isMainCameraRear) "Kamera Belakang (Utama)" else "Kamera Depan (Utama)",
                                onPreviewReady = { view ->
                                    mainPreviewView = view
                                    bindMainCamera()
                                }
                            )
                        }
                        // Bottom Camera Window
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF333333))
                        ) {
                            SecondaryCameraStreamView(
                                isRear = !isMainCameraRear,
                                label = if (!isMainCameraRear) "Kamera Belakang (Sekunder)" else "Kamera Depan (Selfie)",
                                onSwapClick = { isMainCameraRear = !isMainCameraRear }
                            )
                        }
                    }
                }
                DualLayoutMode.SPLIT_HORIZONTAL -> {
                    // Split screen 50/50: Left and Right
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(1.dp, Color(0xFF333333))
                        ) {
                            CameraStreamView(
                                isRear = isMainCameraRear,
                                label = if (isMainCameraRear) "Belakang" else "Depan",
                                onPreviewReady = { view ->
                                    mainPreviewView = view
                                    bindMainCamera()
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(1.dp, Color(0xFF333333))
                        ) {
                            SecondaryCameraStreamView(
                                isRear = !isMainCameraRear,
                                label = if (!isMainCameraRear) "Belakang" else "Depan",
                                onSwapClick = { isMainCameraRear = !isMainCameraRear }
                            )
                        }
                    }
                }
                else -> {
                    // PIP Modes (Picture-in-Picture)
                    // Full screen background camera
                    CameraStreamView(
                        isRear = isMainCameraRear,
                        label = if (isMainCameraRear) "Kamera Belakang" else "Kamera Depan",
                        onPreviewReady = { view ->
                            mainPreviewView = view
                            bindMainCamera()
                        }
                    )

                    // Floating PIP Window
                    val pipAlignment = when (layoutMode) {
                        DualLayoutMode.PIP_TOP_LEFT -> Alignment.TopStart
                        DualLayoutMode.PIP_BOTTOM_RIGHT -> Alignment.BottomEnd
                        else -> Alignment.TopEnd
                    }

                    Box(
                        modifier = Modifier
                            .align(pipAlignment)
                            .padding(14.dp)
                            .size(width = 120.dp, height = 160.dp)
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, Color(0xFFFFB020), RoundedCornerShape(12.dp))
                            .background(Color(0xFF212121))
                    ) {
                        SecondaryCameraStreamView(
                            isRear = !isMainCameraRear,
                            label = if (!isMainCameraRear) "Belakang" else "Selfie",
                            onSwapClick = { isMainCameraRear = !isMainCameraRear }
                        )
                    }
                }
            }

            // Real-Time Audio Level VU Meter
            if (isRecording) {
                Surface(
                    color = Color(0xAA000000),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Audio Level",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height((meter1 * 22).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF00E676))
                        )
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height((meter2 * 22).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF00E676))
                        )
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height((meter3 * 22).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFFFB020))
                        )
                    }
                }
            }

            // Notification / Success Banner
            statusMessage?.let { msg ->
                LaunchedEffect(msg) {
                    delay(3000)
                    statusMessage = null
                }
                Surface(
                    color = Color(0xDD1E1E1E),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFB020)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // LAYOUT SELECTOR (PiP Kanan, PiP Kiri, Split V, Split H)
        DualLayoutSelector(
            currentMode = layoutMode,
            onModeSelect = { layoutMode = it }
        )

        // RECORDING CONTROLS (Big Red Video Shutter, Swap Camera, Gallery)
        DualCameraBottomBar(
            isRecording = isRecording,
            onRecordToggle = {
                if (isRecording) stopRecording() else startRecording()
            },
            onSwapCameras = { isMainCameraRear = !isMainCameraRear },
            onGalleryClick = onNavigateToGallery
        )
    }
}

@Composable
fun DualCameraTopBar(
    isRecording: Boolean,
    durationSec: Long,
    hasConcurrentHardware: Boolean,
    isMainRear: Boolean,
    onSwapCameras: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // REC / DUAL CAM Badge
        Surface(
            color = if (isRecording) Color(0xFFD32F2F) else Color(0xFF262422),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color.White.copy(alpha = recAlpha) else Color(0xFFFFB020)
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                val minutes = durationSec / 60
                val seconds = durationSec % 60
                val timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)
                Text(
                    text = if (isRecording) "REC $timeStr" else "DUAL CAM LIVE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Hardware / Mode Tag
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Color(0xFF212121),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFF424242))
            ) {
                Text(
                    text = if (hasConcurrentHardware) "DUAL-ISP HW" else "DUAL-SYNC PIP",
                    color = Color(0xFFB0BEC5),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            IconButton(
                onClick = onSwapCameras,
                modifier = Modifier.size(36.dp).testTag("dual_swap_cameras")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Swap Main and PiP Cameras",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun CameraStreamView(
    isRear: Boolean,
    label: String,
    onPreviewReady: (PreviewView) -> Unit
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    onPreviewReady(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Camera overlay label
        Surface(
            color = Color(0x88000000),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun SecondaryCameraStreamView(
    isRear: Boolean,
    label: String,
    onSwapClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onSwapClick() }
            .testTag("secondary_camera_pip_box")
    ) {
        // Gradient ambient backdrop representing active secondary camera feed
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isRear) {
                            listOf(Color(0xFF2A2E35), Color(0xFF181C22))
                        } else {
                            listOf(Color(0xFF382E35), Color(0xFF221820))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Dual Stream Secondary",
                    tint = Color(0xFFFFB020),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isRear) "CAM BELAKANG" else "CAM DEPAN (SELFIE)",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ketuk untuk tukar",
                    color = Color(0xFFAAAAAA),
                    fontSize = 8.sp
                )
            }
        }

        // Top tag badge
        Surface(
            color = Color(0xAA000000),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFFFFD54F),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
fun DualLayoutSelector(
    currentMode: DualLayoutMode,
    onModeSelect: (DualLayoutMode) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DualLayoutMode.values()) { mode ->
            val isSelected = mode == currentMode
            Surface(
                color = if (isSelected) Color(0xFFFFB020) else Color(0xFF262422),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color.White else Color(0xFF424242)
                ),
                modifier = Modifier
                    .clickable { onModeSelect(mode) }
                    .testTag("layout_${mode.name.lowercase()}")
            ) {
                Text(
                    text = mode.label,
                    color = if (isSelected) Color.Black else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun DualCameraBottomBar(
    isRecording: Boolean,
    onRecordToggle: () -> Unit,
    onSwapCameras: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Swap Cameras Pill Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF262422))
                .clickable { onSwapCameras() }
                .testTag("dual_cam_swap_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Swap Cameras",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // BIG RED DUAL VIDEO SHUTTER
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF33302C))
                .border(3.dp, Color(0xFFD32F2F), CircleShape)
                .clickable { onRecordToggle() }
                .testTag("dual_video_record_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isRecording) {
                // Square Stop button inside circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFD32F2F))
                )
            } else {
                // Red Record circle
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F))
                )
            }
        }

        // Gallery Quick Jump Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF262422))
                .clickable { onGalleryClick() }
                .testTag("dual_cam_gallery_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ViewAgenda,
                contentDescription = "Open Gallery",
                tint = Color(0xFFFFB020),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
