package com.nuvio.tv.core.auth

/** Alternate official-server login experiment, unavailable in regular and Trial apps. */
internal object TestPasswordLoginPolicy {
    fun available(packageName: String, isCustomServer: Boolean): Boolean =
        packageName == "com.nuvio.livetv.test" && !isCustomServer
}
