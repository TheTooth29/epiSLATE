package com.nuvio.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.theme.epiSlateBackground

@Composable
internal fun EpiSlateProfileWordmark(modifier: Modifier = Modifier, lowerForPicker: Boolean = true) {
    BrandWordmark(
        modifier = modifier.offset(y = (if (lowerForPicker) 16 else 0).dp)
            .height((if (lowerForPicker) 120 else 100).dp),
        contentDescription = "epiSLATE",
        drawableOverride = R.drawable.epislate_wordmark
    )
}

@Composable
fun StartupSplashScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(epiSlateBackground())) {
        EpiSlateProfileWordmark(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = NuvioTheme.spacing.xxxl)
        )
        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center).size(NuvioTheme.spacing.xxxl)
        )
    }
}
