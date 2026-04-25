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