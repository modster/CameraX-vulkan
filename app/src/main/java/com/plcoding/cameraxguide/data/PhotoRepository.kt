package com.plcoding.cameraxguide.data

import android.graphics.Bitmap
import android.net.Uri
import com.plcoding.cameraxguide.model.CapturedPhoto

interface PhotoRepository {
    suspend fun savePhoto(bitmap: Bitmap): CapturedPhoto?
    suspend fun getPhotos(): List<CapturedPhoto>
    suspend fun deletePhoto(uri: Uri): Boolean
}

class PhotoRepositoryImpl(
    private val dataSource: MediaStorePhotoDataSource
) : PhotoRepository {
    override suspend fun savePhoto(bitmap: Bitmap): CapturedPhoto? {
        return dataSource.savePhoto(bitmap)
    }

    override suspend fun getPhotos(): List<CapturedPhoto> {
        return dataSource.getPhotos()
    }

    override suspend fun deletePhoto(uri: Uri): Boolean {
        return dataSource.deletePhoto(uri)
    }
}

