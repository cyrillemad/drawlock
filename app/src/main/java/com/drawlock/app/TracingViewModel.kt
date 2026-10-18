package com.drawlock.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val KEY_URI = "image_uri"

class TracingViewModel(
    application: Application,
    private val savedState: SavedStateHandle,
) : AndroidViewModel(application) {

    var image by mutableStateOf<ImageBitmap?>(null)
        private set
    var loadFailed by mutableStateOf(false)
        private set

    var transform by mutableStateOf(ImageTransform())
    var mirrored by mutableStateOf(false)
    var grayscale by mutableStateOf(false)
    var contrast by mutableFloatStateOf(1.5f)
    var locked by mutableStateOf(false)

    init {
        // After process death only the URI survives; decode it again.
        savedState.get<Uri>(KEY_URI)?.let(::load)
    }

    fun pickImage(uri: Uri) {
        // Keeps the picked image readable if the process is killed and restored.
        runCatching {
            getApplication<Application>().contentResolver
                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        savedState[KEY_URI] = uri
        transform = ImageTransform()
        load(uri)
    }

    fun reset() {
        transform = ImageTransform()
        mirrored = false
    }

    private fun load(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { decodeScaledImage(resolver, uri) }
            }
            image = result.getOrNull()
            loadFailed = result.isFailure
        }
    }
}
