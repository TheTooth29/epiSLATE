package com.nuvio.tv.core.trial

/** Tracks playback identity/progress only: artwork enrichment must never move focus. */
class TrialCatalogReturnPolicy {
    private var baseline: List<String>? = null
    private var changed = false
    private var home = false
    fun depart(signature: List<String>) { baseline = signature; changed = false; home = false }
    fun setHome(visible: Boolean) { home = visible }
    fun cancel() { baseline = null; changed = false }
    fun update(signature: List<String>): Boolean {
        val original = baseline ?: return false
        changed = signature != original
        if (!home || !changed || signature.isEmpty()) return false
        baseline = null
        return true
    }
}
