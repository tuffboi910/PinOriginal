package com.pinoriginal.app.net

import com.pinoriginal.app.data.ExtractResult
import com.pinoriginal.app.data.ImageSource
import com.pinoriginal.app.data.PinExtractionResult
import com.pinoriginal.app.data.PinImage
import com.pinoriginal.app.data.PinImageCandidate
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

class PinterestExtractor(
    private val client: OkHttpClient,
    private val imageProbe: ImageProbe = ImageProbe(client)
) {
    fun extract(rawUrl: String): ExtractResult {
        val normalized = PinterestUrl.normalize(rawUrl)
            ?: return ExtractResult.Failure("Paste a valid HTTPS Pinterest link.")
        val canonical = resolveCanonical(normalized)
            ?: return ExtractResult.Failure("Could not resolve this Pinterest link.")
        val pinId = PinterestUrl.extractPinId(canonical)
            ?: return ExtractResult.Failure("Could not find a pin ID in this link.")
        val page = fetchPage(canonical)
            ?: return ExtractResult.Failure("Pinterest did not return a public pin page.")

        val images = PinterestPageParser.parse(page)
        if (images.isEmpty()) {
            return if (page.contains("\"videos\"", ignoreCase = true) || page.contains("video", ignoreCase = true)) {
                ExtractResult.Success(PinExtractionResult(pinId, canonical, emptyList(), videoOnly = true))
            } else {
                ExtractResult.Failure("No downloadable image metadata was found.")
            }
        }

        val pinImages = images.mapIndexedNotNull { index, exposed ->
            val candidates = buildCandidates(exposed).distinctBy { it.url }
            if (candidates.isEmpty()) return@mapIndexedNotNull null
            val best = CandidateRanker.best(candidates)
            PinImage(index, exposed.firstOrNull()?.url, candidates, best)
        }

        return ExtractResult.Success(PinExtractionResult(pinId, canonical, pinImages))
    }

    private fun resolveCanonical(url: String): String? {
        var current = url
        repeat(8) {
            val parsed = current.toHttpUrlOrNull() ?: return null
            if (parsed.scheme != "https") return null
            if (!PinterestUrl.isAllowedPinterestHost(parsed.host)) return null

            val request = Request.Builder()
                .url(current)
                .get()
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                val location = response.header("Location")
                if (response.code in 300..399 && location != null) {
                    val next = parsed.resolve(location)?.toString() ?: return null
                    current = next
                    return@repeat
                }
                return response.request.url.toString()
            }
        }
        return current
    }

    private fun fetchPage(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun buildCandidates(exposed: List<ExposedImage>): List<PinImageCandidate> {
        val official = exposed.mapNotNull { image ->
            imageProbe.probe(image.url)?.let { probe ->
                PinImageCandidate(
                    url = probe.url,
                    width = probe.width ?: image.width,
                    height = probe.height ?: image.height,
                    contentType = probe.contentType,
                    contentLength = probe.contentLength,
                    source = if (probe.url.contains("/originals/")) {
                        ImageSource.VerifiedOriginalCandidate
                    } else {
                        ImageSource.PinterestVariant
                    },
                    verified = true
                )
            }
        }
        val originals = exposed.mapNotNull { deriveOriginalCandidate(it.url) }
            .mapNotNull { candidateUrl ->
                imageProbe.probe(candidateUrl)?.let {
                    PinImageCandidate(
                        url = it.url,
                        width = it.width,
                        height = it.height,
                        contentType = it.contentType,
                        contentLength = it.contentLength,
                        source = ImageSource.VerifiedOriginalCandidate,
                        verified = true
                    )
                }
            }
        return (official + originals).distinctBy { it.url }
    }

    private fun deriveOriginalCandidate(url: String): String? {
        val parsed = url.toHttpUrlOrNull() ?: return null
        if (parsed.host != "i.pinimg.com") return null
        val parts = parsed.encodedPathSegments
        if (parts.size < 4) return null
        val file = parts.last()
        val hashPath = parts.takeLast(4).dropLast(1)
        if (hashPath.any { it.length != 2 } || file.isBlank()) return null
        val builder = parsed.newBuilder()
            .query(null)
            .fragment(null)
        builder.encodedPath("/")
        (listOf("originals") + hashPath + file).forEach { builder.addEncodedPathSegment(it) }
        return builder
            .build()
            .toString()
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 PinOriginal/1.0"
    }
}
