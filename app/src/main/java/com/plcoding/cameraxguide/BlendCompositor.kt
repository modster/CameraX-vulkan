package com.plcoding.cameraxguide

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build

/**
 * The three long-exposure frame blending modes available in the camera UI.
 */
enum class FrameBlendMode {
    LIGHTEN,
    SCREEN,
    ADDITIVE;

    companion object {
        fun fromIndex(index: Int): FrameBlendMode = entries[index.coerceIn(0, entries.lastIndex)]
    }
}

/**
 * Composites [overlay] on top of [base] using the requested [mode].
 *
 * [strength] (0.0–1.0) controls the alpha weight of the overlay layer, letting the user
 * dial back how aggressively new frames are blended into the accumulation buffer.
 *
 * On API 29+ the native [android.graphics.BlendMode] values are used for pixel-accurate
 * results.  On older devices the closest [PorterDuff.Mode] equivalent is applied.
 */
fun compositeBitmaps(
    base: Bitmap,
    overlay: Bitmap,
    mode: FrameBlendMode,
    strength: Float
): Bitmap {
    val w = base.width
    val h = base.height

    // Scale the incoming frame to match the accumulation buffer's dimensions if needed.
    val scaledOverlay = if (overlay.width != w || overlay.height != h) {
        Bitmap.createScaledBitmap(overlay, w, h, true)
    } else {
        overlay
    }

    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    // Draw the accumulated base at full opacity.
    canvas.drawBitmap(base, 0f, 0f, null)

    // Build a paint that blends the new overlay frame at the requested strength.
    val paint = Paint().apply {
        alpha = (255 * strength.coerceIn(0f, 1f)).toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            blendMode = when (mode) {
                FrameBlendMode.LIGHTEN -> android.graphics.BlendMode.LIGHTEN
                FrameBlendMode.SCREEN -> android.graphics.BlendMode.SCREEN
                FrameBlendMode.ADDITIVE -> android.graphics.BlendMode.PLUS
            }
        } else {
            @Suppress("DEPRECATION")
            xfermode = PorterDuffXfermode(
                when (mode) {
                    FrameBlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
                    FrameBlendMode.SCREEN -> PorterDuff.Mode.SCREEN
                    // PorterDuff has no ADD mode; SCREEN is the closest visual proxy.
                    FrameBlendMode.ADDITIVE -> PorterDuff.Mode.SCREEN
                }
            )
        }
    }

    canvas.drawBitmap(scaledOverlay, 0f, 0f, paint)
    return result
}
