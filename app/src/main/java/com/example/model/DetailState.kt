package com.example.model

data class DetailState(
    val sharpening: Int = 0,     // 0 .. 100
    val radius: Int = 20,        // 0 .. 100
    val detail: Int = 25,        // 0 .. 100
    val noiseReduction: Int = 0  // 0 .. 100
) {
    fun isDefault(): Boolean = sharpening == 0 && noiseReduction == 0
}
