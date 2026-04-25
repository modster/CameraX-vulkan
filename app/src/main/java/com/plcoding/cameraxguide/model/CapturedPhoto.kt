package com.plcoding.cameraxguide.model

import android.net.Uri

data class CapturedPhoto(
    val uri: Uri,
    val displayName: String,
    val timestamp: Long
)

