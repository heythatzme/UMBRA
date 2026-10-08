package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.EditState
import com.example.model.NoirPresetId
import com.example.processing.NoirProcessor
import com.example.utils.ImageUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals("UMBRA", appName)
    }

    @Test
    fun `sample photo creates and renders noir edit`() = runBlocking {
        val sample = ImageUtils.createSamplePhoto(200, 200)
        assertNotNull(sample)
        assertEquals(200, sample.width)
        assertEquals(200, sample.height)

        val rendered = NoirProcessor.render(sample, EditState.fromPreset(NoirPresetId.DEEP_NOIR))
        assertNotNull(rendered)
        assertEquals(200, rendered.width)
        assertEquals(200, rendered.height)
    }
}
