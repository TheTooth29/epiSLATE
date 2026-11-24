package com.nuvio.tv.ui.screens.collection

import org.junit.Assert.*
import org.junit.Test

class CatalogFolderReturnPolicyTest {
    @Test fun freshVisitStartsAtFirstHeadingWithoutForcingPosterFocus() {
        val policy = CatalogFolderReturnPolicy()
        assertFalse(policy.resumed())
        assertNull(policy.restoringTab)
        assertEquals(0, CatalogFolderFocusPolicy.initialTab(-1, 4))
        assertTrue(policy.permitsHeading(3))
    }

    @Test fun repeatedDetailsUseTheLatestHeadingInsteadOfTheFirstVisit() {
        val policy = CatalogFolderReturnPolicy()
        policy.openDetail(0)
        assertTrue(policy.resumed())
        policy.completed()
        policy.openDetail(2)
        assertNull(policy.restoringTab)
        assertTrue(policy.resumed())
        assertEquals(2, policy.restoringTab)
        assertFalse(policy.permitsHeading(0))
        assertTrue(policy.permitsHeading(2))
    }

    @Test fun newNavigationDoesNotRestoreBeforeTheFolderResumes() {
        val policy = CatalogFolderReturnPolicy()
        policy.openDetail(1)
        assertNull(policy.restoringTab)
        assertTrue(policy.permitsHeading(0))
        policy.resumed()
        assertEquals(1, policy.restoringTab)
    }

    @Test fun recreationRecoversTheSavedDetailsIntent() {
        val policy = CatalogFolderReturnPolicy(3)
        assertEquals(3, policy.restoringTab)
        assertEquals(3, CatalogFolderFocusPolicy.initialTab(3, 5))
        assertFalse(policy.permitsHeading(0))
    }

    @Test fun completingRestorationAllowsHeadingNavigationAndCannotReplay() {
        val policy = CatalogFolderReturnPolicy(2)
        policy.completed()
        assertTrue(policy.permitsHeading(0))
        assertTrue(policy.permitsHeading(3))
        assertFalse(policy.resumed())
        assertNull(policy.restoringTab)
    }

    @Test fun firstHeadingIsAValidSavedDetailsTarget() {
        val policy = CatalogFolderReturnPolicy(0)
        assertTrue(policy.resumed())
        assertEquals(0, policy.restoringTab)
        assertTrue(policy.permitsHeading(0))
        assertFalse(policy.permitsHeading(1))
    }

    @Test fun invalidSavedHeadingFallsBackWhenCatalogsChange() {
        assertEquals(0, CatalogFolderFocusPolicy.initialTab(7, 3))
        assertEquals(0, CatalogFolderFocusPolicy.initialTab(-1, 3))
        assertEquals(0, CatalogFolderFocusPolicy.initialTab(0, 0))
    }
}
