package com.plcoding.cameraxguide.model

import engine.exposure.ExposureBlendMode

sealed class ExposureBlendUiMode(
    val label: String,
    val engineMode: ExposureBlendMode
) {
    data object Lighten : ExposureBlendUiMode("Lighten", ExposureBlendMode.LIGHTEN)
    data object Screen : ExposureBlendUiMode("Screen", ExposureBlendMode.SCREEN)
    data object Additive : ExposureBlendUiMode("Additive", ExposureBlendMode.ADDITIVE)

    companion object {
        val all = listOf(Lighten, Screen, Additive)
    }
}

