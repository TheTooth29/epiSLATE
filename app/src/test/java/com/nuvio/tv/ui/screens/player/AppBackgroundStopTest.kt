package com.nuvio.tv.ui.screens.player

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBackgroundStopTest {
    @Test fun backgroundStopReleasesButNavigationDoesNot() {
        assertTrue(isAppBackgroundStop(false, false, Lifecycle.State.CREATED))
        assertFalse(isAppBackgroundStop(false, false, Lifecycle.State.STARTED))
        assertFalse(isAppBackgroundStop(false, false, Lifecycle.State.RESUMED))
    }
    @Test fun finishingRecreationAndDestructionKeepNormalCleanup() {
        assertFalse(isAppBackgroundStop(true, false, Lifecycle.State.CREATED))
        assertFalse(isAppBackgroundStop(false, true, Lifecycle.State.CREATED))
        assertFalse(isAppBackgroundStop(false, false, Lifecycle.State.DESTROYED))
    }
}
