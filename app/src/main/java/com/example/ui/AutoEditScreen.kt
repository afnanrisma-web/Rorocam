package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AutoEnhanceEngine
import com.example.model.AutoEnhanceMode
import com.example.model.AutoEnhanceSettings
import com.example.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.roundToInt

@Composable
fun AutoEditScreen(
    initialPhotoUri: String? = null,
    repository: MediaRepository,
    onNavigateToGallery: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var selectedMode by remember { mutableStateOf(AutoEnhanceMode.MAGIC_AUTO) }
    var settings by remember { mutableStateOf(AutoEnhanceSettings()) }
    var analysisSummary by remember { mutableStateOf<String?>(null) }

    // Before/After slider position (0f to 1f, default 0.5f in center)
    var splitPosition by remember { mutableFloatStateOf(0.5f) }
    var showOnlyOriginal by remember { mutableStateOf(false) }
    var showFineTune by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val stream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    withContext(Dispatchers.Main) {
                        originalBitmap = bmp
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Load initial photo if provided or load sample photo
    LaunchedEffect(initialPhotoUri) {
        if (!initialPhotoUri.isNullOrEmpty()) {
            val bmp = repository.loadBitmap(initialPhotoUri)
            if (bmp != null) {
                originalBitmap = bmp
                return@LaunchedEffect
            }
        }

        // Fallback to sample photo from repository or resource
        scope.launch(Dispatchers.IO) {
            val resId = context.resources.getIdentifier("sample_retro_photo", "drawable", context.packageName)
            val bmp = if (resId != 0) {
                BitmapFactory.decodeResource(context.resources, resId)
            } else null

            withContext(Dispatchers.Main) {
                if (originalBitmap == null && bmp != null) {
                    originalBitmap = bmp
                }
            }
        }
    }

    // Run Auto Enhance when bitmap or mode changes
    fun runAutoEnhance(newMode: AutoEnhanceMode = selectedMode) {
        val orig = originalBitmap ?: return
        isProcessing = true

        scope.launch(Dispatchers.Default) {
            val analysis = AutoEnhanceEngine.analyzeImage(orig)
            val computed = AutoEnhanceEngine.computeAutoSettings(orig, newMode)
            val result = AutoEnhanceEngine.applyEnhance(orig, computed, newMode)

            val summary = buildString {
                append("Pencahayaan ")
                if (analysis.isUnderexposed) append("diangkat (+25%)")
                else if (analysis.isOverexposed) append("dikoreksi (-15%)")
                else append("seimbang")
                append(" • ")
                if (analysis.hasWarmCast) append("Warm cast distabilkan")
                else if (analysis.hasCoolCast) append("Suhu dihangatkan")
                else append("Warna dinamis")
            }

            withContext(Dispatchers.Main) {
                settings = computed
                enhancedBitmap = result
                analysisSummary = summary
                isProcessing = false
            }
        }
    }

    // Trigger auto-enhance whenever original bitmap is loaded
    LaunchedEffect(originalBitmap) {
        if (originalBitmap != null) {
            runAutoEnhance(selectedMode)
        }
    }

    // Trigger when mode changes
    fun onModeSelected(mode: AutoEnhanceMode) {
        selectedMode = mode
        runAutoEnhance(mode)
    }

    // Update settings when sliders move
    fun reapplySettings(newSettings: AutoEnhanceSettings) {
        settings = newSettings
        val orig = originalBitmap ?: return
        scope.launch(Dispatchers.Default) {
            val result = AutoEnhanceEngine.applyEnhance(orig, newSettings, selectedMode)
            withContext(Dispatchers.Main) {
                enhancedBitmap = result
            }
        }
    }

    // Save enhanced photo
    fun saveEnhanced() {
        val bmp = enhancedBitmap ?: originalBitmap ?: return
        scope.launch(Dispatchers.IO) {
            val saved = repository.savePhoto(bmp, "Auto_${selectedMode.name}")
            withContext(Dispatchers.Main) {
                saveSuccessMessage = "Foto berhasil disimpan ke Galeri!"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141312))
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFB020),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Edit Foto Otomatis",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smart Auto-Enhancement",
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Import from device photo picker
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.size(38.dp).testTag("import_photo_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "Pilih Foto",
                        tint = Color.White
                    )
                }

                // Fine Tune Toggle
                IconButton(
                    onClick = { showFineTune = !showFineTune },
                    modifier = Modifier.size(38.dp).testTag("fine_tune_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Fine Tune",
                        tint = if (showFineTune) Color(0xFFFFB020) else Color.White
                    )
                }

                // Save button
                IconButton(
                    onClick = { saveEnhanced() },
                    modifier = Modifier.size(38.dp).testTag("save_enhanced_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Simpan Foto",
                        tint = Color(0xFFFFB020)
                    )
                }
            }
        }

        // MAIN BEFORE / AFTER INTERACTIVE VIEWER
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (originalBitmap != null) {
                val orig = originalBitmap!!
                val enh = enhancedBitmap ?: orig

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val boxWidth = maxWidth
                    val boxHeight = maxHeight
                    val splitPx = (boxWidth.value * splitPosition).coerceIn(0f, boxWidth.value)

                    // Layer 1: Enhanced Image (Base)
                    Image(
                        bitmap = (if (showOnlyOriginal) orig else enh).asImageBitmap(),
                        contentDescription = "Enhanced Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Layer 2: Original Image clipped to left side of split handle
                    if (!showOnlyOriginal && enhancedBitmap != null) {
                        Image(
                            bitmap = orig.asImageBitmap(),
                            contentDescription = "Original Photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithContent {
                                    clipRect(right = size.width * splitPosition) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                        )

                        // Split divider line and draggable handle
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .offset { IntOffset((boxWidth.toPx() * splitPosition).roundToInt(), 0) }
                                .background(Color.White)
                        )

                        // Draggable Center Pill Handle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .offset {
                                    IntOffset(
                                        (boxWidth.toPx() * splitPosition - 18.dp.toPx()).roundToInt(),
                                        (boxHeight.toPx() / 2f - 18.dp.toPx()).roundToInt()
                                    )
                                }
                                .clip(CircleShape)
                                .background(Color(0xFFFFB020))
                                .border(2.dp, Color.White, CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newPos = splitPosition + dragAmount.x / boxWidth.toPx()
                                        splitPosition = newPos.coerceIn(0.05f, 0.95f)
                                    }
                                }
                                .testTag("before_after_slider_handle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Bandingkan",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Tags: "SEBELUM" (Left) and "SESUDAH" (Right)
                        Surface(
                            color = Color(0x99000000),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "ORIGINAL",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = Color(0x99FFB020),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "AUTO ENHANCED",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else {
                CircularProgressIndicator(color = Color(0xFFFFB020))
            }

            // Processing indicator
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFFB020))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Menganalisis & Mengoptimalkan...",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Save Toast Banner
            saveSuccessMessage?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2600)
                    saveSuccessMessage = null
                }
                Surface(
                    color = Color(0xEE2E7D32),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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

        // ANALYSIS SMART SUMMARY PILL
        analysisSummary?.let { summary ->
            Surface(
                color = Color(0xFF21201D),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF383531)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFB020),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = summary,
                        color = Color(0xFFE0E0E0),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // AUTO ENHANCE PRESET MODES
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AutoEnhanceMode.values()) { mode ->
                val isSelected = mode == selectedMode
                Surface(
                    color = if (isSelected) Color(0xFFFFB020) else Color(0xFF262422),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color.White else Color(0xFF424242)
                    ),
                    modifier = Modifier
                        .clickable { onModeSelected(mode) }
                        .testTag("mode_${mode.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = mode.title,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = mode.subtitle,
                            color = if (isSelected) Color(0xFF212121) else Color(0xFF9E9E9E),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // FINE TUNE SLIDERS DRAWER (Expandable)
        AnimatedVisibility(visible = showFineTune) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Intensity
                Text(
                    text = "Intensitas Auto: ${(settings.intensity * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 11.sp
                )
                Slider(
                    value = settings.intensity,
                    onValueChange = { reapplySettings(settings.copy(intensity = it)) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFB020),
                        activeTrackColor = Color(0xFFFFB020)
                    )
                )

                // Warmth
                Text(
                    text = "Kehangatan (Warmth): ${(settings.warmth * 100).toInt()}",
                    color = Color.White,
                    fontSize = 11.sp
                )
                Slider(
                    value = settings.warmth,
                    onValueChange = { reapplySettings(settings.copy(warmth = it)) },
                    valueRange = -0.3f..0.3f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFB020),
                        activeTrackColor = Color(0xFFFFB020)
                    )
                )
            }
        }

        // BOTTOM ACTION: ONE-TAP MAGIC FIX BUTTON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { runAutoEnhance(selectedMode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB020),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("one_tap_magic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Optimasi Otomatis Seketika",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { saveEnhanced() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Simpan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
