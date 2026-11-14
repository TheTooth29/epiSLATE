package com.nuvio.tv.core.trial

/** Local preview policy. This does not claim server-backed or reinstall-proof licensing. */
object TrialPolicy {
    const val DURATION_MS = 72L * 60 * 60 * 1000
    fun isCodeFormatValid(code: String): Boolean = code.length == 6 && code.all { it in '0'..'9' }
    fun validRecord(start: Long, end: Long): Boolean =
        start > 0 && start <= Long.MAX_VALUE - DURATION_MS && end == start + DURATION_MS

    fun effectiveTime(wall: Long, previous: Long, anchor: Long, elapsed: Long, anchorElapsed: Long, sameBoot: Boolean): Long {
        val delta = if (sameBoot && elapsed >= anchorElapsed) elapsed - anchorElapsed else 0L
        val monotonic = if (anchor <= Long.MAX_VALUE - delta) anchor + delta else Long.MAX_VALUE
        return maxOf(wall, previous, monotonic)
    }

    fun remaining(start: Long, end: Long, now: Long): Long =
        if (validRecord(start, end) && now >= start && now < end) end - now else 0L

    fun isSettingsRoute(route: String): Boolean = route.substringBefore('?').substringBefore('/') in setOf(
        "settings", "live_tv_settings", "trakt", "tmdb_settings", "theme_settings", "playback_settings",
        "about", "supporters_contributors", "licenses_attributions", "addon_manager", "catalog_order",
        "plugins", "layout_settings", "account", "manage_profiles", "auth_sign_in", "auth_qr_sign_in",
        "sync_code_generate", "sync_code_claim", "collections", "collection_editor"
    )
}

enum class TrialStatus { NEEDS_CODE, ACTIVE, EXPIRED }
data class TrialSnapshot(val status: TrialStatus, val expiresAt: Long = 0, val remainingMs: Long = 0)
enum class RedemptionResult { ACTIVATED, INVALID_CODE, ALREADY_STARTED, STORAGE_ERROR }
