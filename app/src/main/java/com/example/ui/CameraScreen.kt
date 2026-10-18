package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.camera.CameraHelper
import com.example.engine.RetroFilterEngine
import com.example.model.FilterType
import com.example.model.MediaItem
import com.example.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    repository: MediaRepository,
    onNavigateToGallery: () -> Unit,
    onNavigateToAutoEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember { mutableStateOf(CameraHelper.hasCameraPermission(context)) }
    var selectedFilter by remember { mutableStateOf(FilterType.RETRO_35MM) }
    var isBackCamera by remember { mutableStateOf(true) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var showGrid by remember { mutableStateOf(true) }
    var includeDateStamp by remember { mutableStateOf(true) }

    var isCapturing by remember { mutableStateOf(false) }
    var shutterFlash by remember { mutableStateOf(false) }
    var recentPhoto by remember { mutableStateOf<MediaItem?>(null) }
    var captureSuccessMessage by remember { mutableStateOf<String?>(null) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var cameraAvailable by remember { mutableStateOf(true) }

    // Date stamp text
    val dateStampText = remember {
        val sdf = SimpleDateFormat("''98 MM dd", Locale.US)
        sdf.format(Date())
    }

    // Load recent photo
    LaunchedEffect(Unit) {
        repository.mediaList.collect { list ->
            recentPhoto = list.firstOrNull { !it.isVideo }
        }
    }

    // Bind CameraX
    fun bindCamera() {
        val provider = cameraProvider ?: return
        val pView = previewView ?: return

        try {
            provider.unbindAll()

            val cameraSelector = if (isBackCamera) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(pView.surfaceProvider)
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
                .build()

            imageCapture = capture

            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
            )
            cameraAvailable = true
        } catch (e: Exception) {
            e.printStackTrace()
            cameraAvailable = false
        }
    }

    LaunchedEffect(isBackCamera, flashMode) {
        bindCamera()
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

    // Shutter capture action
    fun takePhoto() {
        if (isCapturing) return
        isCapturing = true
        shutterFlash = true

        val capture = imageCapture
        val executor = ContextCompat.getMainExecutor(context)

        if (capture != null && cameraAvailable) {
            capture.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        scope.launch(Dispatchers.Default) {
                            val bitmap = imageProxyToBitmap(image, isBackCamera)
                            image.close()

                            val processed = RetroFilterEngine.processBitmap(
                                source = bitmap,
                                filter = selectedFilter,
                                includeDateStamp = includeDateStamp,
                                customDateText = dateStampText
                            )

                            val saved = repository.savePhoto(processed, selectedFilter.displayName)
                            withContext(Dispatchers.Main) {
                                isCapturing = false
                                shutterFlash = false
                                captureSuccessMessage = "Tersimpan: ${saved.filterUsed}"
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isCapturing = false
                        shutterFlash = false
                        captureSuccessMessage = "Gagal mengambil foto: ${exception.message}"
                    }
                }
            )
        } else {
            // Emulated capture fallback (e.g. on emulator without physical camera feed)
            scope.launch(Dispatchers.Default) {
                val sampleRes = context.resources.getIdentifier("sample_retro_photo", "drawable", context.packageName)
                val baseBitmap = if (sampleRes != 0) {
                    BitmapFactory.decodeResource(context.resources, sampleRes)
                } else {
                    Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
                }
                val processed = RetroFilterEngine.processBitmap(
                    source = baseBitmap,
                    filter = selectedFilter,
                    includeDateStamp = includeDateStamp,
                    customDateText = dateStampText
                )
                val saved = repository.savePhoto(processed, selectedFilter.displayName)
                withContext(Dispatchers.Main) {
                    isCapturing = false
                    shutterFlash = false
                    captureSuccessMessage = "Foto Retro Tersimpan: ${saved.filterUsed}"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121110))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // TOP BAR: Camera info & Quick Toggles
            TopCameraBar(
                flashMode = flashMode,
                onFlashToggle = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                },
                showGrid = showGrid,
                onGridToggle = { showGrid = !showGrid },
                includeDateStamp = includeDateStamp,
                onDateStampToggle = { includeDateStamp = !includeDateStamp },
                selectedFilter = selectedFilter
            )

            // VIEWFINDER BOX (3:4 Vintage Aspect Ratio)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                // CameraX Preview View
                AndroidView(
                    factory = { ctx ->
                        val view = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                        previewView = view

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            cameraProvider = cameraProviderFuture.get()
                            bindCamera()
                        }, ContextCompat.getMainExecutor(ctx))

                        view
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Vintage Film Overlay Filter
                RetroFilterOverlay(
                    filter = selectedFilter,
                    showGrid = showGrid,
                    includeDateStamp = includeDateStamp,
                    dateStampText = dateStampText
                )

                // Vintage Viewfinder HUD (ISO, Aperture, Shutter Speed)
                RetroViewfinderHud(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    filter = selectedFilter
                )

                // Shutter Flash Animation
                if (shutterFlash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }

                // Temporary Success Toast Banner
                captureSuccessMessage?.let { msg ->
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(2200)
                        captureSuccessMessage = null
                    }
                    Surface(
                        color = Color(0xDD212121),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = msg,
                            color = Color(0xFFFFB020),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // FILTER SELECTOR CAROUSEL (Film Roll Style)
            FilterCarousel(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            // BOTTOM CONTROL BAR (Recent thumbnail, Big Shutter, Flip Camera)
            BottomCameraControls(
                isCapturing = isCapturing,
                recentPhoto = recentPhoto,
                onShutterClick = { takePhoto() },
                onFlipCamera = { isBackCamera = !isBackCamera },
                onThumbnailClick = {
                    recentPhoto?.let { photo ->
                        onNavigateToAutoEdit(photo.uri)
                    } ?: onNavigateToGallery()
                }
            )
        }
    }
}

@Composable
fun TopCameraBar(
    flashMode: Int,
    onFlashToggle: () -> Unit,
    showGrid: Boolean,
    onGridToggle: () -> Unit,
    includeDateStamp: Boolean,
    onDateStampToggle: () -> Unit,
    selectedFilter: FilterType
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Film Roll Name Badge
        Surface(
            color = Color(0xFF262422),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, selectedFilter.accentColor.copy(alpha = 0.6f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(selectedFilter.accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedFilter.yearTag,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date stamp toggle
            IconButton(
                onClick = onDateStampToggle,
                modifier = Modifier.size(40.dp).testTag("date_stamp_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Toggle Vintage Date Stamp",
                    tint = if (includeDateStamp) Color(0xFFFFB020) else Color(0xFF757575)
                )
            }

            // Grid toggle
            IconButton(
                onClick = onGridToggle,
                modifier = Modifier.size(40.dp).testTag("grid_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Toggle Grid",
                    tint = if (showGrid) Color(0xFFFFB020) else Color(0xFF757575)
                )
            }

            // Flash toggle
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier.size(40.dp).testTag("flash_toggle")
            ) {
                val (icon, tint) = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn to Color(0xFFFFD54F)
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto to Color(0xFFFFD54F)
                    else -> Icons.Default.FlashOff to Color(0xFF757575)
                }
                Icon(imageVector = icon, contentDescription = "Flash Mode", tint = tint)
            }
        }
    }
}

@Composable
fun RetroFilterOverlay(
    filter: FilterType,
    showGrid: Boolean,
    includeDateStamp: Boolean,
    dateStampText: String
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Grid
        if (showGrid) {
            val gridColor = Color(0x33FFFFFF)
            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = 1.dp.toPx())
        }

        // 2. Filter Color Tints
        when (filter) {
            FilterType.ORIGINAL -> {}
            FilterType.RETRO_35MM -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x18FFE082), Color(0x33422800), Color(0x66000000)),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.85f
                    ),
                    blendMode = BlendMode.Multiply
                )
            }
            FilterType.KODAK_GOLD -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x22FFB300), Color(0x28FF6F00), Color(0x55000000)),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.85f
                    ),
                    blendMode = BlendMode.Multiply
                )
            }
            FilterType.NOIR_BW -> {
                drawRect(
                    color = Color(0x20000000),
                    blendMode = BlendMode.Color
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x88000000)),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.75f
                    )
                )
            }
            FilterType.SEPIA_VINTAGE -> {
                drawRect(
                    color = Color(0x388D6E63),
                    blendMode = BlendMode.Color
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x125D4037), Color(0x773E2723)),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.85f
                    )
                )
            }
            FilterType.INSTANT_70S -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x1580DEEA), Color(0x15FFE082), Color(0x33004D40))
                    ),
                    blendMode = BlendMode.Overlay
                )
            }
            FilterType.CYBERPUNK -> {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0x3000E5FF), Color(0x30D500F9))
                    ),
                    blendMode = BlendMode.ColorDodge
                )
            }
            FilterType.VHS_GLITCH -> {
                // VHS scanlines
                for (y in 0 until h.toInt() step 12) {
                    drawLine(
                        color = Color(0x26000000),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(w, y.toFloat()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }

    // Vintage Date Stamp Overlay (Orange LED digital clock style)
    if (includeDateStamp) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = dateStampText,
                color = Color(0xFFFF9100),
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF3E1A00),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

@Composable
fun RetroViewfinderHud(
    modifier: Modifier = Modifier,
    filter: FilterType
) {
    Surface(
        color = Color(0x88000000),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RoroCam  1/125  f/2.8  ISO 400",
                color = Color(0xFFECEFF1),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
            )
        }
    }
}

@Composable
fun FilterCarousel(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(FilterType.values()) { filter ->
                val isSelected = filter == selectedFilter
                Surface(
                    color = if (isSelected) filter.accentColor else Color(0xFF262422),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color.White else Color(0xFF424242)
                    ),
                    modifier = Modifier
                        .clickable { onFilterSelected(filter) }
                        .testTag("filter_${filter.name.lowercase()}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter.yearTag,
                            color = if (isSelected) Color.Black else filter.accentColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = filter.displayName,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomCameraControls(
    isCapturing: Boolean,
    recentPhoto: MediaItem?,
    onShutterClick: () -> Unit,
    onFlipCamera: () -> Unit,
    onThumbnailClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Recent thumbnail button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF262422))
                .border(2.dp, Color(0xFF424242), RoundedCornerShape(12.dp))
                .clickable { onThumbnailClick() }
                .testTag("recent_thumbnail_button"),
            contentAlignment = Alignment.Center
        ) {
            if (recentPhoto != null) {
                AsyncImage(
                    model = recentPhoto.uri,
                    contentDescription = "Recent Photo",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Shutter Button (Vintage two-tone analog shutter)
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF33302C))
                .border(3.dp, Color(0xFFFFB020), CircleShape)
                .clickable(enabled = !isCapturing) { onShutterClick() }
                .testTag("camera_shutter_button"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isCapturing) Color(0xFFFF5252) else Color(0xFFEEEEEE))
            )
        }

        // Flip Camera Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF262422))
                .clickable { onFlipCamera() }
                .testTag("flip_camera_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Flip Camera",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Converts ImageProxy to oriented Bitmap.
 */
private fun imageProxyToBitmap(image: ImageProxy, isBackCamera: Boolean): Bitmap {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val rotation = image.imageInfo.rotationDegrees
    val matrix = Matrix()
    matrix.postRotate(rotation.toFloat())
    if (!isBackCamera) {
        matrix.postScale(-1f, 1f) // Mirror front camera
    }

    return Bitmap.createBitmap(
        original, 0, 0, original.width, original.height, matrix, true
    )
}
