package com.nuvio.tv.ui.screens.home

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.player.PlayerSessionTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun HomeViewModel.observeSeriesWatchedBadges() {
    viewModelScope.launch {
        combine(
            watchProgressRepository.watchedItems,
            seriesBadgeProfileReady,
            PlayerSessionTracker.isSessionOpen,
            cwPipelineRefreshTrigger
        ) { items, profileId, playerOpen, refresh -> Triple(items to refresh, profileId, playerOpen) }
            .collectSeriesBadgeRefreshes { (_, profileId, playerOpen) ->
                if (playerOpen || profileId == null) {
                    false
                } else {
                    withContext(Dispatchers.IO) {
                        try {
                            refreshSeriesWatchedBadges(profileId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w(HomeViewModel.TAG, "Series watched badges will retry", e)
                            true
                        }
                    }
                }
            }
    }
}
