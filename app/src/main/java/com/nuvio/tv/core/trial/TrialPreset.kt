package com.nuvio.tv.core.trial

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.Button
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.local.*
import com.nuvio.tv.domain.model.*
import com.nuvio.tv.domain.repository.AddonRepository
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.streams.StreamBadgeRules
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.nuvio.tv.core.sync.homeCatalogKey
import com.nuvio.tv.core.sync.homeCollectionKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrialPreset @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profiles: ProfileDataStore,
    private val layout: LayoutPreferenceDataStore,
    private val experience: ExperienceModeDataStore,
    private val onboarding: AppOnboardingDataStore,
    private val collections: CollectionsDataStore,
    private val addons: AddonRepository,
    private val tmdbSettings: TmdbSettingsDataStore,
    private val streamBadges: StreamBadgeSettingsDataStore
) {
    suspend fun prepare() = withContext(Dispatchers.IO) {
        if (!BuildConfig.TRIAL_ENABLED) return@withContext
        val marker = context.getSharedPreferences("epislate_trial_preset", Context.MODE_PRIVATE)
        // Independent one-time migration: upgrades must not re-import collections,
        // reset the selected layout/profile, or touch watched/progress stores.
        if (!marker.getBoolean("tmdb_enrichment_v1_applied", false)) {
            tmdbSettings.setEnabled(true)
            check(marker.edit().putBoolean("tmdb_enrichment_v1_applied", true).commit())
        }
        if (!marker.getBoolean("stream_badges_v1_applied", false)) {
            val rules = context.assets.open("trial-badge-rules.json").bufferedReader().use {
                Json.decodeFromString<StreamBadgeRules>(it.readText()).normalized()
            }
            require(rules.hasImport && rules.enabledFilterCount > 0)
            // Explicit primary Trial profile avoids active-profile startup races.
            streamBadges.setStreamBadgeRules(rules, profileId = 1)
            check(marker.edit().putBoolean("stream_badges_v1_applied", true).commit())
        }
        if (!marker.getBoolean("addon_logo_hidden_v1_applied", false)) {
            // Apply once to the primary Trial profile without resetting its history or choices.
            streamBadges.setShowAddonLogo(false, profileId = 1)
            check(marker.edit().putBoolean("addon_logo_hidden_v1_applied", true).commit())
        }
        if (!marker.getBoolean("browsing_defaults_v1_applied", false)) {
            layout.setCatalogTypeSuffixEnabled(false)
            layout.setFocusedPosterBackdropExpandEnabled(false)
            check(marker.edit().putBoolean("browsing_defaults_v1_applied", true).commit())
        }
        if (marker.getBoolean("v1_applied", false)) return@withContext
        val preset = context.assets.open("trial-preset.json").bufferedReader().use { JSONObject(it.readText()) }
        val urls = preset.getJSONArray("addonUrls")
        for (i in 0 until urls.length()) {
            val url = urls.getString(i).removeSuffix("/manifest.json")
            require(addons.fetchAddon(url) is NetworkResult.Success) { "Preset addon unavailable" }
            addons.addAddon(url)
        }
        // Primary profile is initialized directly; no cloud login or shared watched data.
        profiles.upsertProfile(UserProfile(id = 1, name = "Trial", avatarColorHex = "#1E88E5"))
        profiles.setActiveProfile(1)
        profiles.setRememberLastProfileEnabled(false)
        val imported = collections.importFromJson(preset.getJSONArray("collections").toString())
        require(imported.isNotEmpty())
        collections.setCollections(imported)
        layout.setLayout(HomeLayout.MODERN)
        layout.setSidebarCollapsedByDefault(true)
        layout.setHomeCatalogOrderKeys(imported.map { homeCollectionKey(it.id) })
        val hidden = preset.getJSONArray("hiddenHomeCatalogs")
        layout.setDisabledHomeCatalogKeys((0 until hidden.length()).map { i ->
            val item = hidden.getJSONObject(i)
            homeCatalogKey("aio-metadata", item.getString("type"), item.getString("id"))
        })
        experience.setMode(ExperienceMode.ADVANCED)
        experience.setAddonSetupSkipped(true)
        onboarding.setHasSeenAuthQrOnFirstLaunch(true)
        check(marker.edit().putBoolean("v1_applied", true).commit())
    }
}

@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
fun TrialPresetGate(preset: TrialPreset, content: @Composable () -> Unit) {
    if (!BuildConfig.TRIAL_ENABLED) { content(); return }
    var attempt by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        failed = false
        try { preset.prepare(); ready = true }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    if (ready) content() else Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(if (failed) "Unable to load the trial setup. Check your internet connection." else "Preparing your epiSLATE trial…")
        if (failed) { Spacer(Modifier.height(20.dp)); Button(onClick = { attempt++ }) { Text("Retry") } }
    }
}

object TrialContentPolicy {
    const val DISCOVER = "collection-bc-9d385ff48f9d4204-2"
    private val locked = setOf("folder-bc-734b38fd11b8", "b466d553-3d52-4957-8f81-571aeb9cc295", "folder-bc-32ca7bd381b2", "folder-b790c003")
    fun locked(collectionId: String, folderId: String) = BuildConfig.TRIAL_ENABLED && collectionId == DISCOVER && folderId in locked
}

object TrialAdminRequest {
    val revision = kotlinx.coroutines.flow.MutableStateFlow(0)
}
