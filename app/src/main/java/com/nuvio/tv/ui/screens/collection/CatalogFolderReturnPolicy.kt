package com.nuvio.tv.ui.screens.collection

/** Only a saved detail-navigation intent restores a catalog; a new category visit starts at heading zero. */
object CatalogFolderFocusPolicy {
    fun initialTab(detailReturnTab: Int, tabCount: Int): Int =
        detailReturnTab.takeIf { it in 0 until tabCount } ?: 0
}

/** Survives repeated detail visits within the same retained folder composition. */
class CatalogFolderReturnPolicy(savedTab: Int = -1) {
    private var pendingTab = savedTab.takeIf { it >= 0 }
    var restoringTab: Int? = pendingTab
        private set
    fun openDetail(tab: Int) { pendingTab = tab; restoringTab = null }
    fun resumed(): Boolean {
        restoringTab = pendingTab
        return restoringTab != null
    }
    fun permitsHeading(tab: Int): Boolean = restoringTab == null || restoringTab == tab
    fun completed() { pendingTab = null; restoringTab = null }
}
