package com.nuvio.tv.core.tracking

import com.nuvio.tv.domain.model.WatchedItem
import com.nuvio.tv.domain.model.WatchProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/** Emit when episode completion changes, not on every playback-position update. */
internal fun Flow<List<WatchProgress>>.completedEpisodeChanges(): Flow<Set<Triple<String, Int?, Int?>>> =
    map { items ->
        items.filter { it.season != null && it.episode != null && it.isCompleted() }
            .map { Triple(it.contentId, it.season, it.episode) }.toSet()
    }.distinctUntilChanged()

/** Match Details: an explicit watched mark OR completed episode progress counts as watched. */
internal fun mergeNuvioWatchedEpisodeProjection(
    watchedItems: List<WatchedItem>,
    progress: Collection<WatchProgress>
): Map<String, Set<Pair<Int, Int>>> {
    val episodes = linkedMapOf<String, MutableSet<Pair<Int, Int>>>()
    watchedItems.forEach { item ->
        val season = item.season
        val episode = item.episode
        if (season != null && episode != null) {
            episodes.getOrPut(item.contentId, ::linkedSetOf).add(season to episode)
        }
    }
    progress.forEach { item ->
        val season = item.season
        val episode = item.episode
        if (season != null && episode != null && item.isCompleted()) {
            episodes.getOrPut(item.contentId, ::linkedSetOf).add(season to episode)
        }
    }
    return episodes
}
