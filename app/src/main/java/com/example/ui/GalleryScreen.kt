package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.repository.MediaRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    repository: MediaRepository,
    onNavigateToAutoEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaList by repository.mediaList.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Semua, 1: Foto Retro, 2: Dual Video
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }

    // Intercept hardware back button when viewing an item in fullscreen
    if (selectedItem != null) {
        BackHandler {
            selectedItem = null
        }
    }

    val filteredList = remember(mediaList, selectedTab) {
        when (selectedTab) {
            1 -> mediaList.filter { !it.isVideo }
            2 -> mediaList.filter { it.isVideo }
            else -> mediaList
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Galeri RoroCam",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "AF9 Media",
                            fontSize = 11.sp,
                            color = Color(0xFFFFB020),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF141312),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF141312)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1D1A),
                contentColor = Color(0xFFFFB020),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFFFFB020)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Semua (${mediaList.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Foto Retro (${mediaList.count { !it.isVideo }})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Dual Video (${mediaList.count { it.isVideo }})", fontSize = 12.sp) }
                )
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Media",
                            color = Color(0xFFE0E0E0),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ambil foto dengan filter retro atau rekam video dual kamera untuk memulai.",
                            color = Color(0xFF9E9E9E),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        MediaGridTile(
                            item = item,
                            onClick = { selectedItem = item }
                        )
                    }
                }
            }
        }

        // FULLSCREEN MEDIA VIEWER MODAL
        selectedItem?.let { item ->
            FullscreenMediaDialog(
                item = item,
                onDismiss = { selectedItem = null },
                onAutoEdit = {
                    selectedItem = null
                    onNavigateToAutoEdit(item.uri)
                },
                onDelete = {
                    scope.launch {
                        repository.deleteMedia(item)
                        selectedItem = null
                    }
                }
            )
        }
    }
}

@Composable
fun MediaGridTile(
    item: MediaItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF262422)),
        border = BorderStroke(1.dp, Color(0xFF383531)),
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .testTag("media_tile_${item.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.isVideo) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Video thumbnail fallback placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E2129)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Dual Video",
                        tint = Color(0xFFFFB020),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Tag Badge (Video or Filter name)
            Surface(
                color = Color(0xAA000000),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    if (item.isVideo) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Text(
                        text = if (item.isVideo) "DUAL" else item.filterUsed,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FullscreenMediaDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
    onAutoEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xF00F0E0D),
        modifier = Modifier
            .fillMaxSize()
            .testTag("fullscreen_media_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: Back & Filename
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (item.isVideo) "Video Dual Kamera" else "Foto Retro",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val dateFormatted = remember(item.timestamp) {
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(item.timestamp))
                    }
                    Text(
                        text = dateFormatted,
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color(0xFFFF5252)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Media Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (!item.isVideo) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Video playback canvas
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF262422))
                                .clickable { isPlaying = !isPlaying },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Videocam else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color(0xFFFFB020),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isPlaying) "Memutar Video Dual Kamera..." else "Ketuk untuk Memutar Video",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Format: MP4 • Dual Mode PiP/Split",
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions: Edit Otomatis (for photos) or Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!item.isVideo) {
                    Button(
                        onClick = onAutoEdit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB020),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("gallery_to_auto_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Foto Otomatis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (item.isVideo) "video/*" else "image/*"
                            putExtra(Intent.EXTRA_TEXT, "Dibuat dengan RoroCam oleh AF9 Media!")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF262422),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .then(if (item.isVideo) Modifier.weight(1f) else Modifier)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bagikan", fontSize = 13.sp)
                }
            }
        }
    }
}
