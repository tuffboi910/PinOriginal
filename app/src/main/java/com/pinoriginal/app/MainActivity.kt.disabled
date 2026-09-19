package com.pinoriginal.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pinoriginal.app.ui.PinOriginalViewModel
import com.pinoriginal.app.ui.sourceLabel

class MainActivity : ComponentActivity() {
    private val viewModel: PinOriginalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        setContent { PinOriginalApp(viewModel) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                viewModel.setUrl(it)
                viewModel.find()
            }
        }
    }
}

@Composable
private fun PinOriginalApp(viewModel: PinOriginalViewModel) {
    val state by viewModel.state.collectAsState()
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("PinOriginal", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Paste Pinterest link") },
                    singleLine = true
                )
                Button(onClick = viewModel::find, enabled = !state.loading && state.url.isNotBlank()) {
                    Text(if (state.loading) "Finding..." else "Find Original")
                }
                if (state.loading || state.downloading) CircularProgressIndicator()
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.result?.let { result ->
                    if (result.videoOnly) {
                        Text("This pin contains video, not a downloadable still image.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (result.images.size > 1) {
                                item {
                                    Button(onClick = viewModel::downloadAll, enabled = !state.downloading) {
                                        Text("Download all")
                                    }
                                }
                            }
                            itemsIndexed(result.images) { index, image ->
                                val selected = image.selected
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (result.images.size > 1) "Image ${index + 1}" else "Preview")
                                    image.previewUrl?.let {
                                        AsyncImage(
                                            model = it,
                                            contentDescription = "Pinterest image preview",
                                            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f)
                                        )
                                    }
                                    Text("Resolution: ${selected.width ?: "?"} x ${selected.height ?: "?"}")
                                    Text("File size: ${selected.contentLength?.let(::formatBytes) ?: "unknown"}")
                                    Text("Source: ${sourceLabel(selected.source)}")
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Button(onClick = { viewModel.download(index) }, enabled = !state.downloading) {
                                            Text("Download")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                state.savedUri?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Saved to Gallery: Pictures/PinOriginal")
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) "%.1f MB".format(mb) else "${bytes / 1024} KB"
}
