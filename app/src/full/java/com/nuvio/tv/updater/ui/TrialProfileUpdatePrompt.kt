@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.updater.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.core.trial.TrialUpdatePolicy
import com.nuvio.tv.updater.ApkDownloader
import com.nuvio.tv.updater.ApkInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

internal data class TrialApprovedUpdate(val versionCode: Long, val sha256: String)
internal data class TrialProfileUpdateState(
    val update: TrialApprovedUpdate? = null,
    val downloading: Boolean = false,
    val percent: Int? = null,
    val permissionRequired: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TrialProfileUpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val downloader: ApkDownloader
) : ViewModel() {
    private val state = MutableStateFlow(TrialProfileUpdateState())
    internal val uiState = state.asStateFlow()
    private var work: Job? = null
    private var readyFile: File? = null

    fun checkOnEntry() {
        if (!BuildConfig.TRIAL_ENABLED || context.packageName != TrialUpdatePolicy.PACKAGE) return
        work?.cancel()
        state.value = TrialProfileUpdateState()
        readyFile = null
        work = viewModelScope.launch {
            val candidate = withContext(Dispatchers.IO) {
                runCatching {
                    client.newCall(Request.Builder().url(TrialUpdatePolicy.FEED_URL)
                        .header("Cache-Control", "no-cache").build()).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        val source = response.body?.source() ?: return@use null
                        source.request(16_385)
                        val text = source.readUtf8(minOf(source.buffer.size, 16_385L))
                        if (text.length > 16_384) return@use null
                        val feed = JSONObject(text)
                        val version = feed.getLong("versionCode")
                        val digest = feed.getString("sha256")
                        if (TrialUpdatePolicy.eligible(context.packageName, BuildConfig.VERSION_CODE.toLong(),
                            feed.getString("packageName"), version, feed.optBoolean("approved", false),
                            feed.getString("apkUrl"), digest)) TrialApprovedUpdate(version, digest) else null
                    }
                }.getOrNull()
            }
            state.value = TrialProfileUpdateState(update = candidate)
        }
    }

    fun dismiss() {
        work?.cancel()
        state.value = TrialProfileUpdateState()
    }

    fun install() {
        if (state.value.downloading) return
        val update = state.value.update ?: return
        if (!ApkInstaller.canRequestPackageInstalls(context)) {
            state.value = state.value.copy(permissionRequired = true, error = null)
            return
        }
        state.value = state.value.copy(permissionRequired = false, error = null, downloading = true)
        work = viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val target = File(context.cacheDir, "trial-updates/${update.versionCode}.apk")
                    val file = readyFile?.takeIf { it.isFile } ?: downloader.download(
                        TrialUpdatePolicy.APK_URL, target
                    ) { downloaded, total ->
                        state.value = state.value.copy(percent = total?.let { ((downloaded * 100) / it).toInt().coerceIn(0, 100) })
                    }.getOrThrow()
                    ensureActive()
                    val digest = MessageDigest.getInstance("SHA-256")
                    file.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            digest.update(buffer, 0, count)
                        }
                    }
                    val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                    check(actual.equals(update.sha256, ignoreCase = true))
                    ApkInstaller.requireSelfUpdate(context, file)
                    @Suppress("DEPRECATION")
                    val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                    check(archive != null && androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(archive) == update.versionCode)
                    file
                }
                readyFile = file
                // Explicit foreground Android confirmation; never silently install or clear app data.
                ApkInstaller.launchSystemInstaller(context, file)
                state.value = state.value.copy(downloading = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                readyFile = null
                state.value = state.value.copy(downloading = false, percent = null,
                    error = "Unable to install this update.  Please try again later.")
            }
        }
    }

    fun openInstallPermission() {
        ApkInstaller.buildUnknownSourcesSettingsIntent(context)?.let(context::startActivity)
    }
}

@Composable
fun TrialProfileUpdatePrompt() {
    if (!BuildConfig.TRIAL_ENABLED) return
    val model: TrialProfileUpdateViewModel = hiltViewModel()
    val state by model.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { model.checkOnEntry() }
    DisposableEffect(Unit) { onDispose { model.dismiss() } }
    if (state.update == null) return
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = model::dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.widthIn(max = 560.dp).background(Color(0xFF101D36), RoundedCornerShape(20.dp))
            .padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("An epiSLATE Trial update is available", fontSize = 24.sp, color = Color.White)
            Text(if (state.permissionRequired) "Allow epiSLATE Trial to install updates, then return here and select Install update."
                else "Update to the latest Trial version.  Your activation and viewing history stay with you.",
                fontSize = 17.sp, lineHeight = 24.sp, color = Color.White)
            state.error?.let { Text(it, color = Color(0xFFFFB4AB), fontSize = 17.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Button(modifier = Modifier.focusRequester(focus), enabled = !state.downloading,
                    colors = ButtonDefaults.colors(containerColor = Color(0xFF203553), contentColor = Color.White,
                        focusedContainerColor = Color(0xFF8AB8ED), focusedContentColor = Color(0xFF101D36)),
                    onClick = { if (state.permissionRequired && !ApkInstaller.canRequestPackageInstalls(context)) model.openInstallPermission() else model.install() }) {
                    Text(when {
                        state.downloading -> "Downloading${state.percent?.let { " $it%" } ?: "…"}"
                        state.permissionRequired && !ApkInstaller.canRequestPackageInstalls(context) -> "Allow updates"
                        else -> "Install update"
                    })
                }
                Button(onClick = model::dismiss,
                    colors = ButtonDefaults.colors(containerColor = Color(0xFF203553), contentColor = Color.White,
                        focusedContainerColor = Color(0xFF8AB8ED), focusedContentColor = Color(0xFF101D36))) { Text("Later") }
            }
        }
        LaunchedEffect(Unit) { withFrameNanos { }; withFrameNanos { }; focus.requestFocus() }
    }
}
