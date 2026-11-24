@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.Icon
import com.nuvio.tv.domain.model.AuthState
import com.nuvio.tv.ui.components.EpiSlateProfileWordmark
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import com.nuvio.tv.ui.theme.NuvioTheme

/** Official-account sign-in; the caller gates the approved regular/Test packages. */
@Composable
internal fun EpiSlateEmailSignInScreen(
    onBack: () -> Unit,
    onContinue: (() -> Unit)?,
    viewModel: AccountViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val eyeFocus = remember { FocusRequester() }
    val signInFocus = remember { FocusRequester() }
    var passwordHasFocus by remember { mutableStateOf(false) }
    var eyeHasFocus by remember { mutableStateOf(false) }
    var keyboardWasVisible by remember { mutableStateOf(false) }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val keyboard = LocalSoftwareKeyboardController.current
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !state.isLoading
    val submit = { if (canSubmit) { keyboard?.hide(); viewModel.signIn(email.trim(), password) } }

    BackHandler(onBack = onBack)
    DisposableEffect(Unit) {
        viewModel.clearQrLoginSession()
        onDispose { password = "" }
    }
    LaunchedEffect(Unit) { emailFocus.requestFocusAfterFrames(frames = 3) }
    LaunchedEffect(keyboardVisible) {
        // Android Back can dismiss the IME without running KeyboardActions.onDone.
        if (keyboardWasVisible && !keyboardVisible && passwordHasFocus) {
            eyeFocus.requestFocusAfterFrames(frames = 2)
        }
        keyboardWasVisible = keyboardVisible
    }
    LaunchedEffect(state.authState, state.isLoading) {
        if (!completed && state.authState is AuthState.FullAccount && !state.isLoading) {
            completed = true
            password = ""
            (onContinue ?: onBack).invoke()
        }
    }

    // Match ProfileSelectionMainContent's exact parent insets and shared wordmark.
    // No separate logo dimensions or placement that could drift from the picker.
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF8AB8ED))) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF101923)).padding(
            horizontal = NuvioTheme.spacing.huge,
            vertical = NuvioTheme.spacing.xxxl
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EpiSlateProfileWordmark(lowerForPicker = true)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to epiSLATE", color = Color.White, fontSize = 26.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Make yourself at home.", color = Color(0xFFCFD8E3), fontSize = 17.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.width(320.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it }, singleLine = true,
                    label = { androidx.compose.material3.Text("Email address") },
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(emailFocus)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, singleLine = true,
                    label = { androidx.compose.material3.Text("Password") },
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        eyeFocus.requestFocus()
                    }),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Button(onClick = { visible = !visible }, enabled = !state.isLoading,
                            modifier = Modifier.size(48.dp).focusRequester(eyeFocus)
                                .onFocusChanged { eyeHasFocus = it.isFocused }.focusProperties {
                                left = passwordFocus
                                down = signInFocus
                                up = emailFocus
                            },
                            contentPadding = PaddingValues(8.dp),
                            scale = ButtonDefaults.scale(focusedScale = 1f),
                            colors = ButtonDefaults.colors(
                                containerColor = Color.Transparent, contentColor = Color(0xFFCFD8E3),
                                focusedContainerColor = Color.Transparent, focusedContentColor = Color(0xFF8AB8ED),
                                pressedContainerColor = Color.Transparent, pressedContentColor = Color(0xFF8AB8ED),
                                disabledContainerColor = Color.Transparent, disabledContentColor = Color(0xFF8090A2)
                            )) {
                            // Keep the accessible 48dp target; only the inset icon gains a focus outline.
                            Box(Modifier.size(32.dp).border(1.dp,
                                if (eyeHasFocus) Color(0xFF8AB8ED).copy(alpha = 0.7f) else Color.Transparent,
                                RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (visible) "Hide password" else "Show password",
                                modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus)
                        .onFocusChanged { passwordHasFocus = it.isFocused }
                        .focusProperties { right = eyeFocus; down = signInFocus; up = emailFocus }
                        .onPreviewKeyEvent { event ->
                            if (!keyboardVisible && event.key == Key.DirectionRight) {
                                if (event.type == KeyEventType.KeyDown) eyeFocus.requestFocus()
                                true
                            } else false
                        }
                )
                // Trial activation's body/action spacing, with its extra text-line clearance.
                Spacer(Modifier.height(20.dp + with(LocalDensity.current) { 24.sp.toDp() }))
                Button(onClick = submit, enabled = canSubmit,
                    modifier = Modifier.focusRequester(signInFocus).focusProperties { up = eyeFocus },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF243548), contentColor = Color(0xFFE4EDF7),
                        focusedContainerColor = Color(0xFF8AB8ED), focusedContentColor = Color(0xFF101923),
                        pressedContainerColor = Color(0xFF6B9AD1), pressedContentColor = Color(0xFF101923),
                        disabledContainerColor = Color(0xFF1B2836), disabledContentColor = Color(0xFF8090A2)
                    )) {
                    Text(if (state.isLoading) "Signing in..." else "Sign in", fontSize = 17.sp)
                }
                state.error?.takeIf { it.isNotBlank() }?.let {
                    Text(com.nuvio.tv.core.auth.AccountPasswordLoginPolicy.errorMessage(it), color = Color(0xFFFFB4AB), fontSize = 16.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    }
}
