package com.nuvio.tv.core.trial

import org.junit.Assert.*
import org.junit.Test

class TrialCatalogReturnPolicyTest {
    @Test fun browsingWithoutPlaybackPreservesCatalogFocus() {
        val policy = TrialCatalogReturnPolicy()
        policy.depart(listOf("movie:100"))
        policy.setHome(true)
        assertFalse(policy.update(listOf("movie:100")))
    }
    @Test fun progressChangeWaitsUntilHomeAndIsConsumedOnce() {
        val policy = TrialCatalogReturnPolicy()
        policy.depart(listOf("movie:100"))
        assertFalse(policy.update(listOf("movie:200")))
        policy.setHome(true)
        assertTrue(policy.update(listOf("movie:200")))
        assertFalse(policy.update(listOf("movie:300")))
    }
    @Test fun firstPlaybackAddsContinueWatching() {
        val policy = TrialCatalogReturnPolicy()
        policy.depart(emptyList())
        policy.setHome(true)
        assertTrue(policy.update(listOf("movie:100")))
    }
    @Test fun lateProgressAfterReturningStillRevealsRow() {
        val policy = TrialCatalogReturnPolicy()
        policy.depart(listOf("episode:1"))
        policy.setHome(true)
        assertFalse(policy.update(listOf("episode:1")))
        assertTrue(policy.update(listOf("episode:2")))
    }
    @Test fun emptyRowDoesNotReceiveFocus() {
        val policy = TrialCatalogReturnPolicy()
        policy.depart(listOf("movie:100"))
        policy.setHome(true)
        assertFalse(policy.update(emptyList()))
    }
    @Test fun anotherCatalogVisitReplacesPreviousBaseline() {
        val policy = TrialCatalogReturnPolicy()
        policy.depart(listOf("movie:100"))
        policy.depart(listOf("movie:200"))
        policy.setHome(true)
        assertFalse(policy.update(listOf("movie:200")))
    }
    @Test fun updatesWithoutCatalogVisitDoNotMoveFocus() {
        val policy = TrialCatalogReturnPolicy()
        policy.setHome(true)
        assertFalse(policy.update(listOf("movie:100")))
    }
}
