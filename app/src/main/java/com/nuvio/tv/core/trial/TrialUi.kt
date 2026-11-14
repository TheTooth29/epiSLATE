@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.nuvio.tv.core.trial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.nuvio.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private class SettingsVisit { var unlocked by mutableStateOf(false) }
private val LocalSettingsVisit = staticCompositionLocalOf { SettingsVisit() }
private val LocalTrialBack = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
private fun trialSnapshot(): TrialSnapshot {
    val access = TrialAccess.get(LocalContext.current)
    val owner = LocalLifecycleOwner.current
    val snapshot by produceState(access.snapshot(), access, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { value = withContext(Dispatchers.IO) { access.snapshot() }; delay(1000) }
        }
    }
    return snapshot
}

@Composable
fun TrialAppGate(onExit: () -> Unit, content: @Composable () -> Unit) {
    if (!BuildConfig.TRIAL_ENABLED) { content(); return }
    val snapshot = trialSnapshot()
    val revision by TrialSessions.revision.collectAsState()
    // Reading revision ensures ending the last finishing session immediately closes the app surface.
    @Suppress("UNUSED_VARIABLE") val observedRevision = revision
    val settingsVisit = remember { SettingsVisit() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) settingsVisit.unlocked = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    CompositionLocalProvider(LocalSettingsVisit provides settingsVisit) {
        when {
            snapshot.status == TrialStatus.ACTIVE || (snapshot.status == TrialStatus.EXPIRED && TrialSessions.hasPlayback()) -> {
                Box(Modifier.fillMaxSize()) {
                    content()
                    if (snapshot.status == TrialStatus.ACTIVE && !TrialSessions.hasPlayback()) {
                        val minutes = (snapshot.remainingMs + 59_999) / 60_000
                        Text("Trial: ${minutes / 60}h ${minutes % 60}m remaining", color = Color(0xFFCFD8E3), fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopEnd).background(Color(0xDD101923)).padding(10.dp))
                    }
                }
            }
            snapshot.status == TrialStatus.NEEDS_CODE -> TrialActivationScreen(onExit)
            else -> TrialEndedScreen(onExit)
        }
    }
}

@Composable
fun TrialNavigationSession(navController: NavHostController, content: @Composable () -> Unit) {
    val entry by navController.currentBackStackEntryAsState()
    val visit = LocalSettingsVisit.current
    LaunchedEffect(entry?.destination?.route) {
        if (!TrialPolicy.isSettingsRoute(entry?.destination?.route.orEmpty())) visit.unlocked = false
    }
    CompositionLocalProvider(LocalTrialBack provides {
        if (!navController.popBackStack()) navController.navigate("home") { launchSingleTop = true }
    }) { content() }
}

/** Protect content before its ViewModels or effects are composed, including direct navigation. */
fun NavGraphBuilder.trialComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(route, arguments, deepLinks) { entry ->
        TrialDestinationGate(route, entry.id) { content(entry) }
    }
}

@Composable
private fun TrialDestinationGate(route: String, entryId: String, content: @Composable () -> Unit) {
    if (!BuildConfig.TRIAL_ENABLED) { content(); return }
    val access = TrialAccess.get(LocalContext.current)
    val snapshot = trialSnapshot()
    val isMovie = route.startsWith("player/")
    val movieKey = "vod:$entryId"
    // Admission is per actual back-stack entry. Restored destinations cannot create expired leases.
    val admitted = remember(entryId) { access.canStart() }
    if (isMovie && admitted) {
        DisposableEffect(entryId) {
            TrialSessions.enter(movieKey)
            onDispose { TrialSessions.leave(movieKey) }
        }
    }
    val canFinish = (isMovie && TrialSessions.contains(movieKey)) ||
        (route in setOf("live_tv", "live_tv_player") && TrialSessions.contains("live"))
    val visit = LocalSettingsVisit.current
    val back = LocalTrialBack.current
    when {
        snapshot.status != TrialStatus.ACTIVE && !canFinish -> TrialEndedScreen(back)
        TrialPolicy.isSettingsRoute(route) && !visit.unlocked -> TrialPinScreen(onCancel = back, onUnlocked = { visit.unlocked = true })
        else -> content()
    }
}

@Composable
private fun TrialFrame(title: String, message: String, content: @Composable ColumnScope.() -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF8AB8ED))) {
        Column(Modifier.fillMaxSize().background(Color(0xFF101923)).padding(36.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("epiSLATE Trial", color = Color(0xFF8AB8ED), fontSize = 24.sp)
            Spacer(Modifier.height(20.dp))
            Text(title, color = Color.White, fontSize = 26.sp)
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color(0xFFCFD8E3), modifier = Modifier.widthIn(max = 620.dp))
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun NumericEntry(value: String, length: Int, label: String, masked: Boolean, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    OutlinedTextField(value, onValueChange = { text ->
        if (text.length <= length && text.all { it in '0'..'9' }) onChange(text)
    }, modifier = Modifier.width(320.dp).focusRequester(focus), singleLine = true,
        label = { androidx.compose.material3.Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (masked) KeyboardType.NumberPassword else KeyboardType.Number),
        visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None)
}

@Composable
private fun TrialActivationScreen(onExit: () -> Unit) {
    val access = TrialAccess.get(LocalContext.current)
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var activated by remember { mutableStateOf(false) }
    BackHandler(onBack = onExit)
    TrialFrame(if (activated) "Your trial is ready" else "Your invitation to try epiSLATE",
        if (activated) "Your 72 hours has started. Enjoy exploring epiSLATE."
        else "Enter your six-digit invitation code. Your 72-hour trial starts when you activate it.") {
        if (activated) {
            Text("Ends: " + DateFormat.getDateTimeInstance().format(Date(access.snapshot().expiresAt)), color = Color.White)
        } else {
            NumericEntry(code, 6, "Invitation code", false) { code = it; error = null }
            error?.let { Text(it, color = Color(0xFFFFB4AB)) }
            Spacer(Modifier.height(16.dp))
            Button(enabled = code.length == 6 && !busy, onClick = {
                busy = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { access.redeem(code) }
                    activated = result == RedemptionResult.ACTIVATED
                    error = when (result) {
                        RedemptionResult.INVALID_CODE -> "That invitation code is not valid. Please check it."
                        RedemptionResult.ALREADY_STARTED -> "This installation has already started its trial."
                        RedemptionResult.STORAGE_ERROR -> "Unable to save activation. Please try again."
                        else -> null
                    }
                    busy = false
                }
            }) { Text(if (busy) "Activating…" else "Activate 72-hour trial") }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onExit) { Text("Exit") }
    }
}

@Composable
fun TrialEndedScreen(onExit: () -> Unit = { }) {
    BackHandler(onBack = onExit)
    TrialFrame("Your trial has ended", "Thanks for trying epiSLATE. Your 72-hour preview is complete.") {
        Button(onClick = onExit) { Text("Close") }
    }
}

@Composable
private fun TrialPinScreen(onCancel: () -> Unit, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    BackHandler(onBack = onCancel)
    TrialFrame("Settings is locked", "Enter the master PIN to open Settings.") {
        NumericEntry(pin, 4, "PIN", true) { pin = it; error = false }
        if (error) Text("Incorrect PIN. Please try again.", color = Color(0xFFFFB4AB))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(enabled = pin.length == 4, onClick = {
                if (pin == "6969") onUnlocked() else { error = true; pin = "" }
            }) { Text("Unlock Settings") }
            Button(onClick = onCancel) { Text("Cancel") }
        }
    }
}
