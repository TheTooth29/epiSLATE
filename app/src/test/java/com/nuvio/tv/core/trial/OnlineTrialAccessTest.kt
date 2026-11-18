package com.nuvio.tv.core.trial

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class OnlineTrialAccessTest {
    private var redeemed = false
    private var elapsed = 100L
    private var available = true
    private var status = "active"
    private val start = 1_000_000L
    private val context = mockk<Context>()
    private val prefs = mockk<SharedPreferences>()
    init {
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.getBoolean("redeemed", false) } answers { redeemed }
        val editor = mockk<SharedPreferences.Editor>()
        every { prefs.edit() } returns editor
        every { editor.putBoolean("redeemed", true) } answers { redeemed = true; editor }
        every { editor.commit() } returns true
    }
    private fun access() = OnlineTrialAccess(context, { elapsed }) { _, _ ->
        if (!available) throw java.io.IOException()
        JSONObject().put("activated_at", start / 1000)
            .put("expires_at", (start + TrialPolicy.DURATION_MS) / 1000)
            .put("server_time", start / 1000).put("status", status)
    }
    @Test fun startsOnlyAfterOnlineRedemptionAndRequiresRevalidationAfterRestart() {
        val access = access()
        assertEquals(TrialStatus.NEEDS_CODE, access.snapshot().status)
        assertEquals(RedemptionResult.ACTIVATED, access.redeem("123456"))
        assertEquals(TrialPolicy.DURATION_MS, access.snapshot().remainingMs)
        elapsed += 10_000
        assertEquals(TrialPolicy.DURATION_MS - 10_000, access.snapshot().remainingMs)
        assertEquals(TrialStatus.CONNECTION_REQUIRED, access().snapshot().status)
    }
    @Test fun unavailableNetworkBlocksNewSessionsWithoutReportingExpiration() {
        val access = access(); access.redeem("123456")
        elapsed += 61_000; available = false; access.refresh()
        assertEquals(TrialStatus.CONNECTION_REQUIRED, access.snapshot().status)
    }
    @Test fun serverRevocationDisablesAccess() {
        val access = access(); access.redeem("123456")
        elapsed += 51_000; status = "revoked"; access.refresh()
        assertEquals(TrialStatus.EXPIRED, access.snapshot().status)
    }
    @Test fun malformedCodeDoesNotCallServerOrStartTrial() {
        available = false
        assertEquals(RedemptionResult.INVALID_CODE, access().redeem("123"))
        assertFalse(redeemed)
    }
}
