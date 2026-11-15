package com.nuvio.tv.ui.screens.home

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** A new history/profile snapshot cancels obsolete work; a failed lookup remains retryable. */
internal suspend fun <T> Flow<T>.collectSeriesBadgeRefreshes(
    retryDelaysMs: List<Long> = listOf(5_000L, 15_000L, 60_000L),
    refresh: suspend (T) -> Boolean
) {
    collectLatest { snapshot ->
        var pending = refresh(snapshot)
        for (retryDelay in retryDelaysMs) {
            if (!pending) break
            delay(retryDelay)
            pending = refresh(snapshot)
        }
    }
}
