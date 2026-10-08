package com.example.model

/**
 * Color channel contributions to monochrome luminance.
 * Values range from -100 to +100 (0 is neutral standard Rec.709/BT.601 weights).
 */
data class BwMixState(
    val reds: Int = 0,
    val oranges: Int = 0,
    val yellows: Int = 0,
    val greens: Int = 0,
    val cyans: Int = 0,
    val blues: Int = 0,
    val magentas: Int = 0
) {
    fun isNeutral(): Boolean =
        reds == 0 && oranges == 0 && yellows == 0 && greens == 0 &&
        cyans == 0 && blues == 0 && magentas == 0
}
