package com.nuvio.tv.ui.screens.home

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesBadgeRefreshTest {
    @Test
    fun `synced episode replacements are processed even when history count stays the same`() = runTest {
        val refreshed = mutableListOf<Set<Pair<Int, Int>>>()
        val first = setOf(1 to 1)
        val replacement = setOf(1 to 2)
        flowOf(first, replacement).collectSeriesBadgeRefreshes {
            refreshed.add(it)
            false
        }
        assertEquals(listOf(first, replacement), refreshed)
    }

    @Test
    fun `failed startup lookup retries without any history change`() = runTest {
        val watched = setOf("series-a")
        var attempts = 0
        val badges = mutableSetOf<String>()
        val job = launch {
            flowOf(watched).collectSeriesBadgeRefreshes(listOf(5_000)) { ids ->
                attempts++
                if (attempts == 1) true else {
                    badges.addAll(ids)
                    false
                }
            }
        }
        runCurrent()
        assertEquals(1, attempts)
        assertTrue(badges.isEmpty())
        advanceTimeBy(5_000)
        runCurrent()
        job.join()
        assertEquals(watched, badges)
        assertEquals(2, attempts)
    }

    @Test
    fun `cancelled lookup is replaced even when the series ids are unchanged`() = runTest {
        val history = MutableSharedFlow<Pair<String, Set<String>>>()
        val firstStarted = CompletableDeferred<Unit>()
        var firstCancelled = false
        val badges = mutableSetOf<String>()
        val job = launch {
            history.collectSeriesBadgeRefreshes { (profile, ids) ->
                if (profile == "old") {
                    firstStarted.complete(Unit)
                    try { awaitCancellation() } finally { firstCancelled = true }
                }
                badges.addAll(ids)
                false
            }
        }
        runCurrent()
        history.emit("old" to setOf("series-a"))
        firstStarted.await()
        history.emit("new" to setOf("series-a"))
        runCurrent()
        assertTrue(firstCancelled)
        assertEquals(setOf("series-a"), badges)
        job.cancelAndJoin()
    }

    @Test
    fun `successful resolution does not schedule more requests`() = runTest {
        var attempts = 0
        flowOf(setOf("series-a")).collectSeriesBadgeRefreshes {
            attempts++
            false
        }
        assertEquals(1, attempts)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun `repeated failure stops after the bounded retry budget`() = runTest {
        var attempts = 0
        flowOf(setOf("series-a")).collectSeriesBadgeRefreshes(listOf(10, 20)) {
            attempts++
            true
        }
        assertEquals(3, attempts)
        assertEquals(30L, testScheduler.currentTime)
    }
}
