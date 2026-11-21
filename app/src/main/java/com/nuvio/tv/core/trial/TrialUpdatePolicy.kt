package com.nuvio.tv.core.trial

/** The approval feed is separate from the regular Beta release list. */
object TrialUpdatePolicy {
    const val PACKAGE = "com.nuvio.livetv.trial"
    const val APK_URL = "https://github.com/TheTooth29/epiSLATE/releases/download/trial-download/epiSLATE-Trial.apk"
    const val FEED_URL = "https://github.com/TheTooth29/epiSLATE/releases/download/trial-download/trial-update.json"
    fun eligible(installedPackage: String, installedVersion: Long, candidatePackage: String,
        candidateVersion: Long, approved: Boolean, url: String, sha256: String): Boolean =
        installedPackage == PACKAGE && candidatePackage == PACKAGE && approved &&
            candidateVersion > installedVersion && url == APK_URL &&
            sha256.matches(Regex("[a-fA-F0-9]{64}"))
}
