package com.nuvio.tv.core.trial

/** Offline invitation codes are retired. Production trial activation uses Cloudflare. */
internal object TrialCodeAllowlist {
    fun accepts(code: String): Boolean = false
}
