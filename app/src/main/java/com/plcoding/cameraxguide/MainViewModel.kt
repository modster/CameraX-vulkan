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
import kotlinx.coroutines.flow.MutableStateFlow
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

    /**
     * The rolling composited frame produced by accumulating captures with the active blend mode.
     * Null until at least one photo has been taken.
     */
    private val _blendedBitmap = MutableStateFlow<Bitmap?>(null)
    val blendedBitmap = _blendedBitmap.asStateFlow()

    /**
     * Accept a new captured frame, composite it into the accumulation buffer using
     * [blendModeIndex] and [blendStrength], then append the result to the in-memory list.
     *
     * @param bitmap        Raw rotated bitmap from the camera.
     * @param blendModeIndex Index into [FrameBlendMode.entries] (0=Lighten, 1=Screen, 2=Additive).
     * @param blendStrength  0.0–1.0 weight applied to each incoming frame.
     */
    fun onTakePhoto(bitmap: Bitmap, blendModeIndex: Int = 1, blendStrength: Float = 0.75f) {
        val mode = FrameBlendMode.fromIndex(blendModeIndex)
        val composited = _blendedBitmap.value?.let { base ->
            compositeBitmaps(base, bitmap, mode, blendStrength)
        } ?: bitmap
        _blendedBitmap.value = composited
        _bitmaps.value += composited
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