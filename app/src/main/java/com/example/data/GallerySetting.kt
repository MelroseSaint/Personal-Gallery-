package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gallery_settings")
data class GallerySetting(
    @PrimaryKey val key: String,
    val value: String
)
