package com.example.model

data class SavedProject(
    val id: String,
    val name: String,
    val imageUri: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val timestamp: Long,
    val editState: EditState
) {
    fun resolutionString(): String = "$originalWidth × $originalHeight"

    fun megapixelsString(): String {
        val mp = (originalWidth.toLong() * originalHeight.toLong()) / 1_000_000.0
        return String.format(java.util.Locale.US, "%.1f MP", mp)
    }
}
