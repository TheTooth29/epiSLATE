package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.WatchedMutationKey

/** Cache protection must never keep offering a specific episode already known to be watched. */
internal fun withoutWatchedNextUp(
    items: List<ContinueWatchingItem>, watched: Set<WatchedMutationKey>
): List<ContinueWatchingItem> = items.filterNot { item ->
    item is ContinueWatchingItem.NextUp &&
        WatchedMutationKey(item.info.contentId, item.info.season, item.info.episode) in watched
}
