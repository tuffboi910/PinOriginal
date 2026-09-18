package com.pinoriginal.app.net

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object PinterestUrl {
    private val pinRegex = Regex("""/(?:pin|pin/[^/]+)/(\d{6,})""")
    private val numericSegmentRegex = Regex("""/(\d{6,})(?:/|$)""")

    fun isAllowedPinterestHost(host: String): Boolean {
        val clean = host.lowercase()
        return clean == "pin.it" ||
            clean == "pinterest.com" ||
            clean.endsWith(".pinterest.com") ||
            clean.endsWith(".pinterest.co.uk") ||
            clean.endsWith(".pinterest.ca") ||
            clean.endsWith(".pinterest.fr") ||
            clean.endsWith(".pinterest.de") ||
            clean.endsWith(".pinterest.es") ||
            clean.endsWith(".pinterest.it") ||
            clean.endsWith(".pinterest.com.au")
    }

    fun normalize(input: String): String? {
        val trimmed = input.trim()
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        if (parsed.scheme != "https") return null
        if (!isAllowedPinterestHost(parsed.host)) return null
        return parsed.newBuilder().fragment(null).build().toString()
    }

    fun extractPinId(url: String): String? {
        val parsed = url.toHttpUrlOrNull() ?: return null
        val path = parsed.encodedPath
        return pinRegex.find(path)?.groupValues?.get(1)
            ?: numericSegmentRegex.find(path)?.groupValues?.get(1)
    }
}
