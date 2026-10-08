package com.example.model

enum class NoirPresetId(val displayName: String, val tag: String, val description: String) {
    ORIGINAL("ORIGINAL", "COLOR", "Unprocessed original color photograph"),
    PURE("PURE", "NEUTRAL", "Balanced neutral black-and-white conversion"),
    DEEP_NOIR("DEEP NOIR", "CINEMATIC", "Strong contrast, deep blacks, controlled highlights"),
    OBSCURA("OBSCURA", "ATMOSPHERE", "Dark shadows, muted highlights, subtle fade"),
    SILVER("SILVER", "LUSTRE", "Soft grayscale, smooth transitions, gentle contrast"),
    HARD_NOIR("HARD NOIR", "GRAPHIC", "Extreme black/white separation, razor tonal edges"),
    SOFT_NOIR("SOFT NOIR", "DREAM", "Low harshness, smooth highlights, subtle blacks"),
    FILM_NOIR("FILM NOIR", "35MM", "High contrast, classic grain, subtle vignette"),
    LOW_KEY("LOW KEY", "SHADOW", "Predominantly dark, preserving focal highlights"),
    HIGH_KEY("HIGH KEY", "LUMINOUS", "Bright monochrome, soft shadows, clean whites"),
    COLD_NOIR("COLD NOIR", "SILVER/CYAN", "Monochrome with subtle cool silver character"),
    VINTAGE_BW("VINTAGE B&W", "PRINT", "Matte fade, authentic grain, aged paper character"),
    PORTRAIT_NOIR("PORTRAIT NOIR", "SKIN TONE", "Prioritizes facial luminance and smooth skin tones")
}
