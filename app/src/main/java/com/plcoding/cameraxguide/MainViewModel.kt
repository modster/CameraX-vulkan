package com.plcoding.cameraxguide

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plcoding.cameraxguide.data.PhotoRepository
import com.plcoding.cameraxguide.model.CapturedPhoto
import com.plcoding.cameraxguide.model.ExposureBlendUiMode
import engine.exposure.BitmapFrameAccumulator
import engine.exposure.FrameAccumulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()
    private val _photos = MutableStateFlow<List<CapturedPhoto>>(emptyList())
    val photos = _photos.asStateFlow()

    // ---------------------------------------------------------------------------
    // Long-exposure state
    // ---------------------------------------------------------------------------

    /** Long-exposure accumulation backend (software now, Vulkan/GL-ready contract). */
    private val frameAccumulator: FrameAccumulator = BitmapFrameAccumulator()

    private val _isLongExposureActive = MutableStateFlow(false)
    val isLongExposureActive: StateFlow<Boolean> = _isLongExposureActive.asStateFlow()

    private val _liveAccumulatedFrame = MutableStateFlow<Bitmap?>(null)
    /** Live snapshot of the accumulation buffer; throttled in app for cleaner layering. */
    val liveAccumulatedFrame: StateFlow<Bitmap?> = _liveAccumulatedFrame.asStateFlow()

    private val _exposureDurationMs = MutableStateFlow(0L)
    val exposureDurationMs: StateFlow<Long> = _exposureDurationMs.asStateFlow()

    private var exposureStartTimeMs = 0L
    private var timerJob: Job? = null
    private var analyzedFrameCount = 0

    /**
     * Begin a new long-exposure accumulation pass.
     *
     * @param blendMode Selected UI mode, mapped to engine-level [engine.exposure.ExposureBlendMode].
     * @param blendStrength Normalized blend intensity in `[0f, 1f]`.
     */
    fun startLongExposure(blendMode: ExposureBlendUiMode, blendStrength: Float) {
        frameAccumulator.blendMode = blendMode.engineMode
        frameAccumulator.blendStrength = blendStrength.coerceIn(0f, 1f)
        frameAccumulator.reset()
        analyzedFrameCount = 0
        _liveAccumulatedFrame.value = null
        exposureStartTimeMs = System.currentTimeMillis()
        _exposureDurationMs.value = 0L
        _isLongExposureActive.value = true
        timerJob = viewModelScope.launch {
            while (_isLongExposureActive.value) {
                delay(100L)
                _exposureDurationMs.value = System.currentTimeMillis() - exposureStartTimeMs
            }
        }
    }

    /**
     * Stop the current long-exposure, persist the final accumulated frame, and return it.
     * Returns `null` if no frames were collected.
     */
    fun stopLongExposure(): Bitmap? {
        _isLongExposureActive.value = false
        timerJob?.cancel()
        val finalFrame = frameAccumulator.getFinalFrame() ?: return null
        _liveAccumulatedFrame.value = null
        onTakePhoto(finalFrame)
        onPersistPhoto(finalFrame)
        return finalFrame
    }

    /**
     * Called from the ImageAnalysis executor thread for every camera frame.
     * Forwards the frame to [frameAccumulator] only when a capture is active.
     * The caller remains responsible for recycling [bitmap] after this returns.
     */
    fun onFrameAnalyzed(bitmap: Bitmap) {
        if (_isLongExposureActive.value) {
            frameAccumulator.accumulate(bitmap)
            analyzedFrameCount++
            if (analyzedFrameCount == 1 || analyzedFrameCount % 3 == 0) {
                _liveAccumulatedFrame.value = frameAccumulator.snapshot()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Existing methods
    // ---------------------------------------------------------------------------

    fun onTakePhoto(bitmap: Bitmap) {
        _bitmaps.value += bitmap
    }

    fun onPersistPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            val savedPhoto = withContext(Dispatchers.IO) {
                photoRepository.savePhoto(bitmap)
            }
            savedPhoto?.let {
                _photos.value = listOf(it) + _photos.value
            }
        }
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _photos.value = withContext(Dispatchers.IO) {
                photoRepository.getPhotos()
            }
        }
    }

    override fun onCleared() {
        frameAccumulator.reset()
        super.onCleared()
    }

    companion object {
        fun factory(photoRepository: PhotoRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MainViewModel(photoRepository)
            }
        }
    }
}