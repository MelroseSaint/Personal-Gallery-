package com.example.data

import kotlinx.coroutines.flow.Flow

class GalleryRepository(private val galleryDao: GalleryDao) {
    val allImages: Flow<List<GalleryImage>> = galleryDao.getAllImages()
    val allSettings: Flow<List<GallerySetting>> = galleryDao.getAllSettings()

    suspend fun insertImage(image: GalleryImage) {
        galleryDao.insertImage(image)
    }

    suspend fun deleteImage(id: Int) {
        galleryDao.deleteImageById(id)
    }

    suspend fun updateImageTitle(id: Int, title: String) {
        galleryDao.updateImageTitle(id, title)
    }

    suspend fun saveSetting(key: String, value: String) {
        galleryDao.saveSetting(GallerySetting(key, value))
    }

    suspend fun getSettingValue(key: String): String? {
        return galleryDao.getSettingValue(key)
    }
}
