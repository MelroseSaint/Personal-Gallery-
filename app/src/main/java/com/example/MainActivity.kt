package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.AppDatabase
import com.example.data.GalleryRepository
import com.example.ui.GalleryHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database, Repo, and ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = GalleryRepository(database.galleryDao())
        val viewModel = GalleryViewModel(repository)

        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settingsState.collectAsState()
            val darkTheme = when (settings.themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            
            MyApplicationTheme(darkTheme = darkTheme) {
                GalleryHomeScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
