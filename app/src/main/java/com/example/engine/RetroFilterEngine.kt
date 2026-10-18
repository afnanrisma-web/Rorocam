package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.ColorFilter
import com.example.model.FilterType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.min

object RetroFilterEngine {

    fun getColorMatrix(filter: FilterType): ColorMatrix {
        val matrix = ColorMatrix()
        when (filter) {
            FilterType.ORIGINAL -> {
                // Identity
            }
            FilterType.RETRO_35MM -> {
                // Warm film, slight green-amber tint, lifted blacks
                matrix.set(
                    floatArrayOf(
                        1.15f, 0.05f, 0.0f, 0f, 18f,
                        0.05f, 1.05f, 0.0f, 0f, 12f,
                        0.0f, 0.05f, 0.85f, 0f, 5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            FilterType.KODAK_GOLD -> {
                // Rich golden amber, punchy saturation, warm highlights
                matrix.set(
                    floatArrayOf(
                        1.25f, 0.1f, -0.05f, 0f, 15f,
                        0.05f, 1.15f, 0.0f, 0f, 10f,
                        -0.1f, 0.0f, 0.8f, 0f, -5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            FilterType.NOIR_BW -> {
                // Punchy high-contrast monochrome
                val gray = ColorMatrix()
                gray.setSaturation(0f)
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.35f, 0f, 0f, 0f, -25f,
                        0f, 1.35f, 0f, 0f, -25f,
                        0f, 0f, 1.35f, 0f, -25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                contrast.preConcat(gray)
                matrix.set(contrast)
            }
            FilterType.SEPIA_VINTAGE -> {
                // Warm antique sepia
                matrix.set(
                    floatArrayOf(
                        0.393f * 1.1f, 0.769f * 1.1f, 0.189f * 1.1f, 0f, 25f,
                        0.349f * 1.05f, 0.686f * 1.05f, 0.168f * 1.05f, 0f, 15f,
                        0.272f * 0.9f, 0.534f * 0.9f, 0.131f * 0.9f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            FilterType.INSTANT_70S -> {
                // Polaroid faded shadows, pastel highlights, slight cyan shift in shadows
                matrix.set(
                    floatArrayOf(
                        1.05f, 0.05f, 0.0f, 0f, 22f,
                        0.0f, 1.1f, 0.05f, 0f, 20f,
                        0.05f, 0.0f, 1.15f, 0f, 26f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            FilterType.CYBERPUNK -> {
                // Cyan and magenta neon split
                matrix.set(
                    floatArrayOf(
                        1.3f, -0.1f, 0.2f, 0f, 30f,
                        -0.1f, 1.0f, 0.3f, 0f, 10f,
                        0.3f, 0.1f, 1.4f, 0f, 40f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            FilterType.VHS_GLITCH -> {
                // Slightly desaturated with chromatic red/cyan shift
                matrix.set(
                    floatArrayOf(
                        1.15f, 0.0f, 0.1f, 0f, 10f,
                        0.05f, 1.0f, 0.0f, 0f, 5f,
                        0.0f, 0.15f, 1.2f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
        }
        return matrix
    }

    fun getComposeColorFilter(filter: FilterType): ColorFilter? {
        if (filter == FilterType.ORIGINAL) return null
        val cm = getColorMatrix(filter)
        return ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(cm.array))
    }

    /**
     * Applies complete retro post-processing to a captured bitmap:
     * - Color grading matrix
     * - Vignette shading
     * - Procedural film grain
     * - Vintage date stamp (amber digital LED font)
     */
    fun processBitmap(
        source: Bitmap,
        filter: FilterType,
        includeDateStamp: Boolean = true,
        customDateText: String? = null
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Draw source with ColorFilter
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (filter != FilterType.ORIGINAL) {
            paint.colorFilter = ColorMatrixColorFilter(getColorMatrix(filter))
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // 2. Apply Vignette
        if (filter != FilterType.ORIGINAL) {
            drawVignette(canvas, width, height, if (filter == FilterType.SEPIA_VINTAGE) 0.45f else 0.3f)
        }

        // 3. Apply Film Grain
        if (filter != FilterType.ORIGINAL) {
            val grainIntensity = when (filter) {
                FilterType.RETRO_35MM -> 0.22f
                FilterType.NOIR_BW -> 0.35f
                FilterType.SEPIA_VINTAGE -> 0.28f
                FilterType.VHS_GLITCH -> 0.30f
                else -> 0.16f
            }
            drawFilmGrain(canvas, width, height, grainIntensity)
        }

        // 4. VHS Scanlines
        if (filter == FilterType.VHS_GLITCH) {
            drawVhsScanlines(canvas, width, height)
        }

        // 5. Date Stamp (amber orange digital stamp in lower right corner)
        if (includeDateStamp) {
            val dateStr = customDateText ?: run {
                val sdf = SimpleDateFormat("''98 MM dd", Locale.US)
                sdf.format(Date())
            }
            drawVintageDateStamp(canvas, width, height, dateStr)
        }

        return output
    }

    private fun drawVignette(canvas: Canvas, width: Int, height: Int, intensity: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = kotlin.math.hypot(centerX.toDouble(), centerY.toDouble()).toFloat()

        val gradient = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(
                AndroidColor.TRANSPARENT,
                AndroidColor.argb((intensity * 0.4f * 255).toInt(), 0, 0, 0),
                AndroidColor.argb((intensity * 255).toInt(), 0, 0, 0)
            ),
            floatArrayOf(0.4f, 0.8f, 1.0f),
            Shader.TileMode.CLAMP
        )

        val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vPaint)
    }

    private fun drawFilmGrain(canvas: Canvas, width: Int, height: Int, intensity: Float) {
        val grainPaint = Paint()
        val random = Random(42)
        val step = 3 // Sample step for performance
        val alphaBase = (intensity * 40).toInt().coerceIn(4, 70)

        for (x in 0 until width step step) {
            for (y in 0 until height step step) {
                if (random.nextFloat() > 0.45f) {
                    val noise = random.nextInt(256)
                    val alpha = random.nextInt(alphaBase)
                    grainPaint.color = AndroidColor.argb(alpha, noise, noise, noise)
                    canvas.drawPoint(x.toFloat(), y.toFloat(), grainPaint)
                }
            }
        }
    }

    private fun drawVhsScanlines(canvas: Canvas, width: Int, height: Int) {
        val linePaint = Paint().apply {
            color = AndroidColor.argb(30, 0, 0, 0)
            strokeWidth = 2f
        }
        for (y in 0 until height step 8) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), linePaint)
        }
    }

    private fun drawVintageDateStamp(canvas: Canvas, width: Int, height: Int, text: String) {
        val textSize = (min(width, height) * 0.045f).coerceAtLeast(32f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(255, 140, 0) // Amber LED orange
            this.textSize = textSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(6f, 2f, 2f, AndroidColor.argb(180, 80, 20, 0))
        }

        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)

        val margin = min(width, height) * 0.05f
        val x = width - bounds.width() - margin
        val y = height - margin

        canvas.drawText(text, x, y, textPaint)
    }
}
