package com.plcoding.cameraxguide

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plcoding.cameraxguide.data.PhotoRepository
import com.plcoding.cameraxguide.model.CapturedPhoto
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

    /** The accumulation engine that blends frames together like a GPU render pass. */
    val longExposureProcessor = LongExposureProcessor()

    private val _isLongExposureActive = MutableStateFlow(false)
    val isLongExposureActive: StateFlow<Boolean> = _isLongExposureActive.asStateFlow()

    /** Live snapshot of the accumulation buffer; updates at ~10 fps while capturing. */
    val liveAccumulatedFrame: StateFlow<Bitmap?> = longExposureProcessor.output

    private val _exposureDurationMs = MutableStateFlow(0L)
    val exposureDurationMs: StateFlow<Long> = _exposureDurationMs.asStateFlow()

    private var exposureStartTimeMs = 0L
    private var timerJob: Job? = null

    /**
     * Begin a new long-exposure accumulation pass.
     *
     * @param blendModeIndex 0 = Lighten, 1 = Screen, 2 = Additive — matches the
     *   existing blend-mode toggle buttons in the UI.
     */
    fun startLongExposure(blendModeIndex: Int) {
        longExposureProcessor.blendMode = when (blendModeIndex) {
            0 -> LongExposureProcessor.BlendMode.LIGHTEN
            2 -> LongExposureProcessor.BlendMode.ADDITIVE
            else -> LongExposureProcessor.BlendMode.SCREEN
        }
        longExposureProcessor.reset()
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
        val finalFrame = longExposureProcessor.getFinalFrame() ?: return null
        onTakePhoto(finalFrame)
        onPersistPhoto(finalFrame)
        return finalFrame
    }

    /**
     * Called from the ImageAnalysis executor thread for every camera frame.
     * Forwards the frame to [longExposureProcessor] only when a capture is active.
     * The caller remains responsible for recycling [bitmap] after this returns.
     */
    fun onFrameAnalyzed(bitmap: Bitmap) {
        if (_isLongExposureActive.value) {
            longExposureProcessor.accumulate(bitmap)
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

    companion object {
        fun factory(photoRepository: PhotoRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MainViewModel(photoRepository)
            }
        }
    }
}