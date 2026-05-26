package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gallery_images")
data class GalleryImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val localPath: String, // File path of the image/video copied to app storage
    val title: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val isVideo: Boolean = false
)
