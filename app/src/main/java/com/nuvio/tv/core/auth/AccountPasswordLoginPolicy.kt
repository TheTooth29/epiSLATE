package com.nuvio.tv.core.auth

/** Approved official-account sign-in for regular epiSLATE and Test; Trial keeps its own flow. */
internal object AccountPasswordLoginPolicy {
    fun available(packageName: String, isCustomServer: Boolean): Boolean =
        (packageName == "com.nuvio.livetv" || packageName == "com.nuvio.livetv.test") && !isCustomServer
    /** Never display raw provider names or response bodies on the sign-in screen. */
    fun errorMessage(error: String): String = when {
        error.contains("invalid", true) || error.contains("credentials", true) ->
            "Email or password wasn't accepted.  Please try again."
        error.contains("network", true) || error.contains("connect", true) || error.contains("timeout", true) ->
            "Unable to connect.  Check your internet connection and try again."
        else -> "Unable to sign in.  Please try again."
    }
}
