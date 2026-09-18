package com.pinoriginal.app.net

import com.pinoriginal.app.data.HttpImageInfo
import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request

class ImageProbe(private val client: OkHttpClient) {
    fun probe(url: String): HttpImageInfo? {
        val request = Request.Builder().url(url).head().build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val type = response.header("Content-Type")
                if (type?.lowercase()?.startsWith("image/") == true) {
                    val dimensions = fetchDimensions(url)
                    return HttpImageInfo(
                        url = url,
                        contentType = type.substringBefore(";"),
                        contentLength = response.header("Content-Length")?.toLongOrNull(),
                        width = dimensions?.first,
                        height = dimensions?.second
                    )
                }
            }
        }
        return probeWithGet(url)
    }

    private fun probeWithGet(url: String): HttpImageInfo? {
        val request = Request.Builder().url(url).header("Range", "bytes=0-262143").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val type = response.header("Content-Type") ?: return null
            if (!type.lowercase().startsWith("image/")) return null
            val bytes = response.body?.byteStream()?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var remaining = 262144
                while (remaining > 0) {
                    val read = stream.read(buffer, 0, minOf(buffer.size, remaining))
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
                output.toByteArray()
            } ?: return null
            val dimensions = decodeDimensions(bytes) ?: return null
            val totalLength = response.header("Content-Range")
                ?.substringAfterLast('/')
                ?.toLongOrNull()
                ?: response.header("Content-Length")?.toLongOrNull()
            return HttpImageInfo(url, type.substringBefore(";"), totalLength, dimensions.first, dimensions.second)
        }
    }

    private fun fetchDimensions(url: String): Pair<Int, Int>? {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-262143")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            return decodeDimensions(bytes)
        }
    }

    private fun decodeDimensions(bytes: ByteArray): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return options.outWidth to options.outHeight
    }
}
