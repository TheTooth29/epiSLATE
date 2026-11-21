package com.nuvio.tv.core.auth

/** Alternate official-server login experiment, unavailable in regular and Trial apps. */
internal object TestPasswordLoginPolicy {
    fun available(packageName: String, isCustomServer: Boolean): Boolean =
        packageName == "com.nuvio.livetv.test" && !isCustomServer
    /** Never display raw provider names or response bodies on the Test sign-in screen. */
    fun errorMessage(error: String): String = when {
        error.contains("invalid", true) || error.contains("credentials", true) ->
            "Email or password wasn't accepted.  Please try again."
        error.contains("network", true) || error.contains("connect", true) || error.contains("timeout", true) ->
            "Unable to connect.  Check your internet connection and try again."
        else -> "Unable to sign in.  Please try again."
    }
}
