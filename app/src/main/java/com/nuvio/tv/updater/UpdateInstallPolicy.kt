package com.nuvio.tv.updater

internal object UpdateInstallPolicy {
    // The published feed contains regular full-app releases, never Test APKs.
    fun supportsReleaseUpdates(applicationId: String): Boolean = applicationId == "com.nuvio.livetv"

    fun isSelfUpdate(
        installedPackage: String,
        installedVersionCode: Long,
        candidatePackage: String?,
        candidateVersionCode: Long?
    ): Boolean = candidatePackage == installedPackage &&
        candidateVersionCode != null && candidateVersionCode > installedVersionCode
}
