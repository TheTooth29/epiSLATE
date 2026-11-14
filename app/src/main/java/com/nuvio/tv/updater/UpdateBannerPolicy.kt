package com.nuvio.tv.updater

internal object UpdateBannerPolicy {
    fun canPresentPrompt(isHome: Boolean, isManual: Boolean, isPlayer: Boolean, startupReady: Boolean): Boolean =
        startupReady && !isPlayer && (isHome || isManual)

    fun shouldShow(
        isRemoteNewer: Boolean,
        force: Boolean,
        bannerEnabled: Boolean,
        dismissedTag: String?,
        updateTag: String
    ): Boolean {
        if (!isRemoteNewer) return false
        if (force) return true
        return bannerEnabled && dismissedTag != updateTag
    }
}
