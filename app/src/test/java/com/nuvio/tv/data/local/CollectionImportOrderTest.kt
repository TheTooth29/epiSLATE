package com.nuvio.tv.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionImportOrderTest {
    @Test fun savedRowsFollowJsonWithOmittedRowsAfterward() {
        val saved = listOf("continue_watching", "collection_local", "addon_a", "collection_c", "collection_b", "collection_a")
        assertEquals(
            listOf("continue_watching", "collection_a", "addon_a", "collection_b", "collection_c", "collection_local"),
            applyImportedCollectionOrder(saved, listOf("a", "b", "c"))
        )
    }

    @Test fun newImportedRowsAreIncludedWithoutDuplicates() {
        assertEquals(listOf("addon", "collection_a", "collection_b", "collection_c"),
            applyImportedCollectionOrder(listOf("addon", "collection_c", "collection_c"), listOf("a", "b", "c", "a")))
    }

    @Test fun noSavedCollectionsKeepsDefaultPlacement() {
        assertEquals(emptyList<String>(), applyImportedCollectionOrder(emptyList(), listOf("a", "b")))
        assertEquals(listOf("addon"), applyImportedCollectionOrder(listOf("addon"), listOf("a", "b")))
    }

    @Test fun manualReorderAfterImportTakesEffect() {
        val afterImport = applyImportedCollectionOrder(
            listOf("collection_discover", "collection_streaming", "addon"),
            listOf("streaming", "discover")
        )
        assertEquals(listOf("collection_streaming", "collection_discover", "addon"), afterImport)
        val afterManualMove = applyImportedCollectionOrder(afterImport, listOf("discover", "streaming"))
        assertEquals(listOf("collection_discover", "collection_streaming", "addon"), afterManualMove)
    }

    @Test fun repeatedImportIsStable() {
        val first = applyImportedCollectionOrder(listOf("addon", "collection_b", "collection_a"), listOf("a", "b"))
        assertEquals(first, applyImportedCollectionOrder(first, listOf("a", "b")))
    }
}
