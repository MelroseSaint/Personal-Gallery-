package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Query("SELECT * FROM gallery_images ORDER BY dateAdded DESC")
    fun getAllImages(): Flow<List<GalleryImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GalleryImage)

    @Query("DELETE FROM gallery_images WHERE id = :id")
    suspend fun deleteImageById(id: Int)

    @Query("UPDATE gallery_images SET title = :newTitle WHERE id = :id")
    suspend fun updateImageTitle(id: Int, newTitle: String)

    // Settings
    @Query("SELECT * FROM gallery_settings")
    fun getAllSettings(): Flow<List<GallerySetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: GallerySetting)

    @Query("SELECT value FROM gallery_settings WHERE key = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?
}
