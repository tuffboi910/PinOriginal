package com.pinoriginal.app.net

import com.pinoriginal.app.data.ImageSource
import com.pinoriginal.app.data.PinImageCandidate

object CandidateRanker {
    fun best(candidates: List<PinImageCandidate>): PinImageCandidate {
        return candidates.maxWith(
            compareBy<PinImageCandidate> { it.area }
                .thenBy { if (it.source == ImageSource.VerifiedOriginalCandidate && it.verified) 1 else 0 }
                .thenBy { qualityScore(it.contentType, it.url) }
                .thenBy { it.contentLength ?: -1L }
        )
    }

    private fun qualityScore(contentType: String?, url: String): Int {
        val joined = "${contentType.orEmpty()} $url".lowercase()
        return when {
            "png" in joined -> 4
            "webp" in joined -> 3
            "jpeg" in joined || "jpg" in joined -> 2
            else -> 1
        }
    }
}
