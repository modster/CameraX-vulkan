package engine.exposure

import android.graphics.Bitmap

/**
 * Backend-agnostic frame accumulation contract for long-exposure engines.
 *
 * Implementations may be software-based (Bitmap/Canvas) or GPU-based (GL/Vulkan).
 */
interface FrameAccumulator {

    /** Active blend mode; apply before [reset] to start a new exposure pass. */
    var blendMode: ExposureBlendMode

    /** Number of frames merged since the last [reset]. */
    val frameCount: Int

    /** Clear internal buffers and prepare for a new exposure pass. */
    fun reset()

    /**
     * Merge [frame] into the internal accumulation buffer.
     *
     * The caller remains responsible for recycling [frame] after this call returns.
     */
    fun accumulate(frame: Bitmap)

    /**
     * Return a read-only copy of the current accumulated image, or `null` if empty.
     *
     * Ownership: the caller owns the returned bitmap copy and must recycle it when done.
     */
    fun snapshot(): Bitmap?

    /**
     * Return a read-only copy of the final accumulated image, or `null` if empty.
     *
     * Ownership: the caller owns the returned bitmap copy and must recycle it when done.
     */
    fun getFinalFrame(): Bitmap?
}

