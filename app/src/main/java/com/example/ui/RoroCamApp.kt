package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repository.MediaRepository

enum class RetroScreen(val title: String) {
    CAMERA("Kamera Retro"),
    DUAL_VIDEO("Dual Video"),
    AUTO_EDIT("Edit Otomatis"),
    GALLERY("Galeri")
}

@Composable
fun RoroCamApp(
    repository: MediaRepository
) {
    var currentScreen by remember { mutableStateOf(RetroScreen.CAMERA) }
    var editTargetPhotoUri by remember { mutableStateOf<String?>(null) }

    // Initialize media repository
    LaunchedEffect(Unit) {
        repository.initialize()
    }

    // Hardware BackHandler: if not on CAMERA, return to CAMERA screen
    if (currentScreen != RetroScreen.CAMERA) {
        BackHandler {
            currentScreen = RetroScreen.CAMERA
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1B1A17),
                contentColor = Color(0xFFFFB020)
            ) {
                // Tab 1: Kamera Retro
                NavigationBarItem(
                    selected = currentScreen == RetroScreen.CAMERA,
                    onClick = { currentScreen = RetroScreen.CAMERA },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Kamera Retro",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Kamera", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFB020),
                        selectedTextColor = Color(0xFFFFB020),
                        unselectedIconColor = Color(0xFF8D8A85),
                        unselectedTextColor = Color(0xFF8D8A85),
                        indicatorColor = Color(0xFF33302B)
                    ),
                    modifier = Modifier.testTag("nav_camera")
                )

                // Tab 2: Dual Kamera Video
                NavigationBarItem(
                    selected = currentScreen == RetroScreen.DUAL_VIDEO,
                    onClick = { currentScreen = RetroScreen.DUAL_VIDEO },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Dual Video",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Dual Video", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF5252),
                        selectedTextColor = Color(0xFFFF5252),
                        unselectedIconColor = Color(0xFF8D8A85),
                        unselectedTextColor = Color(0xFF8D8A85),
                        indicatorColor = Color(0xFF3A2525)
                    ),
                    modifier = Modifier.testTag("nav_dual_video")
                )

                // Tab 3: Edit Foto Otomatis
                NavigationBarItem(
                    selected = currentScreen == RetroScreen.AUTO_EDIT,
                    onClick = { currentScreen = RetroScreen.AUTO_EDIT },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Edit Otomatis",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Edit Otomatis", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFB020),
                        selectedTextColor = Color(0xFFFFB020),
                        unselectedIconColor = Color(0xFF8D8A85),
                        unselectedTextColor = Color(0xFF8D8A85),
                        indicatorColor = Color(0xFF33302B)
                    ),
                    modifier = Modifier.testTag("nav_auto_edit")
                )

                // Tab 4: Galeri
                NavigationBarItem(
                    selected = currentScreen == RetroScreen.GALLERY,
                    onClick = { currentScreen = RetroScreen.GALLERY },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Galeri",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Galeri", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFB020),
                        selectedTextColor = Color(0xFFFFB020),
                        unselectedIconColor = Color(0xFF8D8A85),
                        unselectedTextColor = Color(0xFF8D8A85),
                        indicatorColor = Color(0xFF33302B)
                    ),
                    modifier = Modifier.testTag("nav_gallery")
                )
            }
        },
        containerColor = Color(0xFF141312)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF141312))
        ) {
            when (currentScreen) {
                RetroScreen.CAMERA -> {
                    CameraScreen(
                        repository = repository,
                        onNavigateToGallery = { currentScreen = RetroScreen.GALLERY },
                        onNavigateToAutoEdit = { uri ->
                            editTargetPhotoUri = uri
                            currentScreen = RetroScreen.AUTO_EDIT
                        }
                    )
                }
                RetroScreen.DUAL_VIDEO -> {
                    DualCameraScreen(
                        repository = repository,
                        onNavigateToGallery = { currentScreen = RetroScreen.GALLERY }
                    )
                }
                RetroScreen.AUTO_EDIT -> {
                    AutoEditScreen(
                        initialPhotoUri = editTargetPhotoUri,
                        repository = repository,
                        onNavigateToGallery = { currentScreen = RetroScreen.GALLERY }
                    )
                }
                RetroScreen.GALLERY -> {
                    GalleryScreen(
                        repository = repository,
                        onNavigateToAutoEdit = { uri ->
                            editTargetPhotoUri = uri
                            currentScreen = RetroScreen.AUTO_EDIT
                        }
                    )
                }
            }
        }
    }
}
