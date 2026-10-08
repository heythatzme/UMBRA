package com.example.model

data class CurvePoint(
    val x: Float, // Normalized 0.0f .. 1.0f
    val y: Float  // Normalized 0.0f .. 1.0f
)

data class CurveState(
    // 5 anchor control points: Black (x=0), Shadow (x=0.25), Midtone (x=0.5), Highlight (x=0.75), White (x=1.0)
    val points: List<CurvePoint> = defaultPoints()
) {
    companion object {
        fun defaultPoints(): List<CurvePoint> = listOf(
            CurvePoint(0.00f, 0.00f),
            CurvePoint(0.25f, 0.25f),
            CurvePoint(0.50f, 0.50f),
            CurvePoint(0.75f, 0.75f),
            CurvePoint(1.00f, 1.00f)
        )
    }

    fun isLinear(): Boolean {
        if (points.size != 5) return false
        return points.all { kotlin.math.abs(it.x - it.y) < 0.01f }
    }
}
