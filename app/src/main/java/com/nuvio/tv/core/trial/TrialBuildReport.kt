package com.nuvio.tv.core.trial

import com.nuvio.tv.BuildConfig
import org.json.JSONObject

/** A diagnostic self-report from this APK, never an integrity/attestation claim. */
internal data class TrialBuildReport(val versionCode: Int, val versionName: String, val buildNumber: Int = 0) {
    fun proofSuffix(): String = "\nbuild:v1:$versionCode:$versionName:${if (buildNumber > 0) buildNumber else ""}"
    fun attach(body: JSONObject): JSONObject = body.put("proof_version", 2).put("app_build",
        JSONObject().put("version_code", versionCode).put("version_name", versionName).apply {
            if (buildNumber > 0) put("build_number", buildNumber)
        })

    companion object {
        fun current() = TrialBuildReport(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, BuildConfig.TRIAL_BUILD_NUMBER)
        fun negotiate(challenge: JSONObject, installed: TrialBuildReport): TrialBuildReport? =
            installed.takeIf { challenge.optInt("build_metadata_version") == 1 }
        fun message(challenge: String, action: String, code: String, build: TrialBuildReport?): String =
            challenge + "\n" + (if (action == "redeem") code else "") + (build?.proofSuffix() ?: "")
    }
}
