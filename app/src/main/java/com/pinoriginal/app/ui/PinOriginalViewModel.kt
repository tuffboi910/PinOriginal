package com.pinoriginal.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pinoriginal.app.data.ExtractResult
import com.pinoriginal.app.data.ImageSource
import com.pinoriginal.app.data.PinExtractionResult
import com.pinoriginal.app.net.PinterestExtractor
import com.pinoriginal.app.storage.ImageSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PinOriginalViewModel(app: Application) : AndroidViewModel(app) {
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val extractor = PinterestExtractor(client)
    private val saver = ImageSaver(app, client)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun setUrl(url: String) {
        _state.value = _state.value.copy(url = url)
    }

    fun find() {
        val url = _state.value.url
        _state.value = _state.value.copy(loading = true, error = null, result = null, savedUri = null)
        viewModelScope.launch(Dispatchers.IO) {
            when (val extracted = extractor.extract(url)) {
                is ExtractResult.Success -> _state.value = _state.value.copy(loading = false, result = extracted.result)
                is ExtractResult.Failure -> _state.value = _state.value.copy(loading = false, error = extracted.message)
            }
        }
    }

    fun download(imageIndex: Int = 0) {
        val result = _state.value.result ?: return
        val image = result.images.getOrNull(imageIndex) ?: return
        _state.value = _state.value.copy(downloading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                saver.save(image.selected.url, "pinoriginal_${result.pinId}_${image.index}")
            }.onSuccess { uri ->
                _state.value = _state.value.copy(downloading = false, savedUri = uri.toString())
            }.onFailure {
                _state.value = _state.value.copy(downloading = false, error = it.message ?: "Download failed.")
            }
        }
    }

    fun downloadAll() {
        val result = _state.value.result ?: return
        _state.value = _state.value.copy(downloading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                result.images.forEach { image ->
                    saver.save(image.selected.url, "pinoriginal_${result.pinId}_${image.index}")
                }
            }.onSuccess {
                _state.value = _state.value.copy(downloading = false, savedUri = "Pictures/PinOriginal")
            }.onFailure {
                _state.value = _state.value.copy(downloading = false, error = it.message ?: "Download failed.")
            }
        }
    }
}

data class UiState(
    val url: String = "",
    val loading: Boolean = false,
    val downloading: Boolean = false,
    val result: PinExtractionResult? = null,
    val error: String? = null,
    val savedUri: String? = null
)

fun sourceLabel(source: ImageSource): String = when (source) {
    ImageSource.VerifiedOriginalCandidate -> "VERIFIED ORIGINAL"
    ImageSource.PinterestVariant -> "HIGHEST PINTEREST COPY"
}
