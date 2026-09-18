package com.pinoriginal.app.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import okhttp3.OkHttpClient
import okhttp3.Request

class ImageSaver(
    private val context: Context,
    private val client: OkHttpClient
) {
    fun save(url: String, displayName: String): Uri {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
            val contentType = response.header("Content-Type").orEmpty()
            require(contentType.lowercase().startsWith("image/")) { "Server did not return an image." }

            val extension = when {
                "png" in contentType -> "png"
                "webp" in contentType -> "webp"
                else -> "jpg"
            }
            val name = sanitizeName("$displayName.$extension")
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, contentType.substringBefore(";"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PinOriginal")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create Gallery item.")

            try {
                resolver.openOutputStream(uri)?.use { output ->
                    response.body?.byteStream()?.copyTo(output) ?: error("Empty response body.")
                } ?: error("Could not open output file.")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                return uri
            } catch (t: Throwable) {
                resolver.delete(uri, null, null)
                throw t
            }
        }
    }

    private fun sanitizeName(name: String): String {
        return name.replace(Regex("""[^A-Za-z0-9._-]"""), "_").take(80)
    }
}
