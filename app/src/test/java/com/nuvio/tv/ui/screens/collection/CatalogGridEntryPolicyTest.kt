package com.nuvio.tv.ui.screens.collection

import org.junit.Assert.*
import org.junit.Test

class CatalogGridEntryPolicyTest {
    @Test fun horizontalHeadingMovementDoesNotRequestGridFocus() {
        val entry = CatalogGridEntryPolicy()
        assertFalse(entry.targets(0))
        entry.cancel()
        assertFalse(entry.targets(1))
    }
    @Test fun completedDownCannotReplayWhenAnotherCatalogMounts() {
        val entry = CatalogGridEntryPolicy()
        entry.request(0)
        assertTrue(entry.targets(0))
        entry.completed(0)
        assertFalse(entry.targets(0))
        assertFalse(entry.targets(1))
        assertFalse(entry.targets(2))
    }
    @Test fun headingChangeCancelsDelayedRequestBeforeDataLoads() {
        val entry = CatalogGridEntryPolicy()
        entry.request(1)
        entry.cancel()
        assertFalse(entry.targets(1))
        assertFalse(entry.targets(2))
    }
    @Test fun downTargetsOnlyTheCurrentCatalog() {
        val entry = CatalogGridEntryPolicy()
        entry.request(2)
        assertFalse(entry.targets(0))
        assertFalse(entry.targets(1))
        assertTrue(entry.targets(2))
    }
    @Test fun differentCatalogCannotConsumeThePendingDown() {
        val entry = CatalogGridEntryPolicy()
        entry.request(1)
        entry.completed(0)
        assertTrue(entry.targets(1))
    }
    @Test fun anotherDownWorksAfterReturningToTheHeading() {
        val entry = CatalogGridEntryPolicy()
        entry.request(1)
        entry.completed(1)
        entry.cancel()
        entry.request(3)
        assertTrue(entry.targets(3))
        assertFalse(entry.targets(1))
    }
}
