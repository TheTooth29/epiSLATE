package com.nuvio.tv.ui.screens.player

import androidx.lifecycle.Lifecycle

/** A navigation entry also stops during in-app navigation; only release for host backgrounding. */
internal fun isAppBackgroundStop(
    isFinishing: Boolean,
    isChangingConfigurations: Boolean,
    hostState: Lifecycle.State
): Boolean = !isFinishing && !isChangingConfigurations &&
    hostState != Lifecycle.State.DESTROYED && !hostState.isAtLeast(Lifecycle.State.STARTED)
