package com.nuvio.tv.ui.screens.home

/** Tracks playback identity/progress only: artwork enrichment must never move focus. */
class CatalogReturnPolicy {
    private var baseline: List<String>? = null
    private var changed = false
    private var home = false
    private var playback = false
    fun depart(signature: List<String>) { baseline = signature; changed = false; home = false; playback = false }
    fun recordPlayback() { if (baseline != null) playback = true }
    fun setHome(visible: Boolean) { home = visible }
    fun cancel() { baseline = null; changed = false }
    fun update(signature: List<String>): Boolean {
        val original = baseline ?: return false
        changed = signature != original
        if (!home || !playback || !changed || signature.isEmpty()) return false
        baseline = null
        return true
    }
}
