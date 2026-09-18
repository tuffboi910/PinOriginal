package com.pinoriginal.app.net

import com.pinoriginal.app.data.ExtractResult
import com.pinoriginal.app.data.ImageSource
import com.pinoriginal.app.data.PinExtractionResult
import com.pinoriginal.app.data.PinImage
import com.pinoriginal.app.data.PinImageCandidate
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup

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

        val images = parseImages(page)
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

    private fun parseImages(html: String): List<List<RawImage>> {
        val doc = Jsoup.parse(html)
        val scriptJson = doc.select("script[type=application/json], script#__PWS_DATA__").map { it.data().ifBlank { it.html() } }
        val groups = mutableListOf<List<RawImage>>()
        for (jsonText in scriptJson) {
            runCatching {
                val root = JSONObject(jsonText)
                collectImageGroups(root, groups)
            }
        }
        if (groups.isNotEmpty()) return groups

        val metaImages = doc.select("meta[property=og:image], meta[name=twitter:image]")
            .mapNotNull { it.attr("content").takeIf(String::isNotBlank) }
            .map { listOf(RawImage(it, null, null)) }
        return metaImages
    }

    private fun collectImageGroups(value: Any?, groups: MutableList<List<RawImage>>) {
        when (value) {
            is JSONObject -> {
                val variants = mutableListOf<RawImage>()
                for (key in value.keys()) {
                    val child = value.opt(key)
                    if (key == "images" && child is JSONObject) {
                        child.keys().forEach { variantKey ->
                            val variant = child.optJSONObject(variantKey)
                            val url = variant?.optString("url")?.takeIf { it.startsWith("https://i.pinimg.com/") }
                            if (url != null) variants += RawImage(url, variant.optIntOrNull("width"), variant.optIntOrNull("height"))
                        }
                    } else {
                        collectImageGroups(child, groups)
                    }
                }
                if (variants.isNotEmpty()) groups += variants
            }
            is org.json.JSONArray -> {
                for (i in 0 until value.length()) collectImageGroups(value.opt(i), groups)
            }
        }
    }

    private fun buildCandidates(exposed: List<RawImage>): List<PinImageCandidate> {
        val official = exposed.map {
            PinImageCandidate(it.url, it.width, it.height, source = ImageSource.PinterestVariant, verified = true)
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
        return official + originals
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

    private data class RawImage(val url: String, val width: Int?, val height: Int?)

    private fun JSONObject.optIntOrNull(name: String): Int? = if (has(name)) optInt(name).takeIf { it > 0 } else null

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 PinOriginal/1.0"
    }
}
