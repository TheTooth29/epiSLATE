@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.nuvio.tv.core.trial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.tv.material3.ButtonDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

object TrialAdminSession {
    var unlocked by mutableStateOf(false)
    private var clicks = 0
    fun reset() { unlocked = false; clicks = 0 }
    fun enterSettings(): Boolean {
        if (!BuildConfig.TRIAL_ENABLED || unlocked) return true
        clicks++
        return (clicks >= 7).also { if (it) clicks = 0 }
    }
}
object TrialExitSession { var requested by mutableStateOf(false) }
private class SettingsVisit {
    var unlocked: Boolean
        get() = TrialAdminSession.unlocked
        set(value) { TrialAdminSession.unlocked = value }
}
private val LocalSettingsVisit = staticCompositionLocalOf { SettingsVisit() }
private val LocalTrialBack = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
private fun trialSnapshot(): TrialSnapshot {
    val access = TrialAccess.get(LocalContext.current)
    val owner = LocalLifecycleOwner.current
    val snapshot by produceState(access.snapshot(), access, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { value = withContext(Dispatchers.IO) { access.refreshOnline(); access.snapshot() }; delay(1000) }
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
    var activationWelcome by remember { mutableStateOf(false) }
    var awaitingActivation by remember { mutableStateOf(snapshot.status == TrialStatus.NEEDS_CODE) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                TrialAdminSession.reset()
                // An exit prompt belongs to the current foreground visit, never a later launch.
                TrialExitSession.requested = false
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            TrialExitSession.requested = false
        }
    }
    CompositionLocalProvider(LocalSettingsVisit provides settingsVisit) {
        Box(Modifier.fillMaxSize()) {
        when {
            awaitingActivation -> TrialActivationScreen(onExit) {
                activationWelcome = true
                awaitingActivation = false
            }
            snapshot.status == TrialStatus.ACTIVE && activationWelcome -> TrialWelcomeScreen { activationWelcome = false }
            snapshot.status == TrialStatus.ACTIVE || (snapshot.status != TrialStatus.NEEDS_CODE && TrialSessions.hasPlayback()) -> {
                content()
            }
            snapshot.status == TrialStatus.CONNECTION_REQUIRED -> TrialConnectionScreen(onExit)
            snapshot.status == TrialStatus.NEEDS_CODE -> TrialActivationScreen(onExit) { activationWelcome = true }
            else -> TrialEndedScreen(onExit)
        }
        // Retain navigation underneath, but isolate remote focus in a modal window.
        // Startup/profile/sidebar effects must not steal focus from the exit buttons.
        if (TrialExitSession.requested) Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false,
                dismissOnBackPress = false, dismissOnClickOutside = false, decorFitsSystemWindows = false)
        ) {
            TrialExitScreen(snapshot,
                onStay = { TrialExitSession.requested = false },
                onExit = { TrialExitSession.requested = false; onExit() })
        }
        }
    }
}

@Composable
fun TrialNavigationSession(navController: NavHostController, content: @Composable () -> Unit) {
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
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.nuvio.tv.R.drawable.epislate_wordmark), "epiSLATE", Modifier.width(240.dp).height(70.dp))
            Spacer(Modifier.height(20.dp))
            Text(title, color = Color.White, fontSize = 26.sp)
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color(0xFFCFD8E3), modifier = Modifier.widthIn(max = 620.dp),
                fontSize = 17.sp, lineHeight = 24.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun NumericEntry(value: String, length: Int, label: String, masked: Boolean, nextFocus: FocusRequester? = null, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var keyboardWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(keyboardVisible) {
        if (keyboardWasVisible && !keyboardVisible && value.length == length) nextFocus?.requestFocus()
        keyboardWasVisible = keyboardVisible
    }
    OutlinedTextField(value, onValueChange = { text ->
        if (text.length <= length && text.all { it in '0'..'9' }) onChange(text)
    }, modifier = Modifier.width(320.dp).focusRequester(focus).focusProperties { down = if (value.length == length) nextFocus ?: FocusRequester.Default else FocusRequester.Default }, singleLine = true,
        label = { androidx.compose.material3.Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (masked) KeyboardType.NumberPassword else KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            keyboard?.hide()
            if (value.length == length) nextFocus?.requestFocus()
        }),
        visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None)
}

@Composable
private fun TrialActivationScreen(onExit: () -> Unit, onActivated: () -> Unit) {
    val access = TrialAccess.get(LocalContext.current)
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var activated by remember { mutableStateOf(false) }
    val activateFocus = remember { FocusRequester() }
    val exitFocus = remember { FocusRequester() }
    BackHandler(onBack = onExit)
    TrialFrame(if (activated) "Your trial is ready" else "Your invitation to try epiSLATE",
        if (activated) "Your 72 hours has started.  Enjoy exploring epiSLATE."
        else "Enter your six-digit invitation code.  Your 72-hour trial starts when you activate it.") {
        if (activated) {
            Text("Enjoy your epiSLATE trial.", color = Color.White)
        } else {
            NumericEntry(code, 6, "Invitation code", false, nextFocus = activateFocus) { code = it; error = null }
            error?.let { Text(it, color = Color(0xFFFFB4AB), fontSize = 17.sp, lineHeight = 24.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.widthIn(max = 620.dp)) }
            Spacer(Modifier.height(16.dp))
            TrialButton(modifier = Modifier.focusRequester(activateFocus).focusProperties { down = exitFocus }, enabled = code.length == 6 && !busy, onClick = {
                busy = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { access.redeem(code) }
                    activated = result == RedemptionResult.ACTIVATED
                    if (activated) onActivated()
                    error = when (result) {
                        RedemptionResult.INVALID_CODE -> "That invitation code is not valid."
                        RedemptionResult.REVOKED_CODE -> "This invitation code has been canceled."
                        RedemptionResult.ALREADY_STARTED -> "This installation has already started its trial."
                        RedemptionResult.NETWORK_ERROR -> "Unable to connect.  Check your internet connection and try again."
                        RedemptionResult.RATE_LIMITED -> "Too many attempts.  Please wait a minute and try again.  If this continues, contact your trial administrator."
                        RedemptionResult.STORAGE_ERROR -> "Unable to save activation.  Please try again."
                        else -> null
                    }
                    busy = false
                }
            }) { Text(if (busy) "Activating…" else "Activate 72-hour trial") }
        }
        Spacer(Modifier.height(12.dp))
        TrialButton(modifier = Modifier.focusRequester(exitFocus).focusProperties { if (code.length == 6 && !busy && !activated) up = activateFocus }, enabled = !busy, onClick = onExit) { Text("Exit") }
    }
}

@Composable
private fun TrialWelcomeScreen(onContinue: () -> Unit) {
    val focus = remember { FocusRequester() }
    BackHandler { }
    TrialFrame("Your epiSLATE trial is active", "Your 72-hour trial has started.  Enjoy exploring epiSLATE!") {
        TrialContactLine("Questions during your trial?  Email ")
        // One extra body-text line (24sp) above the action, matching the exit page.
        Spacer(Modifier.height(with(LocalDensity.current) { 24.sp.toDp() } + 20.dp))
        TrialButton(modifier = Modifier.focusRequester(focus), onClick = onContinue) { Text("Continue") }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun TrialContactLine(prompt: String) {
    val text = androidx.compose.ui.text.buildAnnotatedString {
        append(prompt)
        pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF8AB8ED)))
        append("epiSL8@gmail.com")
        pop()
        append(".")
    }
    Text(text = text, color = Color(0xFFCFD8E3), fontSize = 17.sp, lineHeight = 24.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth())
}

@Composable
private fun TrialExitScreen(snapshot: TrialSnapshot, onStay: () -> Unit, onExit: () -> Unit) {
    val stay = remember { FocusRequester() }
    val leave = remember { FocusRequester() }
    BackHandler { /* Explicit button choice keeps the exit page on screen. */ }
    val minutes = (snapshot.remainingMs.coerceAtLeast(0) + 59_999) / 60_000
    val time = if (snapshot.status == TrialStatus.ACTIVE)
        "You have ${minutes / 60} hours, ${minutes % 60} minutes left in your trial."
        else if (snapshot.status == TrialStatus.EXPIRED) "Your trial is complete."
        else "Connect to the internet to check your remaining trial time."
    TrialFrame("Enjoying your sneak peek?", time) {
        TrialContactLine("Questions during your trial?  Email ")
        Spacer(Modifier.height(with(LocalDensity.current) { 24.sp.toDp() } + 20.dp))
        TrialButton(modifier = Modifier.focusRequester(stay).focusProperties {
            up = FocusRequester.Cancel; down = leave; left = FocusRequester.Cancel; right = FocusRequester.Cancel
        }, onClick = onStay) { Text("Return to epiSLATE") }
        Spacer(Modifier.height(12.dp))
        TrialButton(modifier = Modifier.focusRequester(leave).focusProperties {
            up = stay; down = FocusRequester.Cancel; left = FocusRequester.Cancel; right = FocusRequester.Cancel
        }, onClick = onExit) { Text("Exit epiSLATE") }
    }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Wait until the modal window and its buttons have been placed.
            repeat(2) { withFrameNanos { } }
            stay.requestFocus(androidx.compose.ui.focus.FocusDirection.Enter)
            awaitCancellation()
        }
    }
}

@Composable
fun TrialEndedScreen(onExit: () -> Unit = { }) {
    BackHandler(onBack = onExit)
    TrialFrame("Your epiSLATE trial has ended", "Thanks for trying epiSLATE.  Your 72-hour preview is complete.") {
        TrialContactLine("Interested in an epiSLATE subscription?  Email ")
        Spacer(Modifier.height(20.dp))
        TrialButton(onClick = onExit) { Text("Close") }
    }
}

@Composable
private fun TrialPinScreen(onCancel: () -> Unit, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    BackHandler(onBack = onCancel)
    TrialFrame("Settings is locked", "Enter the master PIN to open Settings.") {
        NumericEntry(pin, 4, "PIN", true) { pin = it; error = false }
        if (error) Text("Incorrect PIN.  Please try again.", color = Color(0xFFFFB4AB),
            fontSize = 17.sp, lineHeight = 24.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TrialButton(enabled = pin.length == 4, onClick = {
                if (pin == "6969") onUnlocked() else { error = true; pin = "" }
            }) { Text("Unlock Settings") }
            TrialButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun TrialConnectionScreen(onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    TrialFrame("Connection required", "Connect to the internet to verify your trial.  epiSLATE will retry automatically.") {
        TrialButton(onClick = onExit) { Text("Exit") }
    }
}

/** Explicit TV colors keep unfocused buttons dark even outside the regular app theme. */
@Composable
private fun TrialButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF243548),
            contentColor = Color(0xFFE4EDF7),
            focusedContainerColor = Color(0xFF8AB8ED),
            focusedContentColor = Color(0xFF101923),
            pressedContainerColor = Color(0xFF6B9AD1),
            pressedContentColor = Color(0xFF101923),
            disabledContainerColor = Color(0xFF1B2836),
            disabledContentColor = Color(0xFF8090A2)
        ),
        content = content
    )
}
