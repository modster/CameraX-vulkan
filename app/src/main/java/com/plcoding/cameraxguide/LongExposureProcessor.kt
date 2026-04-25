package com.plcoding.cameraxguide

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Accumulates camera frames using GPU-pipeline blend modes to simulate long-exposure photography.
 *
 * This mirrors what a Vulkan compute shader would do for multi-frame light integration:
 * each incoming frame is composited onto an accumulation buffer using one of three standard
 * GPU framebuffer blend operations:
 *
 *   LIGHTEN  — max(src, dst) per channel.  Great for star trails, lightning.
 *   SCREEN   — 1−(1−src)·(1−dst) per channel.  Natural photographic light integration.
 *   ADDITIVE — saturate(src+dst) per channel.  Light-painting, fireworks, neon.
 *
 * The processor is intentionally framework-agnostic: it receives plain [Bitmap] frames so
 * that the ImageAnalysis pipeline, a software renderer, or a future Vulkan/GLSL compute back-end
 * can all feed it transparently.
 */
class LongExposureProcessor {

    enum class BlendMode(val porterDuffMode: PorterDuff.Mode) {
        LIGHTEN(PorterDuff.Mode.LIGHTEN),
        SCREEN(PorterDuff.Mode.SCREEN),
        ADDITIVE(PorterDuff.Mode.ADD)
    }

    /** Active blend mode; may be changed before calling [reset]. */
    @Volatile var blendMode: BlendMode = BlendMode.SCREEN

    @Volatile private var accumulated: Bitmap? = null

    /** Number of frames merged into the accumulation buffer since the last [reset]. */
    @Volatile var frameCount: Int = 0
        private set

    private val _output = MutableStateFlow<Bitmap?>(null)

    /**
     * Emits a snapshot of the current accumulation buffer after every third frame,
     * keeping the UI refresh rate at roughly 10 fps when the camera runs at 30 fps.
     */
    val output: StateFlow<Bitmap?> = _output.asStateFlow()

    private val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Clear the accumulation buffer and prepare for a new exposure. */
    fun reset() {
        synchronized(this) {
            accumulated?.recycle()
            accumulated = null
            frameCount = 0
        }
        _output.value = null
    }

    /**
     * Blend [frame] into the accumulation buffer.
     *
     * Thread-safe — called on a background executor from the CameraX ImageAnalysis callback.
     * The caller is responsible for recycling [frame] after this method returns.
     */
    fun accumulate(frame: Bitmap) {
        synchronized(this) {
            val acc = accumulated
            if (acc == null) {
                // First frame: seed the accumulation buffer.
                accumulated = frame.copy(Bitmap.Config.ARGB_8888, true)
                frameCount = 1
            } else {
                blendPaint.xfermode = PorterDuffXfermode(blendMode.porterDuffMode)
                Canvas(acc).drawBitmap(frame, 0f, 0f, blendPaint)
                frameCount++
            }
            // Emit a read-only snapshot on the first frame and every 3rd frame thereafter
            // so that the Compose UI gets ~10 fps updates without being flooded.
            // Note: the previous snapshot is NOT explicitly recycled here because Compose
            // may still hold a reference to it while rendering; explicit recycling of a
            // displayed bitmap causes a crash.  The GC reclaims each snapshot promptly once
            // Compose discards it on the next recomposition (~100 ms later at 30 fps), so
            // allocation pressure remains negligible for the 960-pixel target resolution.
            if (frameCount == 1 || frameCount % 3 == 0) {
                _output.value = accumulated?.copy(Bitmap.Config.ARGB_8888, false)
            }
        }
    }

    /**
     * Return a copy of the final accumulated bitmap.  The caller owns the returned bitmap
     * and must recycle it when no longer needed.
     */
    fun getFinalFrame(): Bitmap? = synchronized(this) {
        accumulated?.copy(Bitmap.Config.ARGB_8888, false)
    }
}
