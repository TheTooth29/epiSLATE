package com.nuvio.tv.core.trial

import android.content.SharedPreferences
import com.nuvio.tv.BuildConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TrialAccessTest {
    private class Clock : TrialClock {
        var wall = 1_000_000L
        var elapsed = 100L
        var boot = 1
        override fun wallTimeMillis() = wall
        override fun elapsedRealtime() = elapsed
        override fun bootCount() = boot
        fun advance(ms: Long) { wall += ms; elapsed += ms }
    }
    private val clock = Clock()
    private val data = mutableMapOf<String, Any>()
    private var saveWorks = true
    private val prefs = mockk<SharedPreferences>()
    private fun store() = TrialAccess(prefs, clock) { it == "123456" || it == "654321" }

    @Before fun prepare() {
        assumeTrue("Persistence tests exercise the explicitly enabled trial build", BuildConfig.TRIAL_ENABLED)
        every { prefs.all } answers { data.toMap() }
        every { prefs.contains(any()) } answers { data.containsKey(firstArg<String>()) }
        every { prefs.getLong(any(), any()) } answers { data[firstArg<String>()] as? Long ?: secondArg<Long>() }
        every { prefs.getBoolean(any(), any()) } answers { data[firstArg<String>()] as? Boolean ?: secondArg<Boolean>() }
        every { prefs.getInt(any(), any()) } answers { data[firstArg<String>()] as? Int ?: secondArg<Int>() }
        every { prefs.edit() } answers {
            val editor = mockk<SharedPreferences.Editor>()
            val pending = mutableMapOf<String, Any>()
            every { editor.putLong(any(), any()) } answers { pending[firstArg()] = secondArg<Long>(); editor }
            every { editor.putInt(any(), any()) } answers { pending[firstArg()] = secondArg<Int>(); editor }
            every { editor.putBoolean(any(), any()) } answers { pending[firstArg()] = secondArg<Boolean>(); editor }
            every { editor.putString(any(), any()) } answers { pending[firstArg()] = secondArg<String>(); editor }
            // Android may update in-memory preferences even when the disk commit fails.
            every { editor.commit() } answers { data.putAll(pending); saveWorks }
            editor
        }
    }

    @Test fun invalidCodeDoesNotStartTimer() {
        val access = store()
        assertEquals(TrialStatus.NEEDS_CODE, access.snapshot().status)
        assertEquals(RedemptionResult.INVALID_CODE, access.redeem("000000"))
        assertTrue(data.isEmpty())
    }
    @Test fun activationAndRestartKeepOriginalDeadline() {
        val access = store()
        assertEquals(RedemptionResult.ACTIVATED, access.redeem("123456"))
        val end = access.snapshot().expiresAt
        clock.advance(40_000)
        assertEquals(end, store().snapshot().expiresAt)
        assertEquals(TrialPolicy.DURATION_MS - 40_000, store().snapshot().remainingMs)
        assertEquals(RedemptionResult.ALREADY_STARTED, access.redeem("654321"))
        assertEquals(end, access.snapshot().expiresAt)
    }
    @Test fun elapsedTimeExpiresTrialDespiteWallClockRollback() {
        val access = store()
        access.redeem("123456")
        clock.elapsed += TrialPolicy.DURATION_MS
        clock.wall -= 1000
        assertEquals(TrialStatus.EXPIRED, access.snapshot().status)
        assertEquals(RedemptionResult.ALREADY_STARTED, access.redeem("654321"))
    }
    @Test fun sameBootRestartUsesPersistedMonotonicAnchor() {
        store().redeem("123456")
        clock.elapsed += 60_000
        clock.wall -= 100
        assertEquals(TrialPolicy.DURATION_MS - 60_000, store().snapshot().remainingMs)
    }
    @Test fun rebootWithRolledBackClockDoesNotGrantFreshTime() {
        val access = store()
        access.redeem("123456")
        clock.advance(40_000)
        access.snapshot()
        clock.boot++
        clock.elapsed = 10
        clock.wall -= 50_000
        assertEquals(TrialStatus.EXPIRED, store().snapshot().status)
    }
    @Test fun partialOrCorruptRecordFailsClosed() {
        data["started"] = true
        data["start"] = 1_000_000L
        assertEquals(TrialStatus.EXPIRED, store().snapshot().status)
        assertEquals(RedemptionResult.ALREADY_STARTED, store().redeem("123456"))
    }
    @Test fun failedDiskCommitDoesNotUnlockFromMemory() {
        val access = store()
        saveWorks = false
        assertEquals(RedemptionResult.STORAGE_ERROR, access.redeem("123456"))
        assertEquals(TrialStatus.EXPIRED, access.snapshot().status)
    }
    @Test fun concurrentRedemptionsHaveOnlyOneWinner() {
        val access = store()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results = (1..4).map { pool.submit<RedemptionResult> { start.await(); access.redeem("123456") } }
            start.countDown()
            assertEquals(1, results.map { it.get(10, TimeUnit.SECONDS) }.count { it == RedemptionResult.ACTIVATED })
        } finally { pool.shutdownNow() }
    }
}
