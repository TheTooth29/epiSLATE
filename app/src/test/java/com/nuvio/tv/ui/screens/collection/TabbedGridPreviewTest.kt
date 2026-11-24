package com.nuvio.tv.ui.screens.collection

import org.junit.Assert.*
import org.junit.Test

class TabbedGridPreviewTest {
    @Test fun `full and Test packages support layouts without enabling other packages`() {
        assertTrue(TabbedGridPreviewStore.supportsLayouts("com.nuvio.livetv.test"))
        assertTrue(TabbedGridPreviewStore.supportsLayouts("com.nuvio.livetv"))
        listOf("com.nuvio.livetv.debug", "com.nuvio.app", "other.test").forEach {
            assertFalse(TabbedGridPreviewStore.supportsLayouts(it))
        }
    }

    @Test fun `two rows stay still until focus crosses an edge`() {
        val layout = TabbedGridPreview.TWO_ROWS
        assertEquals(0, layout.firstRow(0, 0))
        assertEquals(0, layout.firstRow(1, 0))
        assertEquals(1, layout.firstRow(2, 0))
        assertEquals(2, layout.firstRow(3, 1))
        assertEquals(1, layout.firstRow(1, 2))
    }

    @Test fun `removed three-row preference safely returns to Original`() {
        assertEquals(TabbedGridPreview.ORIGINAL, TabbedGridPreview.fromStored("THREE_ROWS"))
        assertEquals(TabbedGridPreview.TWO_ROWS, TabbedGridPreview.fromStored(null))
        assertEquals(TabbedGridPreview.ORIGINAL, TabbedGridPreview.fromStored("ORIGINAL"))
        assertEquals(TabbedGridPreview.CENTERED, TabbedGridPreview.fromStored("CENTERED"))
        assertEquals(5, TabbedGridPreview.CENTERED.columns)
        assertFalse(TabbedGridPreview.entries.any { it.name == "THREE_ROWS" })
    }

    @Test fun `two rows and pages have identical geometry and preserve margins`() {
        val two = tabbedGridPreviewGeometry(TabbedGridPreview.TWO_ROWS, 960f, 440f, 24f, false)
        assertEquals(two, tabbedGridPreviewGeometry(TabbedGridPreview.PAGES, 960f, 440f, 24f, false))
        assertTrue(6 * two.cardWidth + 5 * 16f + 2 * 48f <= 960.001f)
        assertEquals(440f, 2 * two.rowHeight + 16f + 24f, .001f)
        assertTrue(two.cardWidth * 1.5f + 24f <= two.rowHeight + .001f)
    }

    @Test fun `centered viewport contains exactly one full row with room for arrows`() {
        for (landscape in listOf(false, true)) {
            val geometry = tabbedGridPreviewGeometry(TabbedGridPreview.CENTERED, 960f, 440f, 24f, landscape)
            val widthScale = if (landscape) 1.5f else 1f
            val heightScale = if (landscape) 1.5f / (16f / 9f) else 1.5f
            assertEquals(960f, 5 * geometry.cardWidth * widthScale + 4 * 16f + 2 * 48f, .001f)
            assertEquals(geometry.cardWidth * heightScale + 24f, geometry.rowHeight, .001f)
            assertEquals(geometry.rowHeight + 24f, geometry.gridHeight, .001f)
            assertTrue(geometry.gridHeight + 72f <= 440.001f)
        }
    }

    @Test fun `pages advance and retreat by two complete rows`() {
        val layout = TabbedGridPreview.PAGES
        assertEquals(listOf(0, 0, 2, 2, 4, 4), (0..5).map { layout.firstRow(it, 0) })
        assertEquals(0, layout.firstRow(1, 2))
    }

    @Test fun `centered preview follows selected row in both directions`() {
        assertEquals(8, TabbedGridPreview.CENTERED.firstRow(8, 0))
        assertEquals(2, TabbedGridPreview.CENTERED.firstRow(2, 8))
        assertEquals(0, TabbedGridPreview.CENTERED.firstRow(0, 2))
    }
}
