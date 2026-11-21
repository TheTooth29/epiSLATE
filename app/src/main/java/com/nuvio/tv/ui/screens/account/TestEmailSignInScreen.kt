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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.nuvio.tv.ui.theme.epiSlateBackground

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
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !state.isLoading
    val submit = { if (canSubmit) viewModel.signIn(email.trim(), password) }

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
    Column(
        Modifier.fillMaxSize().background(epiSlateBackground()).padding(
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
            Text("Welcome to epiSLATE", color = Color.White, fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Make yourself at home.", color = Color(0xFFCFD8E3), fontSize = 17.sp)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InputField(email, { email = it }, "Email address", keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    onImeAction = { passwordFocus.requestFocus() },
                    modifier = Modifier.focusRequester(emailFocus))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InputField(password, { password = it }, "Password", keyboardType = KeyboardType.Password,
                        isPassword = !visible, imeAction = ImeAction.Done, onImeAction = submit,
                        modifier = Modifier.weight(1f).focusRequester(passwordFocus))
                    Button(onClick = { visible = !visible }, enabled = !state.isLoading) {
                        Icon(
                            imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (visible) "Hide password" else "Show password",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Button(onClick = submit, enabled = canSubmit, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF8AB8ED), contentColor = Color(0xFF101923),
                        focusedContainerColor = Color(0xFFB8D6F7), focusedContentColor = Color(0xFF101923)
                    )) {
                    Text(if (state.isLoading) "Signing in…" else "Sign in",
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                state.error?.takeIf { it.isNotBlank() }?.let {
                    Text(com.nuvio.tv.core.auth.TestPasswordLoginPolicy.errorMessage(it), color = Color(0xFFFFB4AB), fontSize = 16.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

