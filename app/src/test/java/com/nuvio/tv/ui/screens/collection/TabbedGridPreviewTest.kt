package com.nuvio.tv.ui.screens.collection

import org.junit.Assert.*
import org.junit.Test

class TabbedGridPreviewTest {
    @Test fun `production and debug packages cannot enable preview`() {
        assertTrue(TabbedGridPreviewStore.isTestPackage("com.nuvio.livetv.test"))
        listOf("com.nuvio.livetv", "com.nuvio.livetv.debug", "com.nuvio.app", "other.test").forEach {
            assertFalse(TabbedGridPreviewStore.isTestPackage(it))
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

    @Test fun `three rows keep all three visible until focus leaves viewport`() {
        val layout = TabbedGridPreview.THREE_ROWS
        assertEquals(0, layout.firstRow(2, 0))
        assertEquals(1, layout.firstRow(3, 0))
        assertEquals(0, layout.firstRow(0, 1))
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
