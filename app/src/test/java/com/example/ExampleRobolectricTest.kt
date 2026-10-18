package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.engine.AutoEnhanceEngine
import com.example.engine.RetroFilterEngine
import com.example.model.AutoEnhanceMode
import com.example.model.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RoroCam", appName)
    }

    @Test
    fun `retro filter engine processes bitmap`() {
        val testBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val filtered = RetroFilterEngine.processBitmap(
            source = testBmp,
            filter = FilterType.RETRO_35MM,
            includeDateStamp = true
        )
        assertNotNull(filtered)
        assertEquals(100, filtered.width)
        assertEquals(100, filtered.height)
    }

    @Test
    fun `auto enhance engine analyzes and enhances bitmap`() {
        val testBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val analysis = AutoEnhanceEngine.analyzeImage(testBmp)
        assertNotNull(analysis)

        val settings = AutoEnhanceEngine.computeAutoSettings(testBmp, AutoEnhanceMode.MAGIC_AUTO)
        assertTrue(settings.contrast >= 1.0f)

        val enhanced = AutoEnhanceEngine.applyEnhance(testBmp, settings, AutoEnhanceMode.MAGIC_AUTO)
        assertNotNull(enhanced)
        assertEquals(100, enhanced.width)
        assertEquals(100, enhanced.height)
    }
}
