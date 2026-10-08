package com.example.processing

import android.graphics.Bitmap
import com.example.model.BwMixState
import com.example.model.CurveMath
import com.example.model.EditState
import com.example.model.NoirPresetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object NoirProcessor {

    /**
     * Applies the complete non-destructive NOIR processing pipeline to [sourceBitmap].
     * Returns a new rendered [Bitmap]. The original [sourceBitmap] is never modified.
     */
    suspend fun render(
        sourceBitmap: Bitmap,
        editState: EditState
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        // If completely unmodified and preset is ORIGINAL, return direct copy
        if (!editState.isModified() && editState.presetId == NoirPresetId.ORIGINAL) {
            return@withContext sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val pixels = IntArray(width * height)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Precompute Look-Up Tables and factors for optimal speed
        val curveLut = CurveMath.computeLut(editState.curves.points)

        // Exposure factor: 2^(exposure)
        val exposureFactor = 2.0f.pow(editState.exposure)

        // Contrast factor
        val contrastFactor = if (editState.contrast != 0) {
            val c = editState.contrast.toFloat()
            (259.0f * (c + 255.0f)) / (255.0f * (259.0f - c))
        } else 1.0f

        val brightnessOffset = editState.brightness * 1.5f
        val highlightsFactor = editState.highlights / 100.0f
        val shadowsFactor = editState.shadows / 100.0f
        val whitesOffset = editState.whites * 1.2f
        val blacksOffset = editState.blacks * 1.2f
        val fadeOffset = editState.fade * 0.45f
        val noirIntensity = editState.noirIntensity / 100.0f

        val isColdNoir = editState.presetId == NoirPresetId.COLD_NOIR
        val isVintageBw = editState.presetId == NoirPresetId.VINTAGE_BW

        val bwMix = editState.bwMix
        val isMixNeutral = bwMix.isNeutral()

        // Vignette precalculations
        val hasVignette = editState.vignetteAmount > 0
        val vAmount = editState.vignetteAmount / 100.0f
        val cx = width / 2.0f
        val cy = height / 2.0f
        val maxDist = sqrt(cx * cx + cy * cy)
        val vMidpoint = (editState.vignetteMidpoint / 100.0f) * maxDist * 0.9f
        val vFeather = max(1.0f, (editState.vignetteFeather / 100.0f) * maxDist * 0.8f)

        // Grain precalculations
        val grainAmount = editState.grain.amount
        val hasGrain = grainAmount > 0
        val grainScale = max(1, (editState.grain.size / 20) + 1)
        val grainRoughness = editState.grain.roughness / 100.0f

        // Multi-threaded parallel slice processing across CPU cores
        val numCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
        val rowsPerCore = (height + numCores - 1) / numCores

        val tasks = (0 until numCores).map { coreIdx ->
            async(Dispatchers.Default) {
                val startY = coreIdx * rowsPerCore
                val endY = min(height, startY + rowsPerCore)

                for (y in startY until endY) {
                    val rowOffset = y * width
                    val dy = y - cy

                    for (x in 0 until width) {
                        val i = rowOffset + x
                        val pixel = pixels[i]
                        val a = (pixel ushr 24) and 0xFF
                        val r = (pixel ushr 16) and 0xFF
                        val g = (pixel ushr 8) and 0xFF
                        val b = pixel and 0xFF

                        // 1. Exposure & Brightness
                        val rf = (r * exposureFactor + brightnessOffset).coerceIn(0.0f, 255.0f)
                        val gf = (g * exposureFactor + brightnessOffset).coerceIn(0.0f, 255.0f)
                        val bf = (b * exposureFactor + brightnessOffset).coerceIn(0.0f, 255.0f)

                        // 2. Monochrome conversion with B&W Channel Mix
                        val monoLuma = if (isMixNeutral) {
                            (rf * 0.299f + gf * 0.587f + bf * 0.114f).coerceIn(0.0f, 255.0f)
                        } else {
                            calculateChannelMixedLuminance(rf, gf, bf, bwMix)
                        }

                        // Blend based on Noir Intensity (1.0 = full monochrome, 0.0 = original color)
                        var outR = if (noirIntensity >= 0.999f) monoLuma else (rf * (1f - noirIntensity) + monoLuma * noirIntensity)
                        var outG = if (noirIntensity >= 0.999f) monoLuma else (gf * (1f - noirIntensity) + monoLuma * noirIntensity)
                        var outB = if (noirIntensity >= 0.999f) monoLuma else (bf * (1f - noirIntensity) + monoLuma * noirIntensity)

                        // 3. Contrast adjustment
                        if (contrastFactor != 1.0f) {
                            outR = contrastFactor * (outR - 128.0f) + 128.0f
                            outG = contrastFactor * (outG - 128.0f) + 128.0f
                            outB = contrastFactor * (outB - 128.0f) + 128.0f
                        }

                        // 4. Highlights, Shadows, Whites, Blacks
                        val curLuma = (outR * 0.299f + outG * 0.587f + outB * 0.114f).coerceIn(0f, 255f)
                        val lumaDelta = if (curLuma > 128.0f) {
                            val normHigh = (curLuma - 128.0f) / 127.0f
                            normHigh * (highlightsFactor * 45.0f + whitesOffset * 0.35f)
                        } else {
                            val normShadow = (128.0f - curLuma) / 128.0f
                            normShadow * (shadowsFactor * 45.0f + blacksOffset * 0.35f)
                        }

                        outR += lumaDelta
                        outG += lumaDelta
                        outB += lumaDelta

                        // 5. Curve LUT Mapping (Evaluates user's tonal curve transfer function)
                        var rInt = curveLut[outR.toInt().coerceIn(0, 255)]
                        var gInt = curveLut[outG.toInt().coerceIn(0, 255)]
                        var bInt = curveLut[outB.toInt().coerceIn(0, 255)]

                        // 6. Fade (lifted matte blacks)
                        if (fadeOffset > 0.1f) {
                            val scale = 1.0f - (fadeOffset / 255.0f)
                            rInt = (fadeOffset + rInt * scale).toInt().coerceIn(0, 255)
                            gInt = (fadeOffset + gInt * scale).toInt().coerceIn(0, 255)
                            bInt = (fadeOffset + bInt * scale).toInt().coerceIn(0, 255)
                        }

                        // 7. Cold Noir / Vintage B&W subtle split character
                        if (isColdNoir) {
                            val coolTint = (rInt / 255.0f) * 12.0f
                            gInt = (gInt + coolTint * 0.5f).toInt().coerceIn(0, 255)
                            bInt = (bInt + coolTint).toInt().coerceIn(0, 255)
                        } else if (isVintageBw) {
                            val warmTint = (rInt / 255.0f) * 10.0f
                            rInt = (rInt + warmTint).toInt().coerceIn(0, 255)
                            bInt = (bInt - warmTint * 0.6f).toInt().coerceIn(0, 255)
                        }

                        // 8. Vignette
                        if (hasVignette) {
                            val dx = x - cx
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist > vMidpoint) {
                                val falloff = ((dist - vMidpoint) / vFeather).coerceIn(0.0f, 1.0f)
                                val smoothFalloff = falloff * falloff * (3.0f - 2.0f * falloff)
                                val vFactor = 1.0f - smoothFalloff * vAmount
                                rInt = (rInt * vFactor).toInt().coerceIn(0, 255)
                                gInt = (gInt * vFactor).toInt().coerceIn(0, 255)
                                bInt = (bInt * vFactor).toInt().coerceIn(0, 255)
                            }
                        }

                        // 9. Procedural Film Grain
                        if (hasGrain) {
                            val px = x / grainScale
                            val py = y / grainScale
                            val noise = pseudoRandomHash(px, py)
                            val midtoneMask = 1.0f - abs(rInt / 255.0f - 0.5f) * 1.5f
                            val gNoise = (noise * grainAmount * 0.6f * midtoneMask.coerceIn(0.2f, 1.0f) * (0.8f + grainRoughness * 0.4f)).toInt()
                            rInt = (rInt + gNoise).coerceIn(0, 255)
                            gInt = (gInt + gNoise).coerceIn(0, 255)
                            bInt = (bInt + gNoise).coerceIn(0, 255)
                        }

                        pixels[i] = (a shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
                    }
                }
            }
        }
        tasks.awaitAll()

        // 10. Clarity & Sharpening (if enabled)
        val sharpVal = editState.sharpness + (editState.clarity.coerceAtLeast(0) / 2)
        if (sharpVal > 0) {
            applySharpeningFilter(pixels, width, height, sharpVal)
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        outBitmap
    }

    /**
     * Calculates luminance with color channel weighting (B&W Channel Mix).
     */
    private fun calculateChannelMixedLuminance(r: Float, g: Float, b: Float, mix: BwMixState): Float {
        val luma = 0.299f * r + 0.587f * g + 0.114f * b
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC

        if (delta < 8.0f) return luma.coerceIn(0.0f, 255.0f)

        val hue = when {
            r == maxC -> (60.0f * ((g - b) / delta) + 360.0f) % 360.0f
            g == maxC -> (60.0f * ((b - r) / delta) + 120.0f)
            else -> (60.0f * ((r - g) / delta) + 240.0f)
        }

        val sat = delta / max(1.0f, maxC)

        val mixDelta = when (hue) {
            in 0.0f..15.0f, in 345.0f..360.0f -> mix.reds
            in 15.0f..45.0f -> (mix.reds + mix.oranges) / 2
            in 45.0f..75.0f -> mix.yellows
            in 75.0f..165.0f -> mix.greens
            in 165.0f..200.0f -> mix.cyans
            in 200.0f..265.0f -> mix.blues
            in 265.0f..345.0f -> mix.magentas
            else -> 0
        }

        val adjustment = (mixDelta / 100.0f) * sat * 65.0f
        return (luma + adjustment).coerceIn(0.0f, 255.0f)
    }

    private fun pseudoRandomHash(x: Int, y: Int): Float {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        val normalized = (h and 0xFFFF) / 65535.0f
        return (normalized * 2.0f) - 1.0f
    }

    private fun applySharpeningFilter(pixels: IntArray, width: Int, height: Int, sharpness: Int) {
        val amount = (sharpness / 100.0f) * 0.75f
        if (amount <= 0.01f || width < 4 || height < 4) return

        val original = pixels.clone()
        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val center = original[idx] and 0xFF
                val up = original[idx - width] and 0xFF
                val down = original[idx + width] and 0xFF
                val left = original[idx - 1] and 0xFF
                val right = original[idx + 1] and 0xFF

                val laplacian = (4 * center) - (up + down + left + right)
                val newLuma = (center + laplacian * amount).toInt().coerceIn(0, 255)

                val a = (pixels[idx] ushr 24) and 0xFF
                pixels[idx] = (a shl 24) or (newLuma shl 16) or (newLuma shl 8) or newLuma
            }
        }
    }
}
