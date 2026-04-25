package com.plcoding.cameraxguide.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.plcoding.cameraxguide.model.CapturedPhoto

class MediaStorePhotoDataSource(
    private val contentResolver: ContentResolver
) {
    suspend fun savePhoto(bitmap: Bitmap): CapturedPhoto? {
        val timestamp = System.currentTimeMillis()
        val displayName = "cameraxguide_${timestamp}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CameraXGuide")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = contentResolver.insert(imageCollection, contentValues) ?: return null

        return runCatching {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            } ?: error("Unable to open output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pendingUpdate = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, pendingUpdate, null, null)
            }

            CapturedPhoto(
                uri = uri,
                displayName = displayName,
                timestamp = timestamp
            )
        }.getOrElse {
            contentResolver.delete(uri, null, null)
            null
        }
    }

    suspend fun getPhotos(): List<CapturedPhoto> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED
        )

        val selection: String?
        val selectionArgs: Array<String>?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ?"
            selectionArgs = arrayOf("${Environment.DIRECTORY_PICTURES}/CameraXGuide/")
        } else {
            selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            selectionArgs = arrayOf("cameraxguide_%")
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        return try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val displayName = cursor.getString(nameColumn) ?: "capture_${id}.jpg"
                        val dateTaken = cursor.getLong(dateTakenColumn)
                        val dateAdded = cursor.getLong(dateAddedColumn)
                        val timestamp = if (dateTaken > 0) dateTaken else dateAdded * 1000
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        add(
                            CapturedPhoto(
                                uri = contentUri,
                                displayName = displayName,
                                timestamp = timestamp
                            )
                        )
                    }
                }
            } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    suspend fun deletePhoto(uri: Uri): Boolean {
        return runCatching {
            contentResolver.delete(uri, null, null) > 0
        }.getOrDefault(false)
    }
}

