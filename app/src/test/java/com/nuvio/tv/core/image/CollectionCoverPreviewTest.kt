package com.nuvio.tv.core.image

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionCoverPreviewTest {
    private val base = "https://raw.githubusercontent.com/TheTooth29/epiSLATE-assets/main/collections/"

    @Test fun knownGenrePngUsesSmallPreview() {
        assertEquals(base + "genres/cover/action.jpg",
            collectionCoverPreviewUrl(base + "genres/cover/action.png"))
        assertEquals(base + "genres/cover/scifi.jpg",
            collectionCoverPreviewUrl(base + "genres/cover/scifi.png"))
    }

    @Test fun decadePngUsesWebpAndExistingPreviewsStayUnchanged() {
        assertEquals(base + "decades/cover/1950s.webp",
            collectionCoverPreviewUrl(base + "decades/cover/1950s.png"))
        for (path in listOf("decades/cover/2020s.webp", "genres/cover/comedy.jpg")) {
            assertEquals(base + path, collectionCoverPreviewUrl(base + path))
        }
    }

    @Test fun customUnknownAndVersionedUrlsArePreserved() {
        for (url in listOf("https://example.com/action.png",
            base + "genres/cover/custom.png", base + "decades/cover/1940s.png",
            base + "genres/cover/action.png?version=2",
            base + "discover/action.png")) {
            assertEquals(url, collectionCoverPreviewUrl(url))
        }
    }

    @Test fun prefetchAndDisplayShareInitialKeyAndRevalidationStaysDistinct() {
        val url = collectionCoverPreviewUrl(base + "genres/cover/action.png")
        assertEquals(collectionCoverCacheKey(url, 400, 225),
            collectionCoverCacheKey(url, 400, 225, 0))
        assertEquals("${url}_400x225_v1", collectionCoverCacheKey(url, 400, 225, 1))
    }
}
