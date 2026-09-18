package com.pinoriginal.app.net

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

internal data class ExposedImage(
    val url: String,
    val width: Int? = null,
    val height: Int? = null
)

internal object PinterestPageParser {
    fun parse(html: String): List<List<ExposedImage>> {
        val doc = Jsoup.parse(html)
        val primary = linkedMapOf<String, ExposedImage>()

        doc.select("script[type=application/ld+json]").forEach { script ->
            runCatching { collectSocialPostingImages(JSONObject(script.data().ifBlank { script.html() }), primary) }
            runCatching { collectSocialPostingImages(JSONArray(script.data().ifBlank { script.html() }), primary) }
        }

        val ogUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?.takeIf(::isPinImageUrl)
        if (ogUrl != null) {
            val width = doc.selectFirst("meta[property=og:image:width]")?.attr("content")?.toIntOrNull()
            val height = doc.selectFirst("meta[property=og:image:height]")?.attr("content")?.toIntOrNull()
            primary.putIfAbsent(ogUrl, ExposedImage(ogUrl, width, height))
        }

        if (primary.isNotEmpty()) return listOf(primary.values.toList())

        val groups = mutableListOf<List<ExposedImage>>()
        doc.select("script[type=application/json], script#__PWS_DATA__").forEach { script ->
            runCatching {
                collectImageGroups(JSONObject(script.data().ifBlank { script.html() }), groups)
            }
        }
        return groups
    }

    private fun collectSocialPostingImages(value: Any?, output: MutableMap<String, ExposedImage>) {
        when (value) {
            is JSONObject -> {
                if (value.optString("@type") == "SocialMediaPosting") {
                    collectImageValue(value.opt("image"), output)
                }
                value.keys().forEach { collectSocialPostingImages(value.opt(it), output) }
            }
            is JSONArray -> for (index in 0 until value.length()) {
                collectSocialPostingImages(value.opt(index), output)
            }
        }
    }

    private fun collectImageValue(value: Any?, output: MutableMap<String, ExposedImage>) {
        when (value) {
            is String -> if (isPinImageUrl(value)) output[value] = ExposedImage(value)
            is JSONObject -> {
                val url = value.optString("url").takeIf(::isPinImageUrl)
                    ?: value.optString("contentUrl").takeIf(::isPinImageUrl)
                if (url != null) {
                    output[url] = ExposedImage(url, value.optPositiveInt("width"), value.optPositiveInt("height"))
                }
            }
            is JSONArray -> for (index in 0 until value.length()) collectImageValue(value.opt(index), output)
        }
    }

    private fun collectImageGroups(value: Any?, groups: MutableList<List<ExposedImage>>) {
        when (value) {
            is JSONObject -> {
                val variants = mutableListOf<ExposedImage>()
                value.keys().forEach { key ->
                    val child = value.opt(key)
                    if (key == "images" && child is JSONObject) {
                        child.keys().forEach { variantKey ->
                            child.optJSONObject(variantKey)?.asImage()?.let(variants::add)
                        }
                    } else {
                        collectImageGroups(child, groups)
                    }
                }
                if (variants.isNotEmpty()) groups += variants
            }
            is JSONArray -> for (index in 0 until value.length()) collectImageGroups(value.opt(index), groups)
        }
    }

    private fun JSONObject.asImage(): ExposedImage? {
        val url = optString("url").takeIf(::isPinImageUrl) ?: return null
        return ExposedImage(url, optPositiveInt("width"), optPositiveInt("height"))
    }

    private fun JSONObject.optPositiveInt(name: String): Int? =
        if (has(name)) optInt(name).takeIf { it > 0 } else null

    private fun isPinImageUrl(url: String): Boolean = url.startsWith("https://i.pinimg.com/")
}
