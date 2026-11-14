package com.nuvio.tv.core.trial

import org.junit.Assert.*
import org.junit.Test

class TrialPolicyTest {
    @Test fun trialEndsAtExactly72Hours() {
        val start = 1_000_000L
        val end = start + TrialPolicy.DURATION_MS
        assertEquals(72L * 60 * 60 * 1000, TrialPolicy.remaining(start, end, start))
        assertEquals(1L, TrialPolicy.remaining(start, end, end - 1))
        assertEquals(0L, TrialPolicy.remaining(start, end, end))
        assertEquals(0L, TrialPolicy.remaining(start, end, end + 1))
    }
    @Test fun partialOrExtendedRecordsNeverUnlock() {
        assertFalse(TrialPolicy.validRecord(0, TrialPolicy.DURATION_MS))
        assertFalse(TrialPolicy.validRecord(100, 100 + TrialPolicy.DURATION_MS + 1))
        assertFalse(TrialPolicy.validRecord(Long.MAX_VALUE, Long.MAX_VALUE))
        assertEquals(0L, TrialPolicy.remaining(100, 100 + TrialPolicy.DURATION_MS, 99))
    }
    @Test fun rollbackCannotReverseTimeWithinBoot() {
        assertEquals(1500L, TrialPolicy.effectiveTime(900, 1100, 1000, 700, 200, true))
        assertEquals(2000L, TrialPolicy.effectiveTime(900, 2000, 1000, 700, 200, true))
    }
    @Test fun rebootDoesNotReusePreviousBootElapsedTime() {
        assertEquals(2000L, TrialPolicy.effectiveTime(2000, 1100, 1000, 800, 200, false))
        assertEquals(1100L, TrialPolicy.effectiveTime(900, 1100, 1000, 100, 200, false))
    }
    @Test fun forwardClockJumpCannotExtendTrial() {
        val end = 100 + TrialPolicy.DURATION_MS
        val now = TrialPolicy.effectiveTime(end + 1000, 100, 100, 500, 0, true)
        assertEquals(0L, TrialPolicy.remaining(100, end, now))
    }
    @Test fun codeFormatIsExactlySixAsciiDigits() {
        assertTrue(TrialPolicy.isCodeFormatValid("123456"))
        listOf("", "12345", "1234567", "１２３４５６", " 12345", "12a456").forEach {
            assertFalse(TrialPolicy.isCodeFormatValid(it))
        }
    }
    @Test fun settingsSubpagesShareTheGuard() {
        listOf("settings", "live_tv_settings", "theme_settings", "playback_settings", "account", "collection_editor?collectionId=x").forEach {
            assertTrue(TrialPolicy.isSettingsRoute(it))
        }
        listOf("home", "player/url/title", "live_tv", "recordings", "profile_selection").forEach {
            assertFalse(TrialPolicy.isSettingsRoute(it))
        }
    }
}
