package com.nuvio.tv.core.trial

/** A Down request belongs to one heading and cannot replay on another catalog. */
class TrialGridEntryPolicy {
    private var pendingTab: Int? = null
    fun request(tab: Int) { pendingTab = tab }
    fun cancel() { pendingTab = null }
    fun targets(tab: Int): Boolean = pendingTab == tab
    fun completed(tab: Int) { if (targets(tab)) cancel() }
}
