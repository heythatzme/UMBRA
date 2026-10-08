package com.example.model

data class EditState(
    // Tone adjustments
    val exposure: Float = 0.0f,          // -2.0 .. +2.0
    val brightness: Int = 0,             // -100 .. +100
    val contrast: Int = 0,               // -100 .. +100
    val highlights: Int = 0,             // -100 .. +100
    val shadows: Int = 0,                // -100 .. +100
    val whites: Int = 0,                 // -100 .. +100
    val blacks: Int = 0,                 // -100 .. +100
    val clarity: Int = 0,                // -100 .. +100
    val texture: Int = 0,                // -100 .. +100
    val sharpness: Int = 0,              // 0 .. 100

    // Noir & Atmosphere
    val noirIntensity: Int = 100,        // 0 .. 100 (100 = full monochrome conversion)
    val fade: Int = 0,                   // 0 .. 100 (lifted matte blacks)
    val presetId: NoirPresetId = NoirPresetId.PURE,

    // Vignette
    val vignetteAmount: Int = 0,         // 0 .. 100
    val vignetteMidpoint: Int = 50,      // 0 .. 100
    val vignetteFeather: Int = 60,       // 0 .. 100
    val vignetteRoundness: Int = 50,     // 0 .. 100

    // Advanced Engines
    val bwMix: BwMixState = BwMixState(),
    val curves: CurveState = CurveState(),
    val grain: GrainState = GrainState(),
    val detail: DetailState = DetailState()
) {

    fun isModified(): Boolean =
        exposure != 0f || brightness != 0 || contrast != 0 ||
        highlights != 0 || shadows != 0 || whites != 0 || blacks != 0 ||
        clarity != 0 || texture != 0 || sharpness != 0 ||
        fade != 0 || vignetteAmount != 0 ||
        presetId != NoirPresetId.PURE || !bwMix.isNeutral() ||
        !curves.isLinear() || grain.amount != 0 || !detail.isDefault()

    fun resetAdjustments(): EditState = copy(
        exposure = 0f,
        brightness = 0,
        contrast = 0,
        highlights = 0,
        shadows = 0,
        whites = 0,
        blacks = 0,
        clarity = 0,
        texture = 0
    )

    fun resetNoir(): EditState = copy(
        presetId = NoirPresetId.PURE,
        noirIntensity = 100,
        fade = 0
    )

    fun resetBwMix(): EditState = copy(
        bwMix = BwMixState()
    )

    fun resetCurves(): EditState = copy(
        curves = CurveState()
    )

    fun resetGrain(): EditState = copy(
        grain = GrainState()
    )

    fun resetDetail(): EditState = copy(
        sharpness = 0,
        detail = DetailState()
    )

    fun resetVignette(): EditState = copy(
        vignetteAmount = 0,
        vignetteMidpoint = 50,
        vignetteFeather = 60,
        vignetteRoundness = 50
    )

    companion object {
        val DEFAULT = EditState()

        /**
         * Creates an EditState tailored with specific parameters for each Noir preset.
         */
        fun fromPreset(preset: NoirPresetId): EditState = when (preset) {
            NoirPresetId.ORIGINAL -> EditState(
                presetId = NoirPresetId.ORIGINAL,
                noirIntensity = 0
            )

            NoirPresetId.PURE -> EditState(
                presetId = NoirPresetId.PURE,
                noirIntensity = 100
            )

            NoirPresetId.DEEP_NOIR -> EditState(
                presetId = NoirPresetId.DEEP_NOIR,
                contrast = 35,
                blacks = -30,
                whites = 15,
                highlights = -10,
                shadows = -15,
                clarity = 25,
                vignetteAmount = 20
            )

            NoirPresetId.OBSCURA -> EditState(
                presetId = NoirPresetId.OBSCURA,
                exposure = -0.3f,
                contrast = 20,
                shadows = -35,
                highlights = -25,
                fade = 25,
                vignetteAmount = 35,
                vignetteMidpoint = 45
            )

            NoirPresetId.SILVER -> EditState(
                presetId = NoirPresetId.SILVER,
                contrast = 8,
                whites = 20,
                highlights = 15,
                shadows = 10,
                texture = 10,
                bwMix = BwMixState(reds = 10, blues = 15)
            )

            NoirPresetId.HARD_NOIR -> EditState(
                presetId = NoirPresetId.HARD_NOIR,
                contrast = 55,
                blacks = -45,
                whites = 35,
                highlights = 20,
                clarity = 35,
                sharpness = 25
            )

            NoirPresetId.SOFT_NOIR -> EditState(
                presetId = NoirPresetId.SOFT_NOIR,
                contrast = -15,
                highlights = -10,
                shadows = 20,
                fade = 15,
                clarity = -10
            )

            NoirPresetId.FILM_NOIR -> EditState(
                presetId = NoirPresetId.FILM_NOIR,
                contrast = 30,
                blacks = -20,
                whites = 15,
                vignetteAmount = 35,
                fade = 10,
                grain = GrainState.fromPreset(GrainPreset.FILM_35MM)
            )

            NoirPresetId.LOW_KEY -> EditState(
                presetId = NoirPresetId.LOW_KEY,
                exposure = -0.6f,
                shadows = -50,
                blacks = -35,
                contrast = 25,
                highlights = 10,
                whites = 15,
                vignetteAmount = 40
            )

            NoirPresetId.HIGH_KEY -> EditState(
                presetId = NoirPresetId.HIGH_KEY,
                exposure = 0.7f,
                shadows = 40,
                highlights = 20,
                whites = 30,
                blacks = 15,
                contrast = -10
            )

            NoirPresetId.COLD_NOIR -> EditState(
                presetId = NoirPresetId.COLD_NOIR,
                contrast = 20,
                highlights = 10,
                shadows = -10,
                bwMix = BwMixState(cyans = 25, blues = 30, yellows = -20)
            )

            NoirPresetId.VINTAGE_BW -> EditState(
                presetId = NoirPresetId.VINTAGE_BW,
                contrast = 15,
                fade = 30,
                texture = -15,
                vignetteAmount = 25,
                grain = GrainState.fromPreset(GrainPreset.VINTAGE),
                bwMix = BwMixState(reds = 20, yellows = 15, blues = -25)
            )

            NoirPresetId.PORTRAIT_NOIR -> EditState(
                presetId = NoirPresetId.PORTRAIT_NOIR,
                contrast = 15,
                highlights = -10,
                shadows = 15,
                texture = 5,
                sharpness = 15,
                bwMix = BwMixState(reds = 35, oranges = 30, yellows = 15)
            )
        }
    }
}
