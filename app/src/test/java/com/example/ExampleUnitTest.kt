package com.example

import com.example.model.CurveMath
import com.example.model.CurvePoint
import com.example.model.EditState
import com.example.model.ExportFormat
import com.example.model.ExportSettings
import com.example.model.GrainPreset
import com.example.model.GrainState
import com.example.model.NoirPresetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun defaultEditState_isNotModified() {
        val state = EditState.DEFAULT
        assertFalse(state.isModified())
        assertEquals(NoirPresetId.PURE, state.presetId)
        assertEquals(0f, state.exposure, 0.001f)
        assertEquals(0, state.contrast)
    }

    @Test
    fun allNoirPresets_generateValidParameters() {
        NoirPresetId.values().forEach { preset ->
            val state = EditState.fromPreset(preset)
            assertEquals(preset, state.presetId)
        }
    }

    @Test
    fun deepNoirPreset_hasStrongContrastAndBlacks() {
        val state = EditState.fromPreset(NoirPresetId.DEEP_NOIR)
        assertTrue(state.contrast > 0)
        assertTrue(state.blacks < 0)
    }

    @Test
    fun curveMathLut_computesMonotonicLookupTable() {
        val points = listOf(
            CurvePoint(0.0f, 0.0f),
            CurvePoint(0.5f, 0.5f),
            CurvePoint(1.0f, 1.0f)
        )
        val lut = CurveMath.computeLut(points)
        assertEquals(256, lut.size)
        assertEquals(0, lut[0])
        assertEquals(255, lut[255])
        // Midpoint should be around 128
        assertTrue(lut[128] in 125..131)
    }

    @Test
    fun curveMathLut_handlesUserCustomControlPoints() {
        val points = listOf(
            CurvePoint(0.00f, 0.00f),
            CurvePoint(0.25f, 0.15f),
            CurvePoint(0.50f, 0.55f),
            CurvePoint(0.75f, 0.85f),
            CurvePoint(1.00f, 1.00f)
        )
        val lut = CurveMath.computeLut(points)
        assertEquals(256, lut.size)
        assertEquals(0, lut[0])
        assertEquals(255, lut[255])
        // Shadows darkened: input 64 (0.25) -> approx 38 (0.15)
        assertTrue(lut[64] in 36..42)
        // Midtones brightened: input 128 (0.50) -> approx 140 (0.55)
        assertTrue(lut[128] in 137..144)
        // Highlights brightened: input 191 (0.75) -> approx 217 (0.85)
        assertTrue(lut[191] in 214..220)
    }

    @Test
    fun grainPresets_configureAppropriateAmounts() {
        val fine = GrainState.fromPreset(GrainPreset.FINE)
        val heavy = GrainState.fromPreset(GrainPreset.HEAVY)
        assertTrue(heavy.amount > fine.amount)
    }

    @Test
    fun exportSettings_estimatesReasonableSizes() {
        val settings = ExportSettings(
            format = ExportFormat.PNG,
            quality = 100,
            targetWidth = 4000,
            targetHeight = 3000
        )
        val estimate = settings.estimatedSizeFormatted()
        assertTrue(estimate.contains("MB"))
    }
}
