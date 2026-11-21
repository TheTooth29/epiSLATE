@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.IconButton
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

/** Hosted-account experiment only; the caller gates by exact Test package. */
@Composable
internal fun TestEmailSignInScreen(
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
    val keyboard = LocalSoftwareKeyboardController.current
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !state.isLoading
    val submit = { if (canSubmit) { keyboard?.hide(); viewModel.signIn(email.trim(), password) } }

    BackHandler(onBack = onBack)
    DisposableEffect(Unit) {
        viewModel.clearQrLoginSession()
        onDispose { password = "" }
    }
    LaunchedEffect(Unit) { emailFocus.requestFocusAfterFrames(frames = 3) }
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
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }, enabled = !state.isLoading) {
                            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (visible) "Hide password" else "Show password",
                                modifier = Modifier.size(24.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus)
                )
                // Trial activation's body/action spacing, with its extra text-line clearance.
                Spacer(Modifier.height(20.dp + with(LocalDensity.current) { 24.sp.toDp() }))
                Button(onClick = submit, enabled = canSubmit,
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF243548), contentColor = Color(0xFFE4EDF7),
                        focusedContainerColor = Color(0xFF8AB8ED), focusedContentColor = Color(0xFF101923),
                        pressedContainerColor = Color(0xFF6B9AD1), pressedContentColor = Color(0xFF101923),
                        disabledContainerColor = Color(0xFF1B2836), disabledContentColor = Color(0xFF8090A2)
                    )) {
                    Text(if (state.isLoading) "Signing in..." else "Sign in", fontSize = 17.sp)
                }
                state.error?.takeIf { it.isNotBlank() }?.let {
                    Text(com.nuvio.tv.core.auth.TestPasswordLoginPolicy.errorMessage(it), color = Color(0xFFFFB4AB), fontSize = 16.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    }
}
