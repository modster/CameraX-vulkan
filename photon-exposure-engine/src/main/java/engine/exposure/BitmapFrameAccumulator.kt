package engine.exposure

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * Software implementation of [FrameAccumulator] using Android bitmap compositing.
 */
class BitmapFrameAccumulator : FrameAccumulator {

    override var blendMode: ExposureBlendMode = ExposureBlendMode.SCREEN

    @Volatile
    private var strengthInternal: Float = 1f

    override var blendStrength: Float
        get() = strengthInternal
        set(value) {
            strengthInternal = value.coerceIn(0f, 1f)
        }

    @Volatile
    private var accumulated: Bitmap? = null

    @Volatile
    override var frameCount: Int = 0
        private set

    private val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun reset() {
        synchronized(this) {
            accumulated?.recycle()
            accumulated = null
            frameCount = 0
        }
    }

    override fun accumulate(frame: Bitmap) {
        synchronized(this) {
            val acc = accumulated
            if (acc == null) {
                accumulated = frame.copy(Bitmap.Config.ARGB_8888, true)
                frameCount = 1
            } else {
                blendPaint.xfermode = PorterDuffXfermode(blendMode.toPorterDuffMode())
                blendPaint.alpha = (blendStrength * 255f).toInt().coerceIn(0, 255)
                Canvas(acc).drawBitmap(frame, 0f, 0f, blendPaint)
                frameCount++
            }
        }
    }

    override fun snapshot(): Bitmap? = synchronized(this) {
        accumulated?.copy(Bitmap.Config.ARGB_8888, false)
    }

    override fun getFinalFrame(): Bitmap? = snapshot()

    private fun ExposureBlendMode.toPorterDuffMode(): PorterDuff.Mode {
        return when (this) {
            ExposureBlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
            ExposureBlendMode.SCREEN -> PorterDuff.Mode.SCREEN
            ExposureBlendMode.ADDITIVE -> PorterDuff.Mode.ADD
        }
    }
}

