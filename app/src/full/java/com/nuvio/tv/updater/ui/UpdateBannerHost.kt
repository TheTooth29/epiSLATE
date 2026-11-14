package com.nuvio.tv.updater.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nuvio.tv.updater.UpdateUiState

@Composable
fun UpdateBannerHost(
    state: UpdateUiState,
    onDismissBanner: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismissUnknownSources: () -> Unit,
    onOpenUnknownSources: () -> Unit,
    onFeedbackShown: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(state.feedbackMessage) {
        val message = state.feedbackMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        onFeedbackShown()
    }

    DisposableEffect(lifecycleOwner, state.showUnknownSourcesDialog) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && state.showUnknownSourcesDialog) {
                onInstall()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Prefer an available update over the notice for a completed automatic install.
    val update = state.update ?: state.installedUpdate
    val showBanner = state.showBanner && update != null
    var readyToPrompt by remember { mutableStateOf(false) }
    LaunchedEffect(showBanner) {
        readyToPrompt = false
        if (showBanner) {
            // Let the startup splash fade and homepage settle before taking focus.
            kotlinx.coroutines.delay(600)
            readyToPrompt = true
        }
    }

    // Keep the homepage at full size; dialogs use a separate window.
    Box(modifier = Modifier.fillMaxSize()) { content() }

    if (showBanner && readyToPrompt && update != null && !state.showUnknownSourcesDialog) {
        UpdateStartupDialog(
            state = state,
            update = update,
            onInstall = { if (state.downloadedApkPath != null) onInstall() else onDownload() },
            onLater = onDismissBanner
        )
    }

    if (state.showUnknownSourcesDialog) {
        UpdateUnknownSourcesDialog(
            onOpenSettings = onOpenUnknownSources,
            onDismiss = onDismissUnknownSources
        )
    }
}
