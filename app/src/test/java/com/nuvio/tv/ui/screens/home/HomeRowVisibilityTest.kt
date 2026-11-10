package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.Collection
import com.nuvio.tv.domain.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class HomeRowVisibilityTest {
    private fun loadedCatalog() = CatalogRow(
        addonId = "aiometadata", addonName = "AIOMetaData",
        addonBaseUrl = "https://example.test", catalogId = "movies",
        catalogName = "Movies", type = ContentType.MOVIE,
        items = listOf(com.nuvio.tv.domain.model.MetaPreview(
            id = "tt123", type = ContentType.MOVIE, name = "Test movie",
            poster = null, posterShape = com.nuvio.tv.domain.model.PosterShape.POSTER,
            background = null, logo = null, description = null, releaseInfo = null,
            imdbRating = null, genres = emptyList()
        ))
    )

    @Test
    fun `modern collection folder keeps loaded catalogs while root Home hides them`() {
        val catalog = loadedCatalog()
        val input = ModernHomePresentationInput(
            homeRows = listOf(HomeRow.Catalog(catalog)), catalogRows = listOf(catalog),
            continueWatchingItems = emptyList(), upcomingItems = emptyList(),
            useLandscapePosters = false, showCatalogTypeSuffix = true,
            showFullReleaseDate = true, showImdbRatings = false, localeTag = "en"
        )
        assertEquals(listOf(HomeRow.Catalog(catalog)), resolveVisibleHomeRows(input))
        assertEquals(emptyList<HomeRow>(), resolveVisibleHomeRows(input.copy(hideStandaloneCatalogRows = true)))
        assertEquals(listOf(HomeRow.Catalog(catalog)), resolveVisibleHomeRows(input.copy(homeRows = emptyList())))
        assertEquals(emptyList<HomeRow>(), resolveVisibleHomeRows(input.copy(homeRows = emptyList(), hideStandaloneCatalogRows = true)))
    }

    @Test
    fun `classic collection folder keeps catalogs while root Home hides them`() {
        val catalog = loadedCatalog()
        val folder = HomeUiState(homeRows = listOf(HomeRow.Catalog(catalog)), catalogRows = listOf(catalog))
        assertEquals(folder.homeRows, resolveClassicHomeRows(folder))
        assertEquals(emptyList<HomeRow>(), resolveClassicHomeRows(folder.copy(hideStandaloneCatalogRows = true)))
        assertEquals(folder.homeRows, resolveClassicHomeRows(folder.copy(homeRows = emptyList())))
        assertEquals(emptyList<HomeRow>(), resolveClassicHomeRows(folder.copy(homeRows = emptyList(), hideStandaloneCatalogRows = true)))
    }

    @Test
    fun `addon rows and future catalogs stay hidden while collection order and data survive`() {
        val collections = listOf("DISCOVER", "STREAMING PLATFORMS", "DECADES").map {
            HomeRow.CollectionRow(Collection(id = it, title = it))
        }
        val catalog = CatalogRow(
            addonId = "aiometadata", addonName = "AIOMetaData",
            addonBaseUrl = "https://example.test", catalogId = "movies",
            catalogName = "Movies", type = ContentType.MOVIE, items = emptyList()
        )
        val rows = listOf(
            HomeRow.Catalog(catalog), collections[0],
            HomeRow.Catalog(catalog.copy(addonId = "future-addon", catalogId = "new")),
            collections[1], HomeRow.PlaceholderCatalog(
                "new", "new", "future-addon", "Future", "https://example.test",
                "new", "New", "movie", "New"
            ), collections[2]
        )

        assertEquals(collections, visibleCollectionHomeRows(rows))
        assertSame(collections[0], visibleCollectionHomeRows(rows)[0])
        assertSame(catalog, (rows[0] as HomeRow.Catalog).row)
        assertEquals(6, rows.size)
    }

    @Test
    fun `no collections never falls back to individual catalogs`() {
        assertEquals(emptyList<HomeRow>(), visibleCollectionHomeRows(emptyList()))
    }

    @Test
    fun `app screensaver defaults off`() {
        assertFalse(com.nuvio.tv.domain.model.AmbientModeSettings().enabled)
    }
}
