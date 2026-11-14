package com.nuvio.tv.core.auth

import android.content.Context
import com.nuvio.tv.core.profile.ProfileScopedCredentialStore
import com.nuvio.tv.core.sync.androidtv.AndroidTvChannelManager
import com.nuvio.tv.data.local.AppOnboardingDataStore
import com.nuvio.tv.data.local.ProfileDataStore
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.data.local.ProfileLockStateDataStore
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountLocalDataResetServiceTest {
    private val context = mockk<Context>()
    private val factory = mockk<ProfileDataStoreFactory>(relaxed = true)
    private val profiles = mockk<ProfileDataStore>(relaxed = true)
    private val onboarding = mockk<AppOnboardingDataStore>(relaxed = true)
    private val locks = mockk<ProfileLockStateDataStore>(relaxed = true)
    private val channels = mockk<AndroidTvChannelManager>(relaxed = true)

    private fun service(): AccountLocalDataResetService {
        val filesDir = mockk<File>()
        every { context.filesDir } returns filesDir
        every { filesDir.listFiles() } returns emptyArray()
        return AccountLocalDataResetService(context, factory, profiles, onboarding, locks,
            emptySet<ProfileScopedCredentialStore>(), channels)
    }

    @Test
    fun `sign out returns to login only after clearing account settings`() = runTest {
        service().clearAfterSignOut()
        coVerifyOrder {
            factory.clearProfileScopedData()
            profiles.clearAll()
            locks.clearAll()
            onboarding.setHasSeenAuthQrOnFirstLaunch(false)
        }
    }

    @Test
    fun `channel cleanup failure still resets sign in onboarding`() = runTest {
        coEvery { channels.clearAll() } throws IllegalStateException("Channel cleanup failed")
        service().clearAfterSignOut()
        coVerifyOrder {
            channels.clearAll()
            profiles.clearAll()
            onboarding.setHasSeenAuthQrOnFirstLaunch(false)
        }
    }
}
