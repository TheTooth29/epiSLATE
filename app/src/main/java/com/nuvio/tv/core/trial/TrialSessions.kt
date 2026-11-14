package com.nuvio.tv.core.trial

import com.nuvio.tv.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory finishing permissions. Process death never restores an expired session. */
object TrialSessions {
    private val entries = mutableSetOf<String>()
    private val _revision = MutableStateFlow(0)
    val revision = _revision.asStateFlow()
    @Synchronized fun enter(id: String) { if (BuildConfig.TRIAL_ENABLED && entries.add(id)) _revision.value++ }
    @Synchronized fun leave(id: String) { if (entries.remove(id)) _revision.value++ }
    @Synchronized fun contains(id: String): Boolean = id in entries
    @Synchronized fun hasPlayback(): Boolean = entries.isNotEmpty()
    @Synchronized fun endMovies() {
        if (entries.removeAll { it.startsWith("vod:") }) _revision.value++
    }
}
