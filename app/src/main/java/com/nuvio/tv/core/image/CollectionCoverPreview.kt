package com.nuvio.tv.core.image

private const val COLLECTION_COVER_BASE =
    "https://raw.githubusercontent.com/TheTooth29/epiSLATE-assets/main/collections/"

private val genreCoverNames = setOf(
    "action", "adventure", "animation", "comedy", "crime", "documentary",
    "drama", "family", "fantasy", "history", "horror", "music", "mystery",
    "romance", "scifi", "thriller", "war", "western"
)
private val decadeCoverNames = setOf(
    "1950s", "1960s", "1970s", "1980s", "1990s", "2000s", "2010s", "2020s"
)

/** Only substitute preview files known to exist in epiSLATE-assets. */
internal fun collectionCoverPreviewUrl(url: String): String {
    val path = url.removePrefix(COLLECTION_COVER_BASE)
    if (path == url) return url
    val genre = path.removePrefix("genres/cover/").removeSuffix(".png")
    if (path == "genres/cover/$genre.png" && genre in genreCoverNames) {
        return "${COLLECTION_COVER_BASE}genres/cover/$genre.jpg"
    }
    val decade = path.removePrefix("decades/cover/").removeSuffix(".png")
    if (path == "decades/cover/$decade.png" && decade in decadeCoverNames) {
        return "${COLLECTION_COVER_BASE}decades/cover/$decade.webp"
    }
    return url
}

internal fun collectionCoverCacheKey(url: String, width: Int, height: Int, version: Int = 0): String =
    "${url}_${width}x${height}_v$version"
