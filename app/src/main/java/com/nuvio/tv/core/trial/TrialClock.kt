package com.nuvio.tv.core.trial

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

internal interface TrialClock {
    fun wallTimeMillis(): Long
    fun elapsedRealtime(): Long
    fun bootCount(): Int
}

internal class AndroidTrialClock(private val context: Context) : TrialClock {
    override fun wallTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun bootCount(): Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
}
