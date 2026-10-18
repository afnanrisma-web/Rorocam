package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.model.AutoEnhanceMode
import com.example.model.AutoEnhanceSettings
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object AutoEnhanceEngine {

    data class ImageAnalysis(
        val meanLuminance: Float,
        val meanRed: Float,
        val meanGreen: Float,
        val meanBlue: Float,
        val minLum: Float,
        val maxLum: Float,
        val isUnderexposed: Boolean,
        val isOverexposed: Boolean,
        val hasWarmCast: Boolean,
        val hasCoolCast: Boolean
    )

    /**
     * Fast histogram and channel statistical analysis using sub-sampled pixels.
     */
    fun analyzeImage(bitmap: Bitmap): ImageAnalysis {
        val width = bitmap.width
        val height = bitmap.height
        val step = max(1, min(width, height) / 100) // sample ~10,000 points

        var totalLum = 0.0
        var totalR = 0.0
        var totalG = 0.0
        var totalB = 0.0
        var count = 0

        var minL = 255f
        var maxL = 0f

        for (x in 0 until width step step) {
            for (y in 0 until height step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = AndroidColor.red(pixel)
                val g = AndroidColor.green(pixel)
                val b = AndroidColor.blue(pixel)

                // Standard Rec. 601 Luma
                val lum = 0.299f * r + 0.587f * g + 0.114f * b

                totalLum += lum
                totalR += r
                totalG += g
                totalB += b
                count++

                if (lum < minL) minL = lum
                if (lum > maxL) maxL = lum
            }
        }

        val meanLum = if (count > 0) (totalLum / count).toFloat() else 128f
        val meanR = if (count > 0) (totalR / count).toFloat() else 128f
        val meanG = if (count > 0) (totalG / count).toFloat() else 128f
        val meanB = if (count > 0) (totalB / count).toFloat() else 128f

        return ImageAnalysis(
            meanLuminance = meanLum,
            meanRed = meanR,
            meanGreen = meanG,
            meanBlue = meanB,
            minLum = minL,
            maxLum = maxL,
            isUnderexposed = meanLum < 100f,
            isOverexposed = meanLum > 175f,
            hasWarmCast = (meanR - meanB) > 25f,
            hasCoolCast = (meanB - meanR) > 25f
        )
    }

    /**
     * Generates optimal auto-enhancement parameters automatically tailored to the analyzed image.
     */
    fun computeAutoSettings(bitmap: Bitmap, mode: AutoEnhanceMode): AutoEnhanceSettings {
        val analysis = analyzeImage(bitmap)

        // Dynamic exposure compensation
        val targetLum = 135f
        val lumDiff = targetLum - analysis.meanLuminance
        val autoBrightness = (lumDiff * 0.28f).coerceIn(-35f, 35f)

        // Contrast based on dynamic range spread
        val dynamicRange = analysis.maxLum - analysis.minLum
        val autoContrast = when {
            dynamicRange < 140f -> 1.30f // flat/foggy image needs strong punch
            dynamicRange < 190f -> 1.18f
            else -> 1.08f
        }

        // White balance adjustment (Gray-World normalization)
        val rGain = if (analysis.meanRed > 0) analysis.meanGreen / analysis.meanRed else 1.0f
        val bGain = if (analysis.meanBlue > 0) analysis.meanGreen / analysis.meanBlue else 1.0f
        val autoWarmth = ((rGain - bGain) * 0.12f).coerceIn(-0.25f, 0.25f)

        return when (mode) {
            AutoEnhanceMode.MAGIC_AUTO -> {
                AutoEnhanceSettings(
                    intensity = 1.0f,
                    brightness = autoBrightness,
                    contrast = autoContrast,
                    saturation = 1.25f,
                    warmth = autoWarmth,
                    grain = 0.08f,
                    vignette = 0.15f
                )
            }
            AutoEnhanceMode.VINTAGE_WARMTH -> {
                AutoEnhanceSettings(
                    intensity = 1.0f,
                    brightness = autoBrightness + 5f,
                    contrast = autoContrast * 1.05f,
                    saturation = 1.12f,
                    warmth = 0.18f,
                    grain = 0.22f,
                    vignette = 0.28f
                )
            }
            AutoEnhanceMode.VIVID_POP -> {
                AutoEnhanceSettings(
                    intensity = 1.0f,
                    brightness = autoBrightness,
                    contrast = autoContrast * 1.12f,
                    saturation = 1.45f,
                    warmth = 0.05f,
                    grain = 0.04f,
                    vignette = 0.12f
                )
            }
            AutoEnhanceMode.PORTRAIT_PRO -> {
                AutoEnhanceSettings(
                    intensity = 1.0f,
                    brightness = (autoBrightness + 8f).coerceAtMost(30f),
                    contrast = 1.08f,
                    saturation = 1.15f,
                    warmth = 0.10f,
                    grain = 0.05f,
                    vignette = 0.18f
                )
            }
            AutoEnhanceMode.BW_DRAMA -> {
                AutoEnhanceSettings(
                    intensity = 1.0f,
                    brightness = autoBrightness,
                    contrast = autoContrast * 1.35f,
                    saturation = 0.0f, // B&W
                    warmth = 0.0f,
                    grain = 0.25f,
                    vignette = 0.35f
                )
            }
        }
    }

    /**
     * Applies auto-enhancement parameters to the bitmap.
     */
    fun applyEnhance(
        source: Bitmap,
        settings: AutoEnhanceSettings,
        mode: AutoEnhanceMode = AutoEnhanceMode.MAGIC_AUTO
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Interpolate settings by intensity (0 = original, 1 = full auto enhance)
        val k = settings.intensity.coerceIn(0f, 1f)
        val brightness = settings.brightness * k
        val contrast = 1.0f + (settings.contrast - 1.0f) * k
        val saturation = if (settings.saturation == 0f) {
            1.0f - k // gradually desaturates towards 0
        } else {
            1.0f + (settings.saturation - 1.0f) * k
        }
        val warmth = settings.warmth * k

        // ColorMatrix: Brightness & Contrast
        // Contrast formula: c * (x - 128) + 128 + b = c*x + (128*(1-c) + b)
        val t = 128f * (1.0f - contrast) + brightness

        val cmContrast = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, t,
                0f, contrast, 0f, 0f, t,
                0f, 0f, contrast, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )

        // Saturation
        val cmSat = ColorMatrix()
        cmSat.setSaturation(saturation)

        // Warmth (Red up, Blue down)
        val cmWarmth = ColorMatrix(
            floatArrayOf(
                1.0f + warmth * 0.4f, 0f, 0f, 0f, warmth * 20f,
                0f, 1.0f + warmth * 0.15f, 0f, 0f, warmth * 8f,
                0f, 0f, 1.0f - warmth * 0.35f, 0f, -warmth * 15f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        // Combine color matrices
        val finalMatrix = ColorMatrix()
        finalMatrix.postConcat(cmSat)
        finalMatrix.postConcat(cmWarmth)
        finalMatrix.postConcat(cmContrast)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(finalMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Subtle Vignette if configured
        val vignette = settings.vignette * k
        if (vignette > 0.05f) {
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = kotlin.math.hypot(centerX.toDouble(), centerY.toDouble()).toFloat()
            val gradient = RadialGradient(
                centerX,
                centerY,
                radius,
                intArrayOf(
                    AndroidColor.TRANSPARENT,
                    AndroidColor.argb((vignette * 0.35f * 255).toInt(), 0, 0, 0),
                    AndroidColor.argb((vignette * 255).toInt(), 0, 0, 0)
                ),
                floatArrayOf(0.5f, 0.85f, 1.0f),
                Shader.TileMode.CLAMP
            )
            val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = gradient
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vPaint)
        }

        return output
    }
}
