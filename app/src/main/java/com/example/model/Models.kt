package com.example.model

import android.graphics.ColorMatrix
import androidx.compose.ui.graphics.Color

enum class FilterType(
    val displayName: String,
    val yearTag: String,
    val accentColor: Color,
    val description: String
) {
    ORIGINAL("Normal", "STD", Color(0xFFB0BEC5), "Tampilan asli tanpa filter"),
    RETRO_35MM("35mm Film", "\'98 FILM", Color(0xFFFFB020), "Warna film 35mm hangat dengan butiran analog"),
    KODAK_GOLD("Kodak Gold", "\'84 GOLD", Color(0xFFFF9100), "Saturasi emas hangat khas era 80-an"),
    NOIR_BW("Noir B&W", "NOIR", Color(0xFFE0E0E0), "Monokrom kontras tinggi dengan grain perak"),
    SEPIA_VINTAGE("Sepia 1920", "\'20 SEPIA", Color(0xFFD7CCC8), "Nuansa cokelat antik dengan vignette lembut"),
    INSTANT_70S("Instant 70s", "\'76 POLA", Color(0xFF80CBC4), "Highlight pastel dan bayangan pudar khas instan"),
    CYBERPUNK("Cyberpunk", "CYBER", Color(0xFF00E5FF), "Kontras tinggi neon cyan dan magenta futuristik"),
    VHS_GLITCH("VHS Tape", "\'92 VHS", Color(0xFFFF5252), "Efek rekaman pita video dengan garis scanline")
}

enum class AutoEnhanceMode(
    val title: String,
    val subtitle: String
) {
    MAGIC_AUTO("Auto Magic", "Koreksi eksposur, kontras & warna otomatis cerdas"),
    VINTAGE_WARMTH("Auto Vintage", "Harmonisasi warna film hangat dan lembut"),
    VIVID_POP("Auto Vivid", "Tingkatkan vibransi langit, daun & subjek"),
    PORTRAIT_PRO("Auto Portrait", "Pencahayaan subjek optimal & skin tone alami"),
    BW_DRAMA("Auto B&W", "Konversi monokrom dinamis dengan kontras dramatis")
}

enum class DualLayoutMode(
    val label: String
) {
    PIP_TOP_RIGHT("PiP Kanan"),
    PIP_TOP_LEFT("PiP Kiri"),
    PIP_BOTTOM_RIGHT("PiP Bawah"),
    SPLIT_VERTICAL("Split Atas-Bawah"),
    SPLIT_HORIZONTAL("Split Kiri-Kanan")
}

data class MediaItem(
    val id: String,
    val uri: String,
    val name: String,
    val isVideo: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val filterUsed: String = "Normal",
    val durationSec: Int = 0,
    val width: Int = 0,
    val height: Int = 0
)

data class AutoEnhanceSettings(
    val intensity: Float = 1.0f,
    val brightness: Float = 0f, // -50 to +50
    val contrast: Float = 1.15f, // 0.5 to 2.0
    val saturation: Float = 1.2f, // 0.0 to 2.0
    val warmth: Float = 0.1f, // -0.5 to +0.5
    val grain: Float = 0.15f, // 0 to 1
    val vignette: Float = 0.2f // 0 to 1
)
