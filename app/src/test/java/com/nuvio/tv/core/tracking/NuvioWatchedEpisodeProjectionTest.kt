package com.nuvio.tv.core.tracking

import com.nuvio.tv.domain.model.WatchedItem
import com.nuvio.tv.domain.model.WatchProgress
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class NuvioWatchedEpisodeProjectionTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `late completed playback triggers refresh while explicit marks remain empty`() = runTest {
        val marks = MutableStateFlow(emptyList<WatchedItem>())
        val progress = MutableStateFlow(emptyList<WatchProgress>())
        val completions = mutableListOf<Set<Triple<String, Int?, Int?>>>()
        val job = launch {
            combine(marks, progress.completedEpisodeChanges()) { _, completed -> completed }
                .collect { completions.add(it) }
        }
        runCurrent()
        progress.value = listOf(episode(1))
        runCurrent()
        assertEquals(listOf(emptySet(), setOf(Triple("show-a", 1, 1))), completions)
        job.cancelAndJoin()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `partial playback updates do not repeatedly restart badge loading`() = runTest {
        val progress = MutableStateFlow(listOf(episode(1).copy(position = 10)))
        var refreshes = 0
        val job = launch { progress.completedEpisodeChanges().collect { refreshes++ } }
        runCurrent()
        progress.value = listOf(episode(1).copy(position = 20))
        runCurrent()
        assertEquals(1, refreshes)
        progress.value = listOf(episode(1))
        runCurrent()
        assertEquals(2, refreshes)
        job.cancelAndJoin()
    }

    @Test
    fun `fresh sync with completed progress but no explicit marks restores episodes`() {
        assertEquals(
            mapOf("show-a" to setOf(1 to 1, 1 to 2)),
            mergeNuvioWatchedEpisodeProjection(emptyList(), listOf(episode(1), episode(2)))
        )
    }

    @Test
    fun `explicit marks and completed playback combine without counting partial episodes`() {
        val explicit = WatchedItem("show-a", "series", "Show", 1, 1, 10L)
        assertEquals(
            mapOf("show-a" to setOf(1 to 1, 1 to 2)),
            mergeNuvioWatchedEpisodeProjection(
                listOf(explicit), listOf(episode(1), episode(2), episode(3).copy(position = 20L))
            )
        )
    }

    @Test
    fun `movies and incomplete episode coordinates do not create series badges`() {
        assertEquals(
            emptyMap<String, Set<Pair<Int, Int>>>(),
            mergeNuvioWatchedEpisodeProjection(
                listOf(WatchedItem("movie", "movie", "Movie", watchedAt = 10L)),
                listOf(episode(1).copy(season = null), episode(2).copy(episode = null))
            )
        )
    }

    @Test
    fun `series remain distinct and specials retain their episode coordinates`() {
        assertEquals(
            mapOf("show-a" to setOf(0 to 1), "show-b" to setOf(1 to 1)),
            mergeNuvioWatchedEpisodeProjection(
                emptyList(), listOf(episode(1).copy(season = 0), episode(1).copy(contentId = "show-b"))
            )
        )
    }

    @Test
    fun `completion uses the same progress threshold as Details`() {
        assertEquals(
            mapOf("show-a" to setOf(1 to 1)),
            mergeNuvioWatchedEpisodeProjection(
                emptyList(), listOf(
                    episode(1).copy(position = 0, duration = 0, progressPercent = 90f),
                    episode(2).copy(position = 0, duration = 0, progressPercent = 89f)
                )
            )
        )
    }

    private fun episode(number: Int) = WatchProgress(
        contentId = "show-a", contentType = "series", name = "Show", poster = null,
        backdrop = null, logo = null, videoId = "show-a:1:$number", season = 1,
        episode = number, episodeTitle = null, position = 100L, duration = 100L, lastWatched = 10L
    )
}
