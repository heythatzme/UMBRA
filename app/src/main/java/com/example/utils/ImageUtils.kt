package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageUtils {

    data class ImageDimensions(
        val width: Int,
        val height: Int,
        val rotationDegrees: Int = 0
    ) {
        val megapixels: Double
            get() = (width.toLong() * height.toLong()) / 1_000_000.0

        fun formattedResolution(): String = "$width × $height"
        fun formattedMegapixels(): String = String.format(java.util.Locale.US, "%.1f MP", megapixels)
    }

    /**
     * Reads image dimensions and EXIF orientation without loading bitmap pixels into memory.
     */
    suspend fun getImageDimensions(context: Context, uri: Uri): ImageDimensions = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        var rotation = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                rotation = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {}

        val rawW = options.outWidth
        val rawH = options.outHeight

        // Swap width and height if rotated 90 or 270 degrees
        val finalW = if (rotation == 90 || rotation == 270) rawH else rawW
        val finalH = if (rotation == 90 || rotation == 270) rawW else rawH

        ImageDimensions(max(1, finalW), max(1, finalH), rotation)
    }

    /**
     * Decodes a memory-efficient preview bitmap (typically 1440-1920px max dimension)
     * for real-time 60fps slider updates without memory pressure.
     */
    suspend fun decodePreviewBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1280
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val dimensions = getImageDimensions(context, uri)
            var sampleSize = 1
            var w = dimensions.width
            var h = dimensions.height

            while (w > maxDimension || h > maxDimension) {
                sampleSize *= 2
                w /= 2
                h /= 2
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap: Bitmap? = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (bitmap != null && dimensions.rotationDegrees != 0) {
                bitmap = rotateBitmap(bitmap, dimensions.rotationDegrees.toFloat())
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes the FULL resolution original bitmap for lossless / maximum-quality export.
     * Retains full original dimensions (e.g. 6000x4000).
     */
    suspend fun decodeFullResolutionBitmap(
        context: Context,
        uri: Uri
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val dimensions = getImageDimensions(context, uri)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }

            var bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (bitmap != null && dimensions.rotationDegrees != 0) {
                bitmap = rotateBitmap(bitmap, dimensions.rotationDegrees.toFloat())
            }

            bitmap
        } catch (e: OutOfMemoryError) {
            // Graceful fallback if device JVM heap is strictly constrained
            System.gc()
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    /**
     * Generates a sample high-resolution architectural / portrait darkroom photo
     * with rich tonal gradients, shadows, highlights, and contrast for immediate testing.
     */
    fun createSamplePhoto(width: Int = 2400, height: Int = 3200): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Darkroom background with subtle deep gradient
        paint.color = Color.rgb(20, 22, 28)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Architectural geometric perspective pillars and beams
        val cx = width / 2f
        val cy = height * 0.45f

        // Sunlit dramatic architectural window rays
        for (i in 0 until 12) {
            val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(40 + i * 15, 230, 210, 180)
            }
            val startX = width * (0.1f + i * 0.08f)
            canvas.drawRect(startX, 0f, startX + width * 0.035f, height.toFloat(), rayPaint)
        }

        // Deep stone arches
        paint.color = Color.rgb(45, 50, 60)
        canvas.drawRoundRect(width * 0.15f, height * 0.2f, width * 0.85f, height * 0.95f, 180f, 180f, paint)

        // Inner dramatic arch with sky highlight
        paint.color = Color.rgb(180, 195, 210)
        canvas.drawRoundRect(width * 0.25f, height * 0.3f, width * 0.75f, height * 0.95f, 140f, 140f, paint)

        // Human silhouette in doorway (cinematic scale)
        val figurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(8, 8, 10)
        }
        // Head
        canvas.drawCircle(cx, height * 0.72f, width * 0.035f, figurePaint)
        // Body / trench coat silhouette
        canvas.drawRoundRect(cx - width * 0.055f, height * 0.76f, cx + width * 0.055f, height * 0.92f, 20f, 20f, figurePaint)

        // Rich color patches for testing B&W channel mix (Red sign, Blue sky, Green foliage)
        // Red neon sign
        paint.color = Color.rgb(235, 45, 45)
        canvas.drawRoundRect(width * 0.32f, height * 0.42f, width * 0.68f, height * 0.47f, 15f, 15f, paint)

        // Blue glow highlight
        paint.color = Color.rgb(40, 120, 240)
        canvas.drawCircle(width * 0.35f, height * 0.26f, width * 0.08f, paint)

        // Golden/Yellow accent
        paint.color = Color.rgb(240, 190, 40)
        canvas.drawCircle(width * 0.65f, height * 0.26f, width * 0.07f, paint)

        return bitmap
    }
}
