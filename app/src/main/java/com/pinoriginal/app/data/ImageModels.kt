package com.pinoriginal.app.data

data class PinImageCandidate(
    val url: String,
    val width: Int?,
    val height: Int?,
    val contentType: String? = null,
    val contentLength: Long? = null,
    val source: ImageSource,
    val verified: Boolean = false
) {
    val area: Long = (width ?: 0).toLong() * (height ?: 0).toLong()
}

enum class ImageSource {
    PinterestVariant,
    VerifiedOriginalCandidate
}

data class PinImage(
    val index: Int,
    val previewUrl: String?,
    val candidates: List<PinImageCandidate>,
    val selected: PinImageCandidate
)

data class PinExtractionResult(
    val pinId: String,
    val canonicalUrl: String,
    val images: List<PinImage>,
    val videoOnly: Boolean = false
)

data class HttpImageInfo(
    val url: String,
    val contentType: String,
    val contentLength: Long?,
    val width: Int?,
    val height: Int?
)

sealed class ExtractResult {
    data class Success(val result: PinExtractionResult) : ExtractResult()
    data class Failure(val message: String) : ExtractResult()
}
