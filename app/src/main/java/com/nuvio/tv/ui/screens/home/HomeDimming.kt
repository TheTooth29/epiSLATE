package com.nuvio.tv.ui.screens.home

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nuvio.tv.LocalSidebarExpanded
import com.nuvio.tv.ui.screens.settings.HomeDimmingViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
internal fun homeDimmingModifier(viewModel: HomeDimmingViewModel = hiltViewModel()): Modifier {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var minute by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val now = Calendar.getInstance()
                minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                ready = true
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }
    val sidebarExpanded = LocalSidebarExpanded.current
    val alpha = if (ready && !sidebarExpanded) settings.alphaAt(minute) else 0f
    return Modifier.drawWithContent {
        drawContent()
        if (alpha > 0f) drawRect(Color.Black.copy(alpha = alpha))
    }
}
