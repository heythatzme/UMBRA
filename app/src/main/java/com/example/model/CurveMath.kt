package com.example.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object CurveMath {

    /**
     * Precomputes a 256-element Look-Up Table (LUT) from control points
     * using the Fritsch-Carlson Monotone Cubic Spline interpolation algorithm.
     * Guarantees monotonicity without overshoot or ringing, mirroring professional photo tone curves.
     */
    fun computeLut(points: List<CurvePoint>): IntArray {
        val lut = IntArray(256)
        if (points.isEmpty()) {
            for (i in 0..255) lut[i] = i
            return lut
        }

        // Sort strictly by X coordinate
        val sorted = points.sortedBy { it.x }
        val n = sorted.size

        if (n == 1) {
            val constVal = (sorted[0].y * 255f).roundToInt().coerceIn(0, 255)
            for (i in 0..255) lut[i] = constVal
            return lut
        }

        // 1. Calculate secant slopes between consecutive points: delta_k = (y_{k+1} - y_k) / (x_{k+1} - x_k)
        val deltas = FloatArray(n - 1)
        val h = FloatArray(n - 1)
        for (k in 0 until n - 1) {
            h[k] = max(1e-5f, sorted[k + 1].x - sorted[k].x)
            deltas[k] = (sorted[k + 1].y - sorted[k].y) / h[k]
        }

        // 2. Initialize tangents m_k
        val m = FloatArray(n)
        m[0] = deltas[0]
        m[n - 1] = deltas[n - 2]
        for (k in 1 until n - 1) {
            if (deltas[k - 1] * deltas[k] <= 0f) {
                m[k] = 0f
            } else {
                m[k] = (deltas[k - 1] + deltas[k]) / 2f
            }
        }

        // 3. Fritsch-Carlson monotonicity condition adjustments
        for (k in 0 until n - 1) {
            if (abs(deltas[k]) < 1e-7f) {
                m[k] = 0f
                m[k + 1] = 0f
            } else {
                val alpha = m[k] / deltas[k]
                val beta = m[k + 1] / deltas[k]
                if (alpha < 0f) m[k] = 0f
                if (beta < 0f) m[k + 1] = 0f

                val lenSq = alpha * alpha + beta * beta
                if (lenSq > 9f) {
                    val tau = 3f / sqrt(lenSq)
                    m[k] = tau * alpha * deltas[k]
                    m[k + 1] = tau * beta * deltas[k]
                }
            }
        }

        // 4. Evaluate cubic Hermite spline for all inputs 0 .. 255 (normalized to 0.0 .. 1.0)
        for (i in 0..255) {
            val x = i / 255.0f

            val evaluatedY = when {
                x <= sorted.first().x -> sorted.first().y
                x >= sorted.last().x -> sorted.last().y
                else -> {
                    // Find segment [x_k, x_{k+1}] containing x
                    var k = 0
                    while (k < n - 2 && sorted[k + 1].x < x) {
                        k++
                    }

                    val segH = h[k]
                    val t = if (segH > 1e-5f) (x - sorted[k].x) / segH else 0f
                    val t2 = t * t
                    val t3 = t2 * t

                    // Hermite basis functions
                    val h00 = 2f * t3 - 3f * t2 + 1f
                    val h10 = t3 - 2f * t2 + t
                    val h01 = -2f * t3 + 3f * t2
                    val h11 = t3 - t2

                    val y = h00 * sorted[k].y +
                            h10 * segH * m[k] +
                            h01 * sorted[k + 1].y +
                            h11 * segH * m[k + 1]

                    y
                }
            }

            lut[i] = (evaluatedY.coerceIn(0.0f, 1.0f) * 255.0f).roundToInt().coerceIn(0, 255)
        }

        return lut
    }
}
