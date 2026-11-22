package com.nuvio.tv.core.trial

import org.junit.Assert.*
import org.junit.Test

class TrialGridEntryPolicyTest {
    @Test fun repeatedDetailVisitsRestoreTheLatestNonFirstHeading() {
        val policy = TrialFolderReturnPolicy()
        assertTrue(policy.permitsHeading(0))
        policy.openDetail(0)
        assertNull(policy.restoringTab)
        assertTrue(policy.resumed())
        policy.completed()
        policy.openDetail(1)
        assertTrue(policy.resumed())
        assertEquals(1, policy.restoringTab)
        assertFalse(policy.permitsHeading(0))
        assertTrue(policy.permitsHeading(1))
        policy.completed()
        assertTrue(policy.permitsHeading(0))
        assertFalse(policy.resumed())
    }
    @Test fun recreatedFolderRestoresSavedNonFirstTab() {
        val policy = TrialFolderReturnPolicy(2)
        assertEquals(2, policy.restoringTab)
        assertFalse(policy.permitsHeading(0))
        assertTrue(policy.permitsHeading(2))
    }

    @Test fun detailReturnRetainsItsHeadingButNewCategoryStartsAtFirst() {
        assertEquals(2, TrialFolderFocusPolicy.initialTab(2, 4))
        assertEquals(0, TrialFolderFocusPolicy.initialTab(-1, 4))
        assertEquals(0, TrialFolderFocusPolicy.initialTab(4, 4))
        assertEquals(0, TrialFolderFocusPolicy.initialTab(0, 0))
    }

    @Test fun horizontalHeadingMovementDoesNotRequestGridFocus() {
        val entry = TrialGridEntryPolicy()
        assertFalse(entry.targets(0))
        entry.cancel()
        assertFalse(entry.targets(1))
    }
    @Test fun completedDownCannotReplayWhenAnotherCatalogMounts() {
        val entry = TrialGridEntryPolicy()
        entry.request(0)
        assertTrue(entry.targets(0))
        entry.completed(0)
        assertFalse(entry.targets(0))
        assertFalse(entry.targets(1))
        assertFalse(entry.targets(2))
    }
    @Test fun headingChangeCancelsDelayedRequestBeforeDataLoads() {
        val entry = TrialGridEntryPolicy()
        entry.request(1)
        entry.cancel()
        assertFalse(entry.targets(1))
        assertFalse(entry.targets(2))
    }
    @Test fun downTargetsOnlyTheCurrentCatalog() {
        val entry = TrialGridEntryPolicy()
        entry.request(2)
        assertFalse(entry.targets(0))
        assertFalse(entry.targets(1))
        assertTrue(entry.targets(2))
    }
    @Test fun differentCatalogCannotConsumeThePendingDown() {
        val entry = TrialGridEntryPolicy()
        entry.request(1)
        entry.completed(0)
        assertTrue(entry.targets(1))
    }
    @Test fun anotherDownWorksAfterReturningToTheHeading() {
        val entry = TrialGridEntryPolicy()
        entry.request(1)
        entry.completed(1)
        entry.cancel()
        entry.request(3)
        assertTrue(entry.targets(3))
        assertFalse(entry.targets(1))
    }
}
