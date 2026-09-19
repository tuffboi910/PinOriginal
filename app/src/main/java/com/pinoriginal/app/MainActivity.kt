package com.pinoriginal.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pinoriginal.app.data.ImageSource
import com.pinoriginal.app.ui.PinOriginalViewModel
import com.pinoriginal.app.ui.sourceLabel

private val Ink = Color(0xFF09090B)
private val CardBlack = Color(0xFF151518)
private val SoftWhite = Color(0xFFF7F7F8)
private val Muted = Color(0xFFA1A1AA)
private val PinRed = Color(0xFFE60023)
private val Success = Color(0xFF5EE6A8)

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
    val colors = darkColorScheme(
        primary = PinRed, onPrimary = Color.White, background = Ink,
        surface = CardBlack, onSurface = SoftWhite, error = Color(0xFFFF6B7A)
    )

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF21080E), Ink, Ink), endY = 800f)
                )
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp, 34.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("PinOriginal", color = SoftWhite, fontSize = 32.sp,
                                fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                            Text("The real pixels. Nothing fake.", color = Muted, fontSize = 14.sp)
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBlack.copy(alpha = 0.94f))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = state.url,
                                    onValueChange = viewModel::setUrl,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Paste a Pinterest link", color = Muted) },
                                    leadingIcon = { Text("↗", color = PinRed, fontSize = 20.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF202024),
                                        unfocusedContainerColor = Color(0xFF202024),
                                        focusedBorderColor = PinRed,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = SoftWhite,
                                        unfocusedTextColor = SoftWhite
                                    )
                                )
                                Button(
                                    onClick = viewModel::find,
                                    enabled = !state.loading && state.url.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PinRed)
                                ) {
                                    if (state.loading) {
                                        CircularProgressIndicator(Modifier.size(22.dp), Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("Find the original", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    state.error?.let { message ->
                        item { MessageCard(message, MaterialTheme.colorScheme.error) }
                    }

                    state.result?.let { result ->
                        if (result.videoOnly) {
                            item { MessageCard("This pin contains video, not a downloadable still image.", Muted) }
                        } else {
                            if (result.images.size > 1) {
                                item {
                                    Button(
                                        onClick = viewModel::downloadAll,
                                        enabled = !state.downloading,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp)
                                    ) { Text("Download all ${result.images.size} images") }
                                }
                            }
                            itemsIndexed(result.images) { index, image ->
                                val selected = image.selected
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardBlack)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        image.previewUrl?.let {
                                            AsyncImage(
                                                model = it,
                                                contentDescription = "Pinterest image preview",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxWidth().aspectRatio(0.78f)
                                                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                            )
                                        }
                                        Column(
                                            Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    if (result.images.size > 1) "Image ${index + 1}" else "Best available",
                                                    color = SoftWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold
                                                )
                                                SourceBadge(selected.source == ImageSource.VerifiedOriginalCandidate)
                                            }
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Stat("RESOLUTION", "${selected.width ?: "?"} × ${selected.height ?: "?"}")
                                                Stat("FILE SIZE", selected.contentLength?.let(::formatBytes) ?: "Unknown")
                                            }
                                            Text(sourceLabel(selected.source), color = Muted, fontSize = 13.sp)
                                            Button(
                                                onClick = { viewModel.download(index) },
                                                enabled = !state.downloading,
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = Ink)
                                            ) {
                                                Text(
                                                    if (state.downloading) "Downloading…" else "Download original bytes  ↓",
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    state.savedUri?.let { item { MessageCard("✓  Saved to Pictures/PinOriginal", Success) } }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No upscaling · No recompression · No bullshit",
                            Modifier.fillMaxWidth(), color = Muted.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center, fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = SoftWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SourceBadge(original: Boolean) {
    val color = if (original) Success else Muted
    Text(
        if (original) "✓ ORIGINAL" else "BEST COPY",
        Modifier.background(color.copy(alpha = 0.13f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp
    )
}

@Composable
private fun MessageCard(message: String, color: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))
    ) { Text(message, Modifier.padding(16.dp), color = color, fontSize = 14.sp) }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) "%.1f MB".format(mb) else "${bytes / 1024} KB"
}
