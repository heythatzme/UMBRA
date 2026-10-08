package com.example.model

enum class GrainPreset(val label: String) {
    NONE("None"),
    FINE("Fine"),
    MEDIUM("Medium"),
    HEAVY("Heavy"),
    FILM_35MM("35mm"),
    FILM_16MM("16mm"),
    VINTAGE("Vintage")
}

data class GrainState(
    val amount: Int = 0,      // 0 .. 100
    val size: Int = 30,       // 0 .. 100
    val roughness: Int = 50,  // 0 .. 100
    val preset: GrainPreset = GrainPreset.NONE
) {
    companion object {
        fun fromPreset(preset: GrainPreset): GrainState = when (preset) {
            GrainPreset.NONE -> GrainState(amount = 0, size = 30, roughness = 50, preset = preset)
            GrainPreset.FINE -> GrainState(amount = 25, size = 15, roughness = 35, preset = preset)
            GrainPreset.MEDIUM -> GrainState(amount = 45, size = 30, roughness = 50, preset = preset)
            GrainPreset.HEAVY -> GrainState(amount = 75, size = 45, roughness = 70, preset = preset)
            GrainPreset.FILM_35MM -> GrainState(amount = 40, size = 25, roughness = 45, preset = preset)
            GrainPreset.FILM_16MM -> GrainState(amount = 65, size = 55, roughness = 65, preset = preset)
            GrainPreset.VINTAGE -> GrainState(amount = 55, size = 40, roughness = 80, preset = preset)
        }
    }
}
