package com.example.model

enum class ExportFormat(val extension: String, val mimeType: String, val label: String, val badge: String) {
    PNG("png", "image/png", "PNG", "LOSSLESS"),
    JPEG("jpg", "image/jpeg", "JPEG", "MAX QUALITY"),
    WEBP("webp", "image/webp", "WEBP", "COMPACT LOSSLESS")
}

data class ExportSettings(
    val format: ExportFormat = ExportFormat.PNG,
    val quality: Int = 100, // 90, 95, 98, 100
    val targetWidth: Int = 0,
    val targetHeight: Int = 0
) {
    fun estimatedSizeFormatted(): String {
        val pixels = targetWidth.toLong() * targetHeight.toLong()
        if (pixels <= 0) return "~ 8-15 MB"
        val mb = when (format) {
            ExportFormat.PNG -> (pixels * 3.2 / (1024 * 1024)).toInt()
            ExportFormat.JPEG -> when {
                quality == 100 -> (pixels * 1.5 / (1024 * 1024)).toInt()
                quality >= 95 -> (pixels * 0.9 / (1024 * 1024)).toInt()
                else -> (pixels * 0.5 / (1024 * 1024)).toInt()
            }
            ExportFormat.WEBP -> (pixels * 1.1 / (1024 * 1024)).toInt()
        }.coerceAtLeast(1)
        return "approx. $mb MB"
    }
}
