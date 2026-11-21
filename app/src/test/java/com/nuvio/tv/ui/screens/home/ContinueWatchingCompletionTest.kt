package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.WatchedMutationKey
import com.nuvio.tv.domain.model.WatchProgress
import org.junit.Assert.*
import org.junit.Test

class ContinueWatchingCompletionTest {
    private fun next(episode: Int, id: String = "nocturne") = ContinueWatchingItem.NextUp(NextUpInfo(
        contentId = id, contentType = "series", name = id, poster = null, backdrop = null,
        logo = null, videoId = "$id:1:$episode", season = 1, episode = episode,
        episodeTitle = null, thumbnail = null, lastWatched = 1L, sortTimestamp = 1L,
        isReleaseAlert = true
    ))
    private val watched = setOf(WatchedMutationKey("nocturne", 1, 5))

    @Test fun watchedEpisodeCannotSurviveCachedReleaseAlert() {
        assertTrue(withoutWatchedNextUp(listOf(next(5)), watched).isEmpty())
    }
    @Test fun nextUnwatchedEpisodeAndOtherShowsRemain() {
        val items = listOf(next(5), next(6), next(5, "other"))
        assertEquals(items.drop(1), withoutWatchedNextUp(items, watched))
    }
    @Test fun unfinishedRewatchIsNotRemovedByHistoricalCheckmark() {
        val progress = ContinueWatchingItem.InProgress(WatchProgress(
            "nocturne", "series", "Nocturne", null, null, null, "nocturne:1:5", 1, 5,
            null, 100L, 1000L, 2L
        ))
        assertEquals(listOf(progress), withoutWatchedNextUp(listOf(progress), watched))
    }
    @Test fun sameCountHistoryReplacementUsesActualEpisodeIdentity() {
        assertEquals(listOf(next(5)), withoutWatchedNextUp(listOf(next(5)), setOf(WatchedMutationKey("nocturne", 1, 4))))
        assertTrue(withoutWatchedNextUp(listOf(next(5)), watched).isEmpty())
    }
    @Test fun unmarkingEpisodeAllowsNextUpAgain() {
        assertEquals(listOf(next(5)), withoutWatchedNextUp(listOf(next(5)), emptySet()))
    }
}
