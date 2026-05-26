package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GalleryImage
import com.example.data.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class BackgroundSettings(
    val type: String = "gradient", // "solid", "gradient", "pattern", "image"
    val valColor: String = "#0F172A", // Slate Dark
    val valGradient: String = "Midnight Nebula",
    val bgImagePath: String = "",
    val bgBlur: Float = 6f, // Blur radius/multiplier
    val bgDim: Float = 0.4f, // Screen dim ratio
    val gridColumns: Int = 2,
    val showDotOverlay: Boolean = true,
    val themeMode: String = "system" // system, light, dark
)

class GalleryViewModel(private val repository: GalleryRepository) : ViewModel() {

    val imagesState: StateFlow<List<GalleryImage>> = repository.allImages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _liveBlur = MutableStateFlow<Float?>(null)
    private val _liveDim = MutableStateFlow<Float?>(null)

    fun updateLiveBlur(blur: Float) {
        _liveBlur.value = blur
    }

    fun updateLiveDim(dim: Float) {
        _liveDim.value = dim
    }

    val settingsState: StateFlow<BackgroundSettings> = combine(
        repository.allSettings,
        _liveBlur,
        _liveDim
    ) { list, liveBlur, liveDim ->
        val map = list.associate { it.key to it.value }
        BackgroundSettings(
            type = map["bg_type"] ?: "gradient",
            valColor = map["bg_value_color"] ?: "#0F172A",
            valGradient = map["bg_value_gradient"] ?: "Midnight Nebula",
            bgImagePath = map["bg_image_path"] ?: "",
            bgBlur = liveBlur ?: map["bg_blur"]?.toFloatOrNull() ?: 6f,
            bgDim = liveDim ?: map["bg_dim"]?.toFloatOrNull() ?: 0.35f,
            gridColumns = map["grid_columns"]?.toIntOrNull() ?: 2,
            showDotOverlay = map["show_dot_overlay"]?.toBooleanStrictOrNull() ?: true,
            themeMode = map["theme_mode"] ?: "system"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BackgroundSettings()
    )

    fun importMedia(context: Context, uri: Uri, title: String, isVideo: Boolean, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = copyUriToInternalStorage(context, uri)
            if (path != null) {
                repository.insertImage(GalleryImage(localPath = path, title = title, isVideo = isVideo))
                launch(Dispatchers.Main) { onFinished(true) }
            } else {
                launch(Dispatchers.Main) { onFinished(false) }
            }
        }
    }

    fun importMediaFileDirectly(absolutePath: String, title: String, isVideo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertImage(
                GalleryImage(
                    localPath = absolutePath,
                    title = title,
                    isVideo = isVideo
                )
            )
        }
    }

    fun deleteImage(image: GalleryImage) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(image.localPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteImage(image.id)
        }
    }

    fun updateImageTitle(image: GalleryImage, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateImageTitle(image.id, newTitle)
        }
    }

    fun saveBackgroundType(type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting("bg_type", type)
        }
    }

    fun saveBackgroundValueColor(hexString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting("bg_value_color", hexString)
            repository.saveSetting("bg_type", "solid")
        }
    }

    fun saveBackgroundValueGradient(gradientName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting("bg_value_gradient", gradientName)
            repository.saveSetting("bg_type", "gradient")
        }
    }

    fun saveCustomImageBackground(context: Context, uri: Uri, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val oldPath = repository.getSettingValue("bg_image_path")
                if (!oldPath.isNullOrEmpty()) {
                    val oldFile = File(oldPath)
                    if (oldFile.exists()) oldFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val path = copyUriToInternalStorage(context, uri)
            if (path != null) {
                repository.saveSetting("bg_image_path", path)
                repository.saveSetting("bg_type", "image")
                launch(Dispatchers.Main) { onFinished(true) }
            } else {
                launch(Dispatchers.Main) { onFinished(false) }
            }
        }
    }

    fun saveThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting("theme_mode", mode)
        }
    }

    fun saveSliderBlur(blur: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting("bg_blur", blur.toString())
            _liveBlur.value = null
        }
    }

    fun saveSliderDim(dim: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting("bg_dim", dim.toString())
            _liveDim.value = null
        }
    }

    fun saveGridColumns(columns: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting("grid_columns", columns.toString())
        }
    }

    fun saveDotOverlay(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting("show_dot_overlay", show.toString())
        }
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver ?: return null
        val filename = "pic_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
        val file = File(context.filesDir, filename)
        return try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class GalleryViewModelFactory(private val repository: GalleryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
