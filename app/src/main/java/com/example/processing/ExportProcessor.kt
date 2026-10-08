package com.example.processing

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.EditState
import com.example.model.ExportFormat
import com.example.model.ExportSettings
import com.example.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ExportProcessor {

    data class ExportResult(
        val success: Boolean,
        val outputUri: Uri? = null,
        val exportedWidth: Int = 0,
        val exportedHeight: Int = 0,
        val bytesWritten: Long = 0,
        val format: ExportFormat = ExportFormat.PNG,
        val quality: Int = 100,
        val errorMessage: String? = null
    ) {
        val megapixels: Double
            get() = (exportedWidth.toLong() * exportedHeight.toLong()) / 1_000_000.0

        fun formattedResolution(): String = "$exportedWidth × $exportedHeight"
        fun formattedMegapixels(): String = String.format(java.util.Locale.US, "%.1f MP", megapixels)
        fun formattedFileSize(): String {
            val mb = bytesWritten / (1024.0 * 1024.0)
            return if (mb >= 1.0) String.format(java.util.Locale.US, "%.1f MB", mb)
            else String.format(java.util.Locale.US, "%d KB", bytesWritten / 1024)
        }
    }

    /**
     * Executes true full-resolution export without downscaling or resolution loss.
     */
    suspend fun exportImage(
        context: Context,
        sourceUri: Uri?,
        sampleBitmapFallback: Bitmap?,
        editState: EditState,
        settings: ExportSettings,
        saveToGallery: Boolean = true,
        onProgress: (Float, String) -> Unit
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            onProgress(0.10f, "Decoding original full-resolution source...")

            // 1. Decode original full-resolution bitmap
            val fullResBitmap: Bitmap? = if (sourceUri != null) {
                ImageUtils.decodeFullResolutionBitmap(context, sourceUri)
            } else {
                sampleBitmapFallback
            }

            if (fullResBitmap == null) {
                return@withContext ExportResult(
                    success = false,
                    errorMessage = "Unable to decode original source image. Insufficient memory or corrupted file."
                )
            }

            val origWidth = fullResBitmap.width
            val origHeight = fullResBitmap.height

            onProgress(0.35f, "Processing Noir engine at full resolution ($origWidth × $origHeight)...")

            // 2. Render non-destructive edit at 100% full original resolution
            val processedBitmap = NoirProcessor.render(fullResBitmap, editState)

            // Recycle intermediate decoded bitmap if different
            if (fullResBitmap != sampleBitmapFallback && fullResBitmap != processedBitmap) {
                fullResBitmap.recycle()
            }

            onProgress(0.75f, "Encoding ${settings.format.label} (${settings.quality}% quality)...")

            val timestamp = System.currentTimeMillis()
            val fileName = "UMBRA_${timestamp}.${settings.format.extension}"

            var bytesCount = 0L
            var finalUri: Uri? = null

            if (saveToGallery) {
                // Save to Android MediaStore Pictures/UMBRA
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, settings.format.mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/UMBRA")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val itemUri = context.contentResolver.insert(collection, contentValues)
                    ?: throw IllegalStateException("Failed to create MediaStore entry")

                context.contentResolver.openOutputStream(itemUri)?.use { outStream ->
                    bytesCount = compressBitmap(processedBitmap, settings, outStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(itemUri, contentValues, null, null)
                }

                finalUri = itemUri
            } else {
                // Save to app cache for immediate sharing
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val exportFile = File(exportDir, fileName)
                FileOutputStream(exportFile).use { outStream ->
                    bytesCount = compressBitmap(processedBitmap, settings, outStream)
                }
                finalUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exportFile
                )
            }

            onProgress(1.0f, "Export Complete!")

            ExportResult(
                success = true,
                outputUri = finalUri,
                exportedWidth = origWidth,
                exportedHeight = origHeight,
                bytesWritten = bytesCount,
                format = settings.format,
                quality = settings.quality
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Export failed unexpectedly"
            )
        }
    }

    private fun compressBitmap(bitmap: Bitmap, settings: ExportSettings, stream: OutputStream): Long {
        val countingStream = CountingOutputStream(stream)
        when (settings.format) {
            ExportFormat.PNG -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, countingStream)
            }
            ExportFormat.JPEG -> {
                bitmap.compress(Bitmap.CompressFormat.JPEG, settings.quality.coerceIn(90, 100), countingStream)
            }
            ExportFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (settings.quality >= 100) {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, countingStream)
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, settings.quality, countingStream)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, settings.quality, countingStream)
                }
            }
        }
        countingStream.flush()
        return countingStream.bytesCount
    }

    /**
     * Triggers standard Android Share sheet for exported image file.
     */
    fun shareExportedImage(context: Context, uri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share UMBRA Photograph"))
    }

    private class CountingOutputStream(private val target: OutputStream) : OutputStream() {
        var bytesCount: Long = 0
            private set

        override fun write(b: Int) {
            target.write(b)
            bytesCount++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            target.write(b, off, len)
            bytesCount += len
        }

        override fun flush() = target.flush()
        override fun close() = target.close()
    }
}
