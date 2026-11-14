package com.nuvio.tv.core.trial

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.tv.BuildConfig

/** All activation writes and checks are serialized within the app process. */
class TrialAccess internal constructor(
    private val prefs: SharedPreferences,
    private val clock: TrialClock,
    private val acceptsCode: (String) -> Boolean = TrialCodeAllowlist::accepts
) {
    private var sessionAnchor = 0L
    private var sessionElapsed = 0L
    private var lastPersistElapsed = 0L
    private var storageFailed = false

    @Synchronized
    fun snapshot(): TrialSnapshot {
        if (!BuildConfig.TRIAL_ENABLED) return TrialSnapshot(TrialStatus.ACTIVE, Long.MAX_VALUE, Long.MAX_VALUE)
        if (storageFailed) return TrialSnapshot(TrialStatus.EXPIRED)
        return try {
            if (!prefs.contains("started")) {
                // A partial record must never be treated as a new invitation.
                return if (prefs.all.isEmpty()) TrialSnapshot(TrialStatus.NEEDS_CODE) else TrialSnapshot(TrialStatus.EXPIRED)
            }
            val start = prefs.getLong("start", 0)
            val end = prefs.getLong("end", 0)
            if (!prefs.getBoolean("started", false) || !TrialPolicy.validRecord(start, end)) return TrialSnapshot(TrialStatus.EXPIRED)
            val wall = clock.wallTimeMillis()
            val elapsed = clock.elapsedRealtime()
            val boot = clock.bootCount()
            val previous = prefs.getLong("maximum_time", end)
            if (sessionAnchor == 0L) {
                val storedBoot = prefs.getInt("boot", -2)
                sessionAnchor = TrialPolicy.effectiveTime(wall, previous,
                    prefs.getLong("anchor_time", previous), elapsed, prefs.getLong("anchor_elapsed", elapsed),
                    boot >= 0 && boot == storedBoot)
                // A rolled-back wall clock after reboot cannot safely establish remaining time offline.
                if ((boot < 0 || boot != storedBoot) && wall < previous) sessionAnchor = maxOf(sessionAnchor, end)
                sessionElapsed = elapsed
            }
            val now = TrialPolicy.effectiveTime(wall, previous, sessionAnchor, elapsed, sessionElapsed, true)
            sessionAnchor = now
            sessionElapsed = elapsed
            val remaining = TrialPolicy.remaining(start, end, now)
            if (elapsed - lastPersistElapsed >= 30_000 || (remaining == 0L && previous < end)) {
                val saved = prefs.edit().putLong("maximum_time", now).putLong("anchor_time", now)
                    .putLong("anchor_elapsed", elapsed).putInt("boot", boot).commit()
                if (!saved) { storageFailed = true; return TrialSnapshot(TrialStatus.EXPIRED, end) }
                lastPersistElapsed = elapsed
            }
            TrialSnapshot(if (remaining > 0) TrialStatus.ACTIVE else TrialStatus.EXPIRED, end, remaining)
        } catch (_: Exception) {
            TrialSnapshot(TrialStatus.EXPIRED)
        }
    }

    @Synchronized
    fun redeem(rawCode: String): RedemptionResult {
        if (!BuildConfig.TRIAL_ENABLED || snapshot().status != TrialStatus.NEEDS_CODE) return RedemptionResult.ALREADY_STARTED
        val code = rawCode.trim()
        if (!TrialPolicy.isCodeFormatValid(code) || !acceptsCode(code)) return RedemptionResult.INVALID_CODE
        val now = clock.wallTimeMillis()
        if (now <= 0 || now > Long.MAX_VALUE - TrialPolicy.DURATION_MS) return RedemptionResult.STORAGE_ERROR
        val elapsed = clock.elapsedRealtime()
        val boot = clock.bootCount()
        // One atomic persisted record: retries, another code, and process restarts never extend it.
        val saved = prefs.edit().putBoolean("started", true).putLong("start", now)
            .putLong("end", now + TrialPolicy.DURATION_MS).putLong("maximum_time", now)
            .putLong("anchor_time", now).putLong("anchor_elapsed", elapsed).putInt("boot", boot)
            .putString("consumed_code_hash", java.security.MessageDigest.getInstance("SHA-256")
                .digest(code.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) })
            .commit()
        if (!saved) { storageFailed = true; return RedemptionResult.STORAGE_ERROR }
        sessionAnchor = now
        sessionElapsed = elapsed
        lastPersistElapsed = elapsed
        return RedemptionResult.ACTIVATED
    }

    fun canStart(): Boolean = !BuildConfig.TRIAL_ENABLED || snapshot().status == TrialStatus.ACTIVE

    companion object {
        @Volatile private var instance: TrialAccess? = null
        fun get(context: Context): TrialAccess = instance ?: synchronized(this) {
            instance ?: TrialAccess(
                context.applicationContext.getSharedPreferences("epislate_invitation_trial", Context.MODE_PRIVATE),
                AndroidTrialClock(context.applicationContext)
            ).also { instance = it }
        }
    }
}
