package com.nuvio.tv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionOrderingTest {
    private fun row(id: String, pinned: Boolean = false) =
        Collection(id = id, title = id, pinToTop = pinned)

    @Test fun homeFollowsSetupAndSubsequentManualMoves() {
        val setup = listOf(row("streaming"), row("discover"), row("decades"), row("genres"))
        assertEquals(setup, collectionsForHome(setup, emptySet()))
        val moved = listOf(setup[2], setup[0], setup[1], setup[3])
        assertEquals(moved, collectionsForHome(moved, emptySet()))
    }

    @Test fun pinnedRowsLeadWithoutReorderingEitherGroup() {
        val setup = listOf(row("a"), row("b", true), row("c"), row("d", true))
        assertEquals(listOf("b", "d", "a", "c"),
            collectionsForHome(setup, emptySet()).map { it.id })
    }

    @Test fun hiddenRowsStayHiddenIncludingPinnedRows() {
        val setup = listOf(row("a"), row("b", true), row("c"))
        assertEquals(listOf("c"), collectionsForHome(setup,
            setOf("collection_a", "collection_b", "unrelated_addon")).map { it.id })
        assertEquals(setup, collectionsForHome(setup, emptySet()).let {
            // Unhiding retains setup-relative order within the pin groups.
            it.sortedBy { item -> setup.indexOf(item) }
        })
    }

    @Test fun jsonOrderReplacesExistingPositionsAndUpdatesContent() {
        val current = listOf(row("discover"), row("streaming"), row("local"))
        val updated = row("streaming").copy(title = "Streaming Platforms")
        val imported = listOf(updated, row("discover"), row("new"))
        val result = mergeCollectionImport(current, imported)
        assertEquals(listOf("streaming", "discover", "new", "local"), result.map { it.id })
        assertEquals("Streaming Platforms", result.first().title)
        assertEquals(result, collectionsForHome(result, emptySet()))
        assertEquals(result, mergeCollectionImport(result, imported))
    }

    @Test fun duplicatesDoNotCreateDuplicateHomeRowsAndLastContentWins() {
        val first = row("a")
        val last = first.copy(title = "Updated")
        assertEquals(listOf(last, row("b")),
            mergeCollectionImport(listOf(row("b")), listOf(first, last)))
        assertEquals(listOf(last), collectionsForHome(listOf(first, last), emptySet()))
        assertEquals(emptyList<Collection>(), collectionsForHome(emptyList(), emptySet()))
    }
}
