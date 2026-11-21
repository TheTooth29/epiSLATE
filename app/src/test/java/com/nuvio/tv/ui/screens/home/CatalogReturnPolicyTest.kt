package com.nuvio.tv.ui.screens.home

import org.junit.Assert.*
import org.junit.Test

class CatalogReturnPolicyTest {
    @Test fun browsingWithoutPlaybackPreservesCatalogFocus() {
        val policy = CatalogReturnPolicy()
        policy.depart(listOf("movie:100"))
        policy.setHome(true)
        assertFalse(policy.update(listOf("movie:100")))
    }
    @Test fun progressChangeWaitsUntilHomeAndIsConsumedOnce() {
        val policy = CatalogReturnPolicy()
        policy.depart(listOf("movie:100"))
        policy.recordPlayback()
        assertFalse(policy.update(listOf("movie:200")))
        policy.setHome(true)
        assertTrue(policy.update(listOf("movie:200")))
        assertFalse(policy.update(listOf("movie:300")))
    }
    @Test fun firstPlaybackAddsContinueWatching() {
        val policy = CatalogReturnPolicy()
        policy.depart(emptyList())
        policy.recordPlayback()
        policy.setHome(true)
        assertTrue(policy.update(listOf("movie:100")))
    }
    @Test fun lateProgressAfterReturningStillRevealsRow() {
        val policy = CatalogReturnPolicy()
        policy.depart(listOf("episode:1"))
        policy.recordPlayback()
        policy.setHome(true)
        assertFalse(policy.update(listOf("episode:1")))
        assertTrue(policy.update(listOf("episode:2")))
    }
    @Test fun emptyRowDoesNotReceiveFocus() {
        val policy = CatalogReturnPolicy()
        policy.depart(listOf("movie:100"))
        policy.recordPlayback()
        policy.setHome(true)
        assertFalse(policy.update(emptyList()))
    }
    @Test fun anotherCatalogVisitReplacesPreviousBaseline() {
        val policy = CatalogReturnPolicy()
        policy.depart(listOf("movie:100"))
        policy.depart(listOf("movie:200"))
        policy.setHome(true)
        assertFalse(policy.update(listOf("movie:200")))
    }
    @Test fun updatesWithoutCatalogVisitDoNotMoveFocus() {
        val policy = CatalogReturnPolicy()
        policy.setHome(true)
        assertFalse(policy.update(listOf("movie:100")))
    }
    @Test fun catalogRefreshWithoutPlaybackDoesNotStealFocus() {
        val policy = CatalogReturnPolicy()
        policy.depart(listOf("episode:1"))
        policy.setHome(true)
        assertFalse(policy.update(listOf("episode:2")))
    }
    @Test fun leavingCatalogFlowCancelsPendingPlaybackReturn() {
        val policy = CatalogReturnPolicy()
        policy.depart(listOf("episode:1"))
        policy.recordPlayback()
        policy.cancel()
        policy.setHome(true)
        assertFalse(policy.update(listOf("episode:2")))
    }
}
