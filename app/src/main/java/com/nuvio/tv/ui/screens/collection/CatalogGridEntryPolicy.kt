package com.nuvio.tv.ui.screens.collection

/** A Down request belongs to one heading and cannot replay on another catalog. */
class CatalogGridEntryPolicy {
    private var pendingTab: Int? = null
    fun request(tab: Int) { pendingTab = tab }
    fun cancel() { pendingTab = null }
    fun targets(tab: Int): Boolean = pendingTab == tab
    fun completed(tab: Int) { if (targets(tab)) cancel() }
}
