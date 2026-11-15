package com.nuvio.tv.updater

import android.app.job.JobScheduler
import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TestAppUpdateIsolationTest {
    private val context = mockk<Context>()
    private val scheduler = mockk<JobScheduler>(relaxed = true)
    private val repository = mockk<UpdateRepository>()
    private val preferences = mockk<UpdatePreferences>()
    private val downloads = mockk<UpdateDownloadStore>()

    private fun viewModel(): UpdateViewModel {
        every { context.packageName } returns "com.nuvio.livetv.test"
        every { context.getSystemService(Context.JOB_SCHEDULER_SERVICE) } returns scheduler
        every { context.getString(any()) } returns "Preview update notification"
        coEvery { downloads.clear() } just Runs
        coEvery { preferences.setAutoInstalledTag(null) } just Runs
        return UpdateViewModel(context, repository, preferences, downloads)
    }

    @Test
    fun `test startup clears cached release cancels jobs and never checks the feed`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val model = viewModel()
            testScheduler.runCurrent()
            assertFalse(model.uiState.value.showBanner)
            assertFalse(model.uiState.value.supportsReleaseUpdates)
            coVerify(exactly = 1) { downloads.clear() }
            coVerify(exactly = 1) { preferences.setAutoInstalledTag(null) }
            coVerify(exactly = 0) { repository.getLatestUpdate(any()) }
            verify { scheduler.cancel(10_020); scheduler.cancel(10_021); scheduler.cancel(10_022) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `manual preview cannot download or install and dismisses normally`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val model = viewModel()
            testScheduler.runCurrent()
            model.checkForUpdates(force = true, showNoUpdateFeedback = false)
            assertTrue(model.uiState.value.showBanner)
            assertTrue(model.uiState.value.isUpdatePreview)
            assertFalse(model.uiState.value.isUpdateAvailable)
            assertNull(model.uiState.value.downloadedApkPath)
            assertEquals("", model.uiState.value.update?.assetUrl)
            model.downloadUpdate()
            model.installUpdateOrRequestPermission()
            testScheduler.runCurrent()
            coVerify(exactly = 0) { repository.getLatestUpdate(any()) }
            coVerify(exactly = 0) { downloads.download(any(), any()) }
            model.dismissBanner()
            assertFalse(model.uiState.value.showBanner)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
