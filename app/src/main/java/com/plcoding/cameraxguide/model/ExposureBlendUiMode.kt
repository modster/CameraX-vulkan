package com.plcoding.cameraxguide.model

import engine.exposure.ExposureBlendMode

enum class ExposureBlendUiMode(
    val label: String,
    val engineMode: ExposureBlendMode
) {
    LIGHTEN("Lighten", ExposureBlendMode.LIGHTEN),
    SCREEN("Screen", ExposureBlendMode.SCREEN),
    ADDITIVE("Additive", ExposureBlendMode.ADDITIVE);

    companion object {
        val all: List<ExposureBlendUiMode> = entries
    }
}

