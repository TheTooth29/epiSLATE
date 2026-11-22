package com.nuvio.tv.core.trial

/** Only a saved detail-navigation intent restores a catalog; a new category visit starts at heading zero. */
object TrialFolderFocusPolicy {
    fun initialTab(detailReturnTab: Int, tabCount: Int): Int =
        detailReturnTab.takeIf { it in 0 until tabCount } ?: 0
}
