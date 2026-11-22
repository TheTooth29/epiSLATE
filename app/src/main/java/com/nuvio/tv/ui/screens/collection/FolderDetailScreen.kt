package com.nuvio.tv.ui.screens.collection

import com.nuvio.tv.ui.theme.NuvioTheme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.nuvio.tv.ui.util.contentTextDirection
import com.nuvio.tv.ui.util.dpadRepeatThrottle
import com.nuvio.tv.ui.util.dpadVerticalFastScroll
import com.nuvio.tv.ui.util.localizedContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.domain.model.FolderViewMode
import com.nuvio.tv.domain.model.HomeLayout
import com.nuvio.tv.ui.components.CatalogRowSection
import com.nuvio.tv.ui.components.ContentCard
import com.nuvio.tv.ui.components.GridContentCard
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.R
import androidx.compose.ui.res.stringResource
import com.nuvio.tv.ui.components.PosterCardDefaults
import com.nuvio.tv.ui.components.PosterCardStyle
import com.nuvio.tv.ui.components.LocalCardDepthStyle
import com.nuvio.tv.ui.components.LocalLandscapePosterMode
import com.nuvio.tv.ui.components.nuvioCardDepth
import com.nuvio.tv.domain.model.CardDepthSurface
import com.nuvio.tv.ui.screens.home.ClassicFocusArtwork
import com.nuvio.tv.ui.screens.home.ClassicFocusGradientBackdrop
import com.nuvio.tv.ui.screens.home.ClassicHomeContent
import com.nuvio.tv.ui.screens.home.toClassicFocusArtwork
import com.nuvio.tv.ui.screens.home.ContinueWatchingItem
import com.nuvio.tv.ui.screens.home.GridHomeContent
import com.nuvio.tv.ui.screens.home.HeroBackdropState
import com.nuvio.tv.ui.screens.home.HomeScreenFocusState
import com.nuvio.tv.ui.screens.home.key
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.ui.screens.home.ModernHomeContent
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun FolderDetailScreen(
    viewModel: FolderDetailViewModel = hiltViewModel(),
    onNavigateToDetail: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rowsFocusState by viewModel.rowsFocusState.collectAsStateWithLifecycle()
    val followLayoutFocusState by viewModel.followLayoutFocusState.collectAsStateWithLifecycle()
    val tabFocusStates by viewModel.tabFocusStates.collectAsStateWithLifecycle()
    val folder = uiState.folder

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        return
    }

    if (folder == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.folder_detail_not_found), color = NuvioTheme.colors.TextSecondary)
        }
        return
    }

    val isItemWatched: (MetaPreview) -> Boolean = remember(uiState.movieWatchedStatus) {
        { item -> uiState.movieWatchedStatus[com.nuvio.tv.ui.screens.home.homeItemStatusKey(item.id, item.apiType)] == true }
    }

    val enrichingItemId by viewModel.enrichingItemId.collectAsStateWithLifecycle()
    val enrichedPreviews by viewModel.enrichedPreviews.collectAsStateWithLifecycle()
    val failedEnrichmentIds by viewModel.failedEnrichmentIds.collectAsStateWithLifecycle()
    val trailerPreviewUrls by viewModel.trailerPreviewUrls.collectAsStateWithLifecycle()
    val trailerPreviewAudioUrls by viewModel.trailerPreviewAudioUrls.collectAsStateWithLifecycle()
    val scrollToTopTrigger by viewModel.scrollToTopTrigger.collectAsStateWithLifecycle()

    if (uiState.viewMode == FolderViewMode.FOLLOW_LAYOUT) {
        FollowLayoutContent(
            uiState = uiState,
            focusState = followLayoutFocusState,
            enrichingItemId = enrichingItemId,
            enrichedPreviews = enrichedPreviews,
            failedEnrichmentIds = failedEnrichmentIds,
            onNavigateToDetail = onNavigateToDetail,
            onLoadMoreCatalog = viewModel::loadMoreForCatalog,
            onSelectTab = viewModel::selectTab,
            loadUsDigitalDate = viewModel::usDigitalMovieDate,
            onLoadMoreForSelectedTab = { viewModel.loadMoreItems(viewModel.uiState.value.selectedTabIndex) },
            onSaveFocusState = { vi, vo, rk, ikm, m, ma, ri, ii ->
                viewModel.saveFollowLayoutFocusState(vi, vo, rk, ikm, m, ma, ri, ii)
            },
            onItemFocus = viewModel::onItemFocused,
            onPreloadAdjacentItem = viewModel::preloadAdjacentItem,
            onCatalogItemLongPress = { item, addonBaseUrl ->
                viewModel.posterOptions.show(item, addonBaseUrl)
            },
            trailerPreviewUrls = trailerPreviewUrls,
            trailerPreviewAudioUrls = trailerPreviewAudioUrls,
            onRequestTrailerPreview = viewModel::requestTrailerPreview,
            onFocusedRowKeyChanged = viewModel::onFocusedRowChanged,
            scrollToTopTrigger = scrollToTopTrigger
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = NuvioTheme.spacing.xl)
        ) {
            when (uiState.viewMode) {
                FolderViewMode.TABBED_GRID -> TabbedGridContent(
                    uiState = uiState,
                    folder = folder,
                    tabFocusState = tabFocusStates[uiState.selectedTabIndex] ?: FolderDetailGridFocusState(),
                    onSelectTab = viewModel::selectTab,
                    loadUsDigitalDate = viewModel::usDigitalMovieDate,
                    onNavigateToDetail = onNavigateToDetail,
                    isItemWatched = isItemWatched,
                    onLoadMore = { viewModel.loadMoreItems(uiState.selectedTabIndex) },
                    onSaveFocusState = { verticalIndex, verticalOffset, focusedItemKey ->
                        viewModel.saveTabFocusState(
                            tabIndex = uiState.selectedTabIndex,
                            verticalScrollIndex = verticalIndex,
                            verticalScrollOffset = verticalOffset,
                            focusedItemKey = focusedItemKey
                        )
                    },
                    onItemLongPress = { item, addonBaseUrl ->
                        viewModel.posterOptions.show(item, addonBaseUrl)
                    }
                )
                FolderViewMode.ROWS -> {
                    FolderHeader(folder = folder)
                    RowsContent(
                        uiState = uiState,
                        focusState = rowsFocusState,
                        onNavigateToDetail = onNavigateToDetail,
                        isItemWatched = isItemWatched,
                        onLoadMoreCatalog = viewModel::loadMoreForCatalog,
                        onSaveFocusState = { vi, vo, rk, ikm, m, ma, ri, ii ->
                            viewModel.saveRowsFocusState(vi, vo, rk, ikm, m, ma, ri, ii)
                        },
                        onItemFocus = viewModel::onItemFocused,
                        onItemLongPress = { item, addonBaseUrl ->
                            viewModel.posterOptions.show(item, addonBaseUrl)
                        }
                    )
                }
                FolderViewMode.FOLLOW_LAYOUT -> {} // handled above
            }
        }
    }

    val posterOptionsState by viewModel.posterOptions.state.collectAsStateWithLifecycle()
    com.nuvio.tv.ui.components.posteroptions.PosterOptionsHost(
        state = posterOptionsState,
        controller = viewModel.posterOptions,
        onNavigateToDetail = { id, type, addonBaseUrl ->
            onNavigateToDetail(id, type, addonBaseUrl)
        }
    )
}

@Composable
private fun FolderHeader(folder: com.nuvio.tv.domain.model.CollectionFolder) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NuvioTheme.spacing.xxxl, vertical = NuvioTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.lg)
    ) {
        if (!folder.coverImageUrl.isNullOrBlank()) {
            val iconWidth: androidx.compose.ui.unit.Dp
            val iconHeight: androidx.compose.ui.unit.Dp
            when (folder.tileShape) {
                com.nuvio.tv.domain.model.PosterShape.POSTER -> { iconWidth = NuvioTheme.spacing.xxl; iconHeight = NuvioTheme.spacing.xxxl }
                com.nuvio.tv.domain.model.PosterShape.LANDSCAPE -> { iconWidth = 64.dp; iconHeight = 36.dp }
                com.nuvio.tv.domain.model.PosterShape.SQUARE -> { iconWidth = NuvioTheme.spacing.xxxl; iconHeight = NuvioTheme.spacing.xxxl }
            }
            AsyncImage(
                model = folder.coverImageUrl,
                contentDescription = folder.title,
                modifier = Modifier
                    .width(iconWidth)
                    .height(iconHeight)
                    .clip(RoundedCornerShape(NuvioTheme.radii.sm)),
                contentScale = ContentScale.FillBounds
            )
        } else if (!folder.coverEmoji.isNullOrBlank()) {
            Text(
                text = folder.coverEmoji,
                style = MaterialTheme.typography.headlineLarge
            )
        }
        Text(
            text = folder.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                textDirection = folder.title.contentTextDirection()
            ),
            color = NuvioTheme.colors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun TabbedGridContent(
    uiState: FolderDetailUiState,
    folder: com.nuvio.tv.domain.model.CollectionFolder,
    tabFocusState: FolderDetailGridFocusState,
    onSelectTab: (Int) -> Unit,
    onNavigateToDetail: (String, String, String) -> Unit,
    onSaveFocusState: (Int, Int, String?) -> Unit,
    onLoadMore: () -> Unit = {},
    isItemWatched: (MetaPreview) -> Boolean = { false },
    loadUsDigitalDate: (suspend (String) -> String?)? = null,
    onItemLongPress: (MetaPreview, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    if (!TabbedGridPreviewStore.isTestPackage(context.packageName) || uiState.viewMode != FolderViewMode.TABBED_GRID) {
        OriginalTabbedGridContent(uiState, folder, tabFocusState, onSelectTab, onNavigateToDetail,
            onSaveFocusState, onLoadMore, isItemWatched, loadUsDigitalDate, onItemLongPress)
        return
    }
    // Save only a detail-navigation intent in this back-stack entry. A new folder visit has none.
    var pendingDetailTab by rememberSaveable(folder.id) { mutableIntStateOf(-1) }
    val detailReturnTab = remember(folder.id) { pendingDetailTab }
    val restoreTrialPoster = com.nuvio.tv.BuildConfig.TRIAL_ENABLED &&
        pendingDetailTab >= 0 && detailReturnTab == uiState.selectedTabIndex
    val detailNavigate: (String, String, String) -> Unit = { id, type, addon ->
        if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) pendingDetailTab = uiState.selectedTabIndex
        onNavigateToDetail(id, type, addon)
    }
    val store = remember(context) { TabbedGridPreviewStore.get(context) }
    val mode by store.mode.collectAsStateWithLifecycle()
    var tabRestored by remember(folder.id, mode) { mutableStateOf(false) }
    LaunchedEffect(folder.id, mode, uiState.tabs.size) {
        if (!tabRestored && uiState.tabs.isNotEmpty()) {
            onSelectTab(if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED)
                com.nuvio.tv.core.trial.TrialFolderFocusPolicy.initialTab(detailReturnTab, uiState.tabs.size) else
                store.selectedTab(folder.id, mode, uiState.selectedTabIndex).coerceIn(uiState.tabs.indices))
            tabRestored = true
        }
    }
    if (!tabRestored) return
    androidx.compose.runtime.SideEffect { store.saveSelectedTab(folder.id, mode, uiState.selectedTabIndex) }
    val tab = uiState.tabs.getOrNull(uiState.selectedTabIndex)
    val positionKey = remember(folder.id, tab, uiState.selectedTabIndex, mode) {
        org.json.JSONArray(listOf(folder.id, uiState.selectedTabIndex, tab?.label, tab?.source.toString(), mode.name)).toString()
    }
    val saved = remember(positionKey) { store.position(positionKey) }
    androidx.compose.runtime.key(mode, folder.id) {
        val latestSave by rememberUpdatedState<(Int, Int, String?) -> Unit> { index, offset, focus ->
            store.save(positionKey, index, offset, focus)
            if (mode == TabbedGridPreview.ORIGINAL) onSaveFocusState(index, offset, focus)
        }
        val save: (Int, Int, String?) -> Unit = { index, offset, focus -> latestSave(index, offset, focus) }
        if (mode == TabbedGridPreview.ORIGINAL) {
            OriginalTabbedGridContent(uiState, folder,
                if (saved.hasSavedFocus) saved else tabFocusState,
                onSelectTab, detailNavigate, save, onLoadMore, isItemWatched,
                loadUsDigitalDate, onItemLongPress,
                restoreTrialPoster = restoreTrialPoster,
                skipTrialHeadingOnEntry = detailReturnTab >= 0,
                onTrialPosterRestored = { pendingDetailTab = -1 })
        } else {
            PreviewTabbedGridContent(uiState, folder, saved, onSelectTab, detailNavigate,
                { index, offset, focus -> store.save(positionKey, index, offset, focus) },
                onLoadMore, isItemWatched, loadUsDigitalDate, onItemLongPress, mode,
                restoreTrialPoster = restoreTrialPoster,
                skipTrialHeadingOnEntry = detailReturnTab >= 0,
                onTrialPosterRestored = { pendingDetailTab = -1 })
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun OriginalTabbedGridContent(
    uiState: FolderDetailUiState,
    folder: com.nuvio.tv.domain.model.CollectionFolder,
    tabFocusState: FolderDetailGridFocusState,
    onSelectTab: (Int) -> Unit,
    onNavigateToDetail: (String, String, String) -> Unit,
    onSaveFocusState: (Int, Int, String?) -> Unit,
    onLoadMore: () -> Unit = {},
    isItemWatched: (MetaPreview) -> Boolean = { false },
    loadUsDigitalDate: (suspend (String) -> String?)? = null,
    onItemLongPress: (MetaPreview, String) -> Unit = { _, _ -> },
    restoreTrialPoster: Boolean = false,
    skipTrialHeadingOnEntry: Boolean = false,
    onTrialPosterRestored: () -> Unit = {}
) {
    val tabFocusRequesters = remember(uiState.tabs.size) { uiState.tabs.indices.map { FocusRequester() } }
    var gridHasFocus by remember { mutableStateOf(false) }
    var gridScrollToTopTrigger by remember { mutableIntStateOf(0) }
    var firstPosterRequest by remember { mutableIntStateOf(0) }
    val posterEntry = remember(folder.id) { com.nuvio.tv.core.trial.TrialGridEntryPolicy() }
    val gridEntryFocus = remember { FocusRequester() }
    LaunchedEffect(folder.id, uiState.tabs.isNotEmpty()) {
        if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && !skipTrialHeadingOnEntry && uiState.tabs.isNotEmpty()) {
            onSelectTab(0)
            withFrameNanos { }
            runCatching { tabFocusRequesters.first().requestFocus() }
        }
    }

    // Grid Back: focus on grid -> move focus to active tab (grid stays scrolled).
    BackHandler(enabled = gridHasFocus && (uiState.tabs.size > 1 || (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && uiState.tabs.isNotEmpty()))) {
        tabFocusRequesters.getOrNull(uiState.selectedTabIndex)?.let { runCatching { it.requestFocus() } }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = NuvioTheme.spacing.xxxl, end = NuvioTheme.spacing.xxxl, top = NuvioTheme.spacing.sm, bottom = NuvioTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
    ) {
        Text(
            text = folder.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                textDirection = folder.title.contentTextDirection()
            ),
            color = NuvioTheme.colors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 300.dp)
        )

        if (uiState.tabs.size > 1 || (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && uiState.tabs.isNotEmpty())) {
            TabRow(
                selectedTabIndex = uiState.selectedTabIndex,
                indicator = { positions, focused ->
                    positions.getOrNull(uiState.selectedTabIndex)?.let { position ->
                        TabRowDefaults.PillIndicator(position, focused,
                            activeColor = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED)
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.onSurface)
                    }
                },
                modifier = Modifier
                    .focusRestorer {
                        tabFocusRequesters.getOrNull(uiState.selectedTabIndex) ?: FocusRequester.Default
                    }
            ) {
                uiState.tabs.forEachIndexed { index, tab ->
                    Tab(
                        colors = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) TabDefaults.pillIndicatorTabColors(
                            focusedContentColor = Color.White, focusedSelectedContentColor = Color.White,
                            selectedContentColor = NuvioTheme.colors.TextSecondary
                        ) else TabDefaults.pillIndicatorTabColors(),
                        selected = index == uiState.selectedTabIndex,
                        onFocus = {
                            if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) posterEntry.cancel()
                            onSelectTab(index)
                        },
                        onClick = {
                            onSelectTab(index)
                            gridScrollToTopTrigger++
                        },
                        modifier = if (index < tabFocusRequesters.size) {
                            Modifier.focusRequester(tabFocusRequesters[index])
                                .then(if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) Modifier.focusProperties {
                                    left = tabFocusRequesters.getOrNull(index - 1) ?: FocusRequester.Cancel
                                    right = tabFocusRequesters.getOrNull(index + 1) ?: FocusRequester.Cancel
                                    up = FocusRequester.Cancel
                                } else Modifier)
                                .onPreviewKeyEvent { event ->
                                if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED &&
                                    event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                    if (event.type == KeyEventType.KeyDown) {
                                        posterEntry.request(index)
                                        firstPosterRequest++
                                    }
                                    true
                                } else false
                            }
                        } else Modifier
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = NuvioTheme.spacing.lg, vertical = NuvioTheme.spacing.sm),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = if (tab.isAllTab) stringResource(R.string.collections_tab_all) else tab.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (uiState.catalogTypeSuffixEnabled && tab.typeLabel.isNotBlank()) {
                                val localizedType = when {
                                    tab.isAllTab -> stringResource(R.string.collections_tab_combined)
                                    tab.rawType.lowercase() == "movie" -> stringResource(R.string.type_movie)
                                    tab.rawType.lowercase() == "series" -> stringResource(R.string.type_series)
                                    else -> tab.typeLabel
                                }
                                Text(
                                    text = localizedType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NuvioTheme.colors.TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val currentTab = uiState.tabs.getOrNull(uiState.selectedTabIndex)
    if (currentTab == null) return

    when {
        currentTab.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
        currentTab.error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = currentTab.error,
                    color = NuvioTheme.colors.TextSecondary
                )
            }
        }
        currentTab.catalogRow != null -> {
            val items = currentTab.catalogRow.items
            val posterCardStyle = PosterCardStyle(
                width = uiState.posterCardWidthDp.dp,
                height = (uiState.posterCardWidthDp * 1.5f).dp,
                cornerRadius = uiState.posterCardCornerRadiusDp.dp
            )
            val itemFocusRequesters = remember(uiState.selectedTabIndex) { mutableMapOf<String, FocusRequester>() }
            var lastFocusedItemKey by remember(
                uiState.selectedTabIndex,
                tabFocusState.focusedItemKey
            ) {
                mutableStateOf(tabFocusState.focusedItemKey)
            }
            val gridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && !restoreTrialPoster) 0 else tabFocusState.verticalScrollIndex,
                initialFirstVisibleItemScrollOffset = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && !restoreTrialPoster) 0 else tabFocusState.verticalScrollOffset
            )

            // Scroll grid to top when OK is pressed on a tab or tab changes
            LaunchedEffect(gridScrollToTopTrigger, uiState.selectedTabIndex) {
                if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && skipTrialHeadingOnEntry && gridScrollToTopTrigger == 0) return@LaunchedEffect
                if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) {
                    gridState.scrollToItem(0, 0)
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    onSaveFocusState(
                        gridState.firstVisibleItemIndex,
                        gridState.firstVisibleItemScrollOffset,
                        lastFocusedItemKey
                    )
                }
            }

            LaunchedEffect(tabFocusState.verticalScrollIndex, tabFocusState.verticalScrollOffset) {
                if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) return@LaunchedEffect
                val targetIndex = tabFocusState.verticalScrollIndex
                val targetOffset = tabFocusState.verticalScrollOffset
                if (gridState.firstVisibleItemIndex == targetIndex &&
                    gridState.firstVisibleItemScrollOffset == targetOffset
                ) {
                    return@LaunchedEffect
                }
                if (targetIndex > 0 || targetOffset > 0) {
                    gridState.scrollToItem(targetIndex, targetOffset)
                }
            }

            LaunchedEffect(items, tabFocusState.hasSavedFocus, tabFocusState.focusedItemKey) {
                if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) {
                    if (!restoreTrialPoster) return@LaunchedEffect
                    val targetKey = tabFocusState.focusedItemKey ?: return@LaunchedEffect
                    val targetIndex = items.indices.firstOrNull { "${items[it].id}_$it" == targetKey }
                        ?: return@LaunchedEffect
                    gridState.scrollToItem(tabFocusState.verticalScrollIndex, tabFocusState.verticalScrollOffset)
                    repeat(2) { withFrameNanos { } }
                    val requester = itemFocusRequesters[targetKey] ?: return@LaunchedEffect
                    if (runCatching { requester.requestFocus(androidx.compose.ui.focus.FocusDirection.Enter) }.getOrDefault(false)) {
                        lastFocusedItemKey = targetKey
                        onTrialPosterRestored()
                    }
                    return@LaunchedEffect
                }
                val targetKey = tabFocusState.focusedItemKey ?: return@LaunchedEffect
                if (!tabFocusState.hasSavedFocus) return@LaunchedEffect
                val requester = itemFocusRequesters[targetKey] ?: return@LaunchedEffect
                repeat(2) { withFrameNanos { } }
                if (runCatching { requester.requestFocus() }.isSuccess) {
                    lastFocusedItemKey = targetKey
                }
            }

            LaunchedEffect(firstPosterRequest, items.size) {
                if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && posterEntry.targets(uiState.selectedTabIndex) && items.isNotEmpty()) {
                    gridState.scrollToItem(0)
                    repeat(2) { withFrameNanos { } }
                    if (posterEntry.targets(uiState.selectedTabIndex) &&
                        runCatching { gridEntryFocus.requestFocus(androidx.compose.ui.focus.FocusDirection.Enter) }.getOrDefault(false)) {
                        posterEntry.completed(uiState.selectedTabIndex)
                    }
                }
            }
            val catalogRow = currentTab.catalogRow
            LaunchedEffect(gridState, items.size) {
                snapshotFlow {
                    val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val total = gridState.layoutInfo.totalItemsCount
                    lastVisible to total
                }
                    .distinctUntilChanged()
                    .collect { (lastVisible, total) ->
                        if (total > 0 && lastVisible >= total - 10) {
                            if (catalogRow != null && catalogRow.hasMore && !catalogRow.isLoading) {
                                onLoadMore()
                            }
                        }
                    }
            }

            val globalLandscape = LocalLandscapePosterMode.current
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = if (globalLandscape) posterCardStyle.height else posterCardStyle.width),
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { gridHasFocus = it.hasFocus }
                    .then(if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) Modifier else Modifier.focusRestorer {
                        lastFocusedItemKey?.let { itemFocusRequesters[it] } ?: FocusRequester.Default
                    })
                    .dpadRepeatThrottle(),
                contentPadding = PaddingValues(
                    start = NuvioTheme.spacing.xxxl,
                    end = NuvioTheme.spacing.xxxl,
                    top = NuvioTheme.spacing.lg,
                    bottom = NuvioTheme.spacing.xxxl
                ),
                horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.lg)
            ) {
                itemsIndexed(
                    items = items,
                    key = { index, item -> "${item.id}_$index" }
                ) { index, item ->
                    val itemKey = "${item.id}_$index"
                    val focusReq = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && index == 0) gridEntryFocus
                        else itemFocusRequesters.getOrPut(itemKey) { FocusRequester() }
                    itemFocusRequesters[itemKey] = focusReq
                    val useDigitalDate = item.apiType == "movie"
                    val digitalDate by produceState<String?>(null, item.id, useDigitalDate) {
                        if (useDigitalDate) value = loadUsDigitalDate?.invoke(item.id)
                    }
                    GridContentCard(
                        item = item,
                        showNeutralOutline = com.nuvio.tv.BuildConfig.TRIAL_ENABLED,
                        yearLabelOverride = (if (item.apiType == "series") formatGridReleaseDate(item.released) else digitalDate)
                            ?.uppercase(java.util.Locale.US),
                        subtleReleaseDate = true,
                        posterCardStyle = posterCardStyle,
                        focusRequester = focusReq,
                        isWatched = isItemWatched(item),
                        showYearOnly = true,
                        onFocused = { lastFocusedItemKey = itemKey },
                        onClick = {
                            if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) onSaveFocusState(
                                gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset, itemKey)
                            HeroBackdropState.update(item.backdropUrl)
                            onNavigateToDetail(
                                item.id,
                                item.apiType,
                                currentTab.catalogRow.addonBaseUrl
                            )
                        },
                        onLongPress = {
                            lastFocusedItemKey = itemKey
                            onItemLongPress(item, currentTab.catalogRow.addonBaseUrl)
                        }
                    )
                }
                if (catalogRow != null && catalogRow.isLoading) {
                    item(
                        key = "loading_more"
                    ) {
                        val cardShape = RoundedCornerShape(posterCardStyle.cornerRadius)
                        val cardDepthStyle = LocalCardDepthStyle.current
                        val globalLandscape = LocalLandscapePosterMode.current
                        val loadingCardHeight = if (globalLandscape) {
                            posterCardStyle.width / com.nuvio.tv.domain.model.PosterShape.LANDSCAPE.aspectRatio()
                        } else {
                            posterCardStyle.width * 1.5f
                        }
                        Column(
                            modifier = Modifier.width(posterCardStyle.width)
                        ) {
                            Card(
                                onClick = {},
                                modifier = Modifier
                                    .width(posterCardStyle.width)
                                    .height(loadingCardHeight)
                                    .focusProperties { canFocus = false },
                                shape = CardDefaults.shape(shape = cardShape),
                                colors = CardDefaults.colors(
                                    containerColor = NuvioTheme.colors.BackgroundCard,
                                    focusedContainerColor = NuvioTheme.colors.BackgroundCard
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(cardShape)
                                        .nuvioCardDepth(
                                            shape = cardShape,
                                            surface = CardDepthSurface.POSTERS,
                                            style = cardDepthStyle
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator()
                                }
                            }
                            // Reserve space for title + release date to match ContentCard height
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = NuvioTheme.spacing.sm)
                                    .height(
                                        MaterialTheme.typography.titleMedium.lineHeight.value.dp +
                                            MaterialTheme.typography.labelMedium.lineHeight.value.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun PreviewTabbedGridContent(
    uiState: FolderDetailUiState,
    folder: com.nuvio.tv.domain.model.CollectionFolder,
    tabFocusState: FolderDetailGridFocusState,
    onSelectTab: (Int) -> Unit,
    onNavigateToDetail: (String, String, String) -> Unit,
    onSaveFocusState: (Int, Int, String?) -> Unit,
    onLoadMore: () -> Unit = {},
    isItemWatched: (MetaPreview) -> Boolean = { false },
    loadUsDigitalDate: (suspend (String) -> String?)? = null,
    onItemLongPress: (MetaPreview, String) -> Unit = { _, _ -> },
    mode: TabbedGridPreview,
    restoreTrialPoster: Boolean = false,
    skipTrialHeadingOnEntry: Boolean = false,
    onTrialPosterRestored: () -> Unit = {}
) {
    val tabFocusRequesters = remember(uiState.tabs.size) { uiState.tabs.indices.map { FocusRequester() } }
    var gridHasFocus by remember { mutableStateOf(false) }
    var gridScrollToTopTrigger by remember { mutableIntStateOf(0) }
    var firstPosterRequest by remember { mutableIntStateOf(0) }
    val posterEntry = remember(folder.id) { com.nuvio.tv.core.trial.TrialGridEntryPolicy() }
    val gridEntryFocus = remember { FocusRequester() }
    LaunchedEffect(folder.id, uiState.tabs.isNotEmpty()) {
        if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && !skipTrialHeadingOnEntry && uiState.tabs.isNotEmpty()) {
            onSelectTab(0)
            withFrameNanos { }
            runCatching { tabFocusRequesters.first().requestFocus() }
        }
    }

    // Grid Back: focus on grid -> move focus to active tab (grid stays scrolled).
    BackHandler(enabled = gridHasFocus && (uiState.tabs.size > 1 || (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && uiState.tabs.isNotEmpty()))) {
        tabFocusRequesters.getOrNull(uiState.selectedTabIndex)?.let { runCatching { it.requestFocus() } }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = NuvioTheme.spacing.xxxl, end = NuvioTheme.spacing.xxxl, top = NuvioTheme.spacing.sm, bottom = NuvioTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
    ) {
        Text(
            text = folder.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                textDirection = folder.title.contentTextDirection()
            ),
            color = NuvioTheme.colors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 300.dp)
        )

        if (uiState.tabs.size > 1 || (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && uiState.tabs.isNotEmpty())) {
            TabRow(
                selectedTabIndex = uiState.selectedTabIndex,
                indicator = { positions, focused ->
                    positions.getOrNull(uiState.selectedTabIndex)?.let { position ->
                        TabRowDefaults.PillIndicator(position, focused,
                            activeColor = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED)
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.onSurface)
                    }
                },
                modifier = Modifier
                    .focusRestorer {
                        tabFocusRequesters.getOrNull(uiState.selectedTabIndex) ?: FocusRequester.Default
                    }
            ) {
                uiState.tabs.forEachIndexed { index, tab ->
                    Tab(
                        colors = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) TabDefaults.pillIndicatorTabColors(
                            focusedContentColor = Color.White, focusedSelectedContentColor = Color.White,
                            selectedContentColor = NuvioTheme.colors.TextSecondary
                        ) else TabDefaults.pillIndicatorTabColors(),
                        selected = index == uiState.selectedTabIndex,
                        onFocus = {
                            if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) posterEntry.cancel()
                            onSelectTab(index)
                        },
                        onClick = {
                            onSelectTab(index)
                            gridScrollToTopTrigger++
                        },
                        modifier = if (index < tabFocusRequesters.size) {
                            Modifier.focusRequester(tabFocusRequesters[index])
                                .then(if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) Modifier.focusProperties {
                                    left = tabFocusRequesters.getOrNull(index - 1) ?: FocusRequester.Cancel
                                    right = tabFocusRequesters.getOrNull(index + 1) ?: FocusRequester.Cancel
                                    up = FocusRequester.Cancel
                                } else Modifier)
                                .onPreviewKeyEvent { event ->
                                if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED &&
                                    event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                    if (event.type == KeyEventType.KeyDown) {
                                        posterEntry.request(index)
                                        firstPosterRequest++
                                    }
                                    true
                                } else false
                            }
                        } else Modifier
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = NuvioTheme.spacing.lg, vertical = NuvioTheme.spacing.sm),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = if (tab.isAllTab) stringResource(R.string.collections_tab_all) else tab.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (uiState.catalogTypeSuffixEnabled && tab.typeLabel.isNotBlank()) {
                                val localizedType = when {
                                    tab.isAllTab -> stringResource(R.string.collections_tab_combined)
                                    tab.rawType.lowercase() == "movie" -> stringResource(R.string.type_movie)
                                    tab.rawType.lowercase() == "series" -> stringResource(R.string.type_series)
                                    else -> tab.typeLabel
                                }
                                Text(
                                    text = localizedType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NuvioTheme.colors.TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val currentTab = uiState.tabs.getOrNull(uiState.selectedTabIndex)
    if (currentTab == null) return

    when {
        currentTab.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
        currentTab.error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = currentTab.error,
                    color = NuvioTheme.colors.TextSecondary
                )
            }
        }
        currentTab.catalogRow != null -> {
            val items = currentTab.catalogRow.items
            androidx.compose.runtime.key(uiState.selectedTabIndex) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val gap = NuvioTheme.spacing.lg
            val inset = NuvioTheme.spacing.xxxl
            val density = androidx.compose.ui.platform.LocalDensity.current
            val dateTopPadding = 10.dp
            val dateHeight = with(density) { MaterialTheme.typography.labelMedium.lineHeight.toDp() }
            val geometry = tabbedGridPreviewGeometry(
                mode, maxWidth.value, maxHeight.value, (dateTopPadding + dateHeight).value,
                LocalLandscapePosterMode.current, inset.value, gap.value
            )
            val rowHeight = geometry.rowHeight.dp
            val cardWidth = geometry.cardWidth.dp
            val posterCardStyle = PosterCardStyle(
                width = cardWidth.coerceAtLeast(24.dp),
                height = (cardWidth * 1.5f).coerceAtLeast(36.dp),
                cornerRadius = uiState.posterCardCornerRadiusDp.dp,
                focusedScale = 1.03f
            )
            val itemFocusRequesters = remember(uiState.selectedTabIndex) { mutableMapOf<String, FocusRequester>() }
            var lastFocusedItemKey by remember(
                uiState.selectedTabIndex,
                tabFocusState.focusedItemKey
            ) {
                mutableStateOf(tabFocusState.focusedItemKey)
            }
            val gridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && !restoreTrialPoster) 0 else tabFocusState.verticalScrollIndex,
                initialFirstVisibleItemScrollOffset = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && !restoreTrialPoster) 0 else tabFocusState.verticalScrollOffset
            )

            var focusedIndex by remember { mutableIntStateOf(-1) }
            var firstRow by remember { mutableIntStateOf(
                if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && !restoreTrialPoster) 0 else tabFocusState.verticalScrollIndex / mode.columns) }
            LaunchedEffect(focusedIndex) {
                if (focusedIndex >= 0) {
                    val target = mode.firstRow(focusedIndex / mode.columns, firstRow)
                    firstRow = target
                    gridState.animateScrollToItem(target * mode.columns)
                }
            }
            LaunchedEffect(gridScrollToTopTrigger) {
                if (gridScrollToTopTrigger > 0) {
                    firstRow = 0
                    gridState.scrollToItem(0)
                }
            }
            DisposableEffect(Unit) {
                onDispose {
                    onSaveFocusState(
                        gridState.firstVisibleItemIndex,
                        gridState.firstVisibleItemScrollOffset,
                        lastFocusedItemKey
                    )
                }
            }

            LaunchedEffect(tabFocusState.verticalScrollIndex, tabFocusState.verticalScrollOffset) {
                if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) return@LaunchedEffect
                val targetIndex = tabFocusState.verticalScrollIndex
                val targetOffset = tabFocusState.verticalScrollOffset
                if (gridState.firstVisibleItemIndex == targetIndex &&
                    gridState.firstVisibleItemScrollOffset == targetOffset
                ) {
                    return@LaunchedEffect
                }
                if (targetIndex > 0 || targetOffset > 0) {
                    gridState.scrollToItem(targetIndex, targetOffset)
                }
            }

            var focusRestored by remember { mutableStateOf(false) }
            LaunchedEffect(items, tabFocusState.hasSavedFocus, tabFocusState.focusedItemKey) {
                if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) {
                    if (!restoreTrialPoster) return@LaunchedEffect
                    val targetKey = tabFocusState.focusedItemKey ?: return@LaunchedEffect
                    val targetIndex = items.indices.firstOrNull { "${items[it].id}_$it" == targetKey }
                        ?: return@LaunchedEffect
                    firstRow = mode.firstRow(targetIndex / mode.columns, tabFocusState.verticalScrollIndex / mode.columns)
                    gridState.scrollToItem(firstRow * mode.columns, tabFocusState.verticalScrollOffset)
                    repeat(2) { withFrameNanos { } }
                    val requester = itemFocusRequesters[targetKey] ?: return@LaunchedEffect
                    if (runCatching { requester.requestFocus(androidx.compose.ui.focus.FocusDirection.Enter) }.getOrDefault(false)) {
                        lastFocusedItemKey = targetKey
                        onTrialPosterRestored()
                    }
                    return@LaunchedEffect
                }
                if (focusRestored) return@LaunchedEffect
                val targetKey = tabFocusState.focusedItemKey ?: return@LaunchedEffect
                if (!tabFocusState.hasSavedFocus) return@LaunchedEffect
                val requester = itemFocusRequesters[targetKey] ?: return@LaunchedEffect
                repeat(2) { withFrameNanos { } }
                if (runCatching { requester.requestFocus() }.isSuccess) {
                    lastFocusedItemKey = targetKey
                    focusRestored = true
                }
            }

            LaunchedEffect(firstPosterRequest, items.size) {
                if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && posterEntry.targets(uiState.selectedTabIndex) && items.isNotEmpty()) {
                    gridState.scrollToItem(0)
                    repeat(2) { withFrameNanos { } }
                    if (posterEntry.targets(uiState.selectedTabIndex) &&
                        runCatching { gridEntryFocus.requestFocus(androidx.compose.ui.focus.FocusDirection.Enter) }.getOrDefault(false)) {
                        posterEntry.completed(uiState.selectedTabIndex)
                    }
                }
            }
            val catalogRow = currentTab.catalogRow
            LaunchedEffect(gridState, items.size) {
                snapshotFlow {
                    val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val total = gridState.layoutInfo.totalItemsCount
                    lastVisible to total
                }
                    .distinctUntilChanged()
                    .collect { (lastVisible, total) ->
                        if (total > 0 && lastVisible >= total - 10) {
                            if (catalogRow != null && catalogRow.hasMore && !catalogRow.isLoading) {
                                onLoadMore()
                            }
                        }
                    }
            }

            val pageRemainder = ((items.size + mode.columns - 1) / mode.columns) % mode.rows
            val pageBottomPadding = if (mode == TabbedGridPreview.PAGES && pageRemainder != 0)
                (rowHeight + gap) * (mode.rows - pageRemainder) else 0.dp
            val noAutomaticScroll = remember {
                object : androidx.compose.foundation.gestures.BringIntoViewSpec {
                    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) = 0f
                }
            }
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides noAutomaticScroll
            ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(mode.columns),
                modifier = Modifier
                    .height(geometry.gridHeight.dp)
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .onFocusChanged { gridHasFocus = it.hasFocus }
                    .then(if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) Modifier else Modifier.focusRestorer {
                        lastFocusedItemKey?.let { itemFocusRequesters[it] } ?: FocusRequester.Default
                    })
                    .dpadRepeatThrottle(),
                contentPadding = PaddingValues(
                    start = NuvioTheme.spacing.xxxl,
                    end = NuvioTheme.spacing.xxxl,
                    top = 12.dp,
                    bottom = 12.dp + pageBottomPadding
                ),
                horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.lg)
            ) {
                itemsIndexed(
                    items = items,
                    key = { index, item -> "${item.id}_$index" }
                ) { index, item ->
                    val itemKey = "${item.id}_$index"
                    val focusReq = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && index == 0) gridEntryFocus
                        else itemFocusRequesters.getOrPut(itemKey) { FocusRequester() }
                    itemFocusRequesters[itemKey] = focusReq
                    val useDigitalDate = item.apiType == "movie"
                    val digitalDate by produceState<String?>(null, item.id, useDigitalDate) {
                        if (useDigitalDate) value = loadUsDigitalDate?.invoke(item.id)
                    }
                    GridContentCard(
                        modifier = Modifier.height(rowHeight),
                        showNeutralOutline = com.nuvio.tv.BuildConfig.TRIAL_ENABLED,
                        item = item,
                        yearLabelOverride = (if (item.apiType == "series") formatGridReleaseDate(item.released) else digitalDate)
                            ?.uppercase(java.util.Locale.US),
                        subtleReleaseDate = true,
                        releaseLabelTopPadding = dateTopPadding,
                        posterCardStyle = posterCardStyle,
                        focusRequester = focusReq,
                        upFocusRequester = if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED && index < mode.columns)
                            tabFocusRequesters.getOrNull(uiState.selectedTabIndex) else null,
                        isWatched = isItemWatched(item),
                        showYearOnly = true,
                        onFocused = { lastFocusedItemKey = itemKey; focusedIndex = index },
                        onClick = {
                            if (com.nuvio.tv.BuildConfig.TRIAL_ENABLED) onSaveFocusState(
                                gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset, itemKey)
                            HeroBackdropState.update(item.backdropUrl)
                            onNavigateToDetail(
                                item.id,
                                item.apiType,
                                currentTab.catalogRow.addonBaseUrl
                            )
                        },
                        onLongPress = {
                            lastFocusedItemKey = itemKey
                            onItemLongPress(item, currentTab.catalogRow.addonBaseUrl)
                        }
                    )
                }
                if (catalogRow != null && catalogRow.isLoading) {
                    item(
                        key = "loading_more"
                    ) {
                        val cardShape = RoundedCornerShape(posterCardStyle.cornerRadius)
                        val cardDepthStyle = LocalCardDepthStyle.current
                        val globalLandscape = LocalLandscapePosterMode.current
                        val loadingCardHeight = if (globalLandscape) {
                            posterCardStyle.width / com.nuvio.tv.domain.model.PosterShape.LANDSCAPE.aspectRatio()
                        } else {
                            posterCardStyle.width * 1.5f
                        }
                        Column(
                            modifier = Modifier.width(posterCardStyle.width).height(rowHeight)
                        ) {
                            Card(
                                onClick = {},
                                modifier = Modifier
                                    .width(posterCardStyle.width)
                                    .height(loadingCardHeight)
                                    .focusProperties { canFocus = false },
                                shape = CardDefaults.shape(shape = cardShape),
                                colors = CardDefaults.colors(
                                    containerColor = NuvioTheme.colors.BackgroundCard,
                                    focusedContainerColor = NuvioTheme.colors.BackgroundCard
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(cardShape)
                                        .nuvioCardDepth(
                                            shape = cardShape,
                                            surface = CardDepthSurface.POSTERS,
                                            style = cardDepthStyle
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator()
                                }
                            }
                            // Reserve space for title + release date to match ContentCard height
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = NuvioTheme.spacing.sm)
                                    .height(
                                        MaterialTheme.typography.titleMedium.lineHeight.value.dp +
                                            MaterialTheme.typography.labelMedium.lineHeight.value.dp
                                    )
                            )
                        }
                    }
                }
            }
            }
            if (mode == TabbedGridPreview.CENTERED) {
                val activeRow = (focusedIndex.takeIf { it >= 0 } ?: (tabFocusState.focusedItemKey
                    ?.substringAfterLast('_')?.toIntOrNull() ?: tabFocusState.verticalScrollIndex)) / mode.columns
                if (activeRow > 0) {
                    androidx.tv.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowUp,
                        contentDescription = "More titles above",
                        tint = NuvioTheme.colors.TextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(24.dp)
                    )
                }
                if ((activeRow + 1) * mode.columns < items.size || catalogRow.hasMore) {
                    androidx.tv.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                        contentDescription = "More titles below",
                        tint = NuvioTheme.colors.TextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).size(24.dp)
                    )
                }
            }
            }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun RowsContent(
    uiState: FolderDetailUiState,
    focusState: HomeScreenFocusState,
    onNavigateToDetail: (String, String, String) -> Unit,
    onLoadMoreCatalog: (String, String, String) -> Unit = { _, _, _ -> },
    onSaveFocusState: (Int, Int, String?, Map<String, String>, Map<String, Int>, Map<String, String>, Int, Int) -> Unit,
    isItemWatched: (MetaPreview) -> Boolean = { false },
    onItemFocus: (MetaPreview) -> Unit = {},
    onItemLongPress: (MetaPreview, String) -> Unit = { _, _ -> },
    posterCardStyle: PosterCardStyle = PosterCardDefaults.Style,
    focusedPosterBackdropExpandEnabled: Boolean = false,
    focusedPosterBackdropExpandDelaySeconds: Int = 3,
    focusedPosterBackdropTrailerEnabled: Boolean = false,
    focusedPosterBackdropTrailerMuted: Boolean = true,
    trailerPreviewUrls: Map<String, String> = emptyMap(),
    trailerPreviewAudioUrls: Map<String, String> = emptyMap()
) {
    val sourceTabs = uiState.tabs.filter { tab ->
        if (tab.isAllTab) return@filter false
        // Hide sources that returned zero results after loading completed
        if (!tab.isLoading && tab.error == null &&
            tab.catalogRow != null && tab.catalogRow.items.isEmpty()
        ) return@filter false
        true
    }
    
    // Nested prefetch: pre-compose cards in nested LazyRows to prevent frame spikes
    val nestedPrefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 2) }
    
    val columnListState = rememberLazyListState(
        initialFirstVisibleItemIndex = focusState.verticalScrollIndex,
        initialFirstVisibleItemScrollOffset = focusState.verticalScrollOffset,
        prefetchStrategy = nestedPrefetchStrategy
    )
    val rowStates = remember { mutableMapOf<String, LazyListState>() }
    val rowFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val rowEntryFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val rowFocusedItemIndex = remember { mutableMapOf<String, Int>() }
    val focusedItemByRow = remember { mutableStateMapOf<String, Int>() }
    val currentFocusedRowKey = remember { mutableStateOf(focusState.focusedRowKey) }
    val folderScope = rememberCoroutineScope()

    // Improved Back: scroll to first item in row before exiting collection
    BackHandler(enabled = run {
        val rowKey = currentFocusedRowKey.value ?: return@run false
        val itemIndex = focusedItemByRow[rowKey] ?: rowFocusedItemIndex[rowKey] ?: 0
        itemIndex > 0
    }) {
        val rowKey = currentFocusedRowKey.value ?: return@BackHandler
        val listState = rowStates[rowKey]
        rowFocusedItemIndex[rowKey] = 0
        focusedItemByRow[rowKey] = 0
        folderScope.launch {
            listState?.scrollToItem(0, 0)
            rowFocusRequesters[rowKey]?.let { runCatching { it.requestFocus() } }
        }
    }

    // Keep a stable ref to the latest sourceTabs so DisposableEffect.onDispose
    // (which captures its closure only once) can read the most recent data.
    val currentSourceTabs by rememberUpdatedState(sourceTabs)

    DisposableEffect(Unit) {
        onDispose {
            val focusedRowKey = currentFocusedRowKey.value
            val itemKeys = mutableMapOf<String, String>()
            currentSourceTabs.forEach { tab ->
                val row = tab.catalogRow
                if (row != null) {
                    val rowKey = row.key()
                    val focusedIdx = rowFocusedItemIndex[rowKey] ?: 0
                    val itemId = row.items.getOrNull(focusedIdx)?.id ?: ""
                    itemKeys[rowKey] = itemId
                }
            }

            onSaveFocusState(
                columnListState.firstVisibleItemIndex,
                columnListState.firstVisibleItemScrollOffset,
                focusedRowKey,
                itemKeys,
                rowStates.mapValues { it.value.firstVisibleItemIndex },
                emptyMap(), // rows here are restored by index
                -1, // rowIndex
                rowFocusedItemIndex[focusedRowKey] ?: 0 // itemIndex — positional fallback
            )
        }
    }

    LaunchedEffect(focusState.hasSavedFocus) {
        if (focusState.hasSavedFocus && focusedItemByRow.isEmpty()) {
            val savedRowKey = focusState.focusedRowKey
            if (savedRowKey != null) {
                val savedItemKey = focusState.focusedItemKeyByRow[savedRowKey]
                sourceTabs.forEach { tab ->
                    val row = tab.catalogRow
                    if (row != null) {
                        val rowKey = row.key()
                        if (rowKey == savedRowKey) {
                            val itemIdx = if (savedItemKey != null) {
                                row.items.indexOfFirst { it.id == savedItemKey }.coerceAtLeast(0)
                            } else 0
                            focusedItemByRow[savedRowKey] = itemIdx
                            rowFocusedItemIndex[savedRowKey] = itemIdx
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(focusState.verticalScrollIndex, focusState.verticalScrollOffset) {
        val targetIndex = focusState.verticalScrollIndex
        val targetOffset = focusState.verticalScrollOffset
        if (columnListState.firstVisibleItemIndex == targetIndex &&
            columnListState.firstVisibleItemScrollOffset == targetOffset
        ) {
            return@LaunchedEffect
        }
        if (targetIndex > 0 || targetOffset > 0) {
            columnListState.scrollToItem(targetIndex, targetOffset)
        }
    }

    val loadMoreLabel = stringResource(R.string.action_load_more)

    LazyColumn(
        state = columnListState,
        modifier = Modifier
            .fillMaxSize()
            .focusRestorer()
            .dpadVerticalFastScroll(
                scrollableState = columnListState,
                resolveVerticalLanding = { sign ->
                    // Pick the item at the leading edge and map to its FocusRequester
                    val layoutInfo = columnListState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    val lastIdx = layoutInfo.totalItemsCount - 1
                    val viewportEnd = layoutInfo.viewportEndOffset
                    val lastItemAtBottom = lastIdx >= 0 &&
                        visibleItems.lastOrNull { it.index == lastIdx }?.let {
                            it.offset + it.size <= viewportEnd
                        } == true
                    val upwardTopItem = if (sign < 0) {
                        visibleItems.firstOrNull()?.takeIf {
                            it.offset > -it.size / 2
                        }
                    } else null
                    val target = when {
                        lastItemAtBottom -> visibleItems.lastOrNull { it.index == lastIdx }
                        upwardTopItem != null -> upwardTopItem
                        else ->
                            visibleItems.firstOrNull { it.offset >= 0 }
                                ?: visibleItems.firstOrNull()
                    }
                    fun requesterForKey(k: String?): FocusRequester? = when {
                        k == null -> null
                        rowEntryFocusRequesters.containsKey(k) -> rowEntryFocusRequesters[k]
                        else -> {
                            val baseKey = k.substringBeforeLast('_')
                            rowEntryFocusRequesters[baseKey]
                        }
                    }
                    val requester = if (target == null) null
                    else requesterForKey(target.key as? String)
                        ?: visibleItems.firstNotNullOfOrNull { requesterForKey(it.key as? String) }

                    requester?.let { req ->
                        folderScope.launch {
                            repeat(6) {
                                val ok = runCatching { req.requestFocus(); true }
                                    .getOrDefault(false)
                                if (ok) return@launch
                                withFrameNanos { }
                            }
                        }
                    }
                    null
                },
            ),
        contentPadding = PaddingValues(top = NuvioTheme.spacing.lg, bottom = NuvioTheme.spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xxl)
    ) {
        sourceTabs.forEachIndexed { index, tab ->
            // Compute rowKey early so we can use it as item key for focus restoration
            val catalogRow = tab.catalogRow
            val rowKey = if (catalogRow != null) {
                catalogRow.key()
            } else {
                "row_${index}_${tab.label}"
            }
            item(key = "${rowKey}_$index") {
                val folderContext = LocalContext.current
                val localizedTypeLabel = remember(tab.rawType, folderContext) {
                    localizedContentType(folderContext, tab.rawType)
                }
                val rowTitle = remember(tab.label, localizedTypeLabel, uiState.catalogTypeSuffixEnabled) {
                    if (uiState.catalogTypeSuffixEnabled && tab.label != tab.typeLabel && localizedTypeLabel.isNotEmpty()) {
                        "${tab.label} - $localizedTypeLabel"
                    } else {
                        tab.label
                    }
                }
                val globalLandscape = LocalLandscapePosterMode.current
                val containerHeight = if (globalLandscape) {
                    posterCardStyle.width / com.nuvio.tv.domain.model.PosterShape.LANDSCAPE.aspectRatio()
                } else {
                    posterCardStyle.height
                }
                when {
                    tab.isLoading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = rowTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                color = NuvioTheme.colors.TextPrimary,
                                modifier = Modifier.padding(start = NuvioTheme.spacing.xxxl, end = NuvioTheme.spacing.xxxl, bottom = NuvioTheme.spacing.md)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(containerHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator()
                            }
                        }
                    }
                    tab.error != null -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = rowTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                color = NuvioTheme.colors.TextPrimary,
                                modifier = Modifier.padding(start = NuvioTheme.spacing.xxxl, end = NuvioTheme.spacing.xxxl, bottom = NuvioTheme.spacing.md)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(containerHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = tab.error, color = NuvioTheme.colors.TextSecondary)
                            }
                        }
                    }
                    catalogRow != null -> {
                        val listState = rowStates.getOrPut(rowKey) {
                            LazyListState(
                                firstVisibleItemIndex = focusState.catalogRowScrollStates[rowKey] ?: 0
                            )
                        }
                        val rowFocusRequester = rowFocusRequesters.getOrPut(rowKey) { FocusRequester() }
                        CatalogRowSection(
                            catalogRow = catalogRow,
                            onItemClick = onNavigateToDetail,
                            onItemLongPress = onItemLongPress,
                            posterCardStyle = posterCardStyle,
                            focusedPosterBackdropExpandEnabled = focusedPosterBackdropExpandEnabled,
                            focusedPosterBackdropExpandDelaySeconds = focusedPosterBackdropExpandDelaySeconds,
                            focusedPosterBackdropTrailerEnabled = focusedPosterBackdropTrailerEnabled,
                            focusedPosterBackdropTrailerMuted = focusedPosterBackdropTrailerMuted,
                            trailerPreviewUrls = trailerPreviewUrls,
                            trailerPreviewAudioUrls = trailerPreviewAudioUrls,
                            onSeeAll = {
                                onLoadMoreCatalog(
                                    catalogRow.catalogId,
                                    catalogRow.addonId,
                                    catalogRow.apiType
                                )
                            },
                            showSeeAll = catalogRow.hasMore && !catalogRow.isLoading,
                            seeAllLabel = loadMoreLabel,
                            showPosterLabels = true,
                            showAddonName = false,
                            showCatalogTypeSuffix = uiState.catalogTypeSuffixEnabled,
                            isItemWatched = isItemWatched,
                            onItemFocus = onItemFocus,
                            listState = listState,
                            rowFocusRequester = rowFocusRequester,
                            entryFocusRequester = rowEntryFocusRequesters.getOrPut(rowKey) { FocusRequester() },
                            enableRowFocusRestorer = true,
                            focusedItemIndex = when {
                                focusState.hasSavedFocus && focusState.focusedRowKey == rowKey ->
                                    focusState.focusedItemIndex
                                !focusState.hasSavedFocus && index == 0 -> 0
                                else -> -1
                            },
                            restorerFocusedIndex = rowFocusedItemIndex[rowKey]
                                ?: if (focusState.hasSavedFocus && focusState.focusedRowKey == rowKey) focusState.focusedItemIndex else -1,
                            onItemFocused = { itemIndex ->
                                currentFocusedRowKey.value = rowKey
                                rowFocusedItemIndex[rowKey] = itemIndex
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowLayoutContent(
    uiState: FolderDetailUiState,
    focusState: HomeScreenFocusState,
    enrichingItemId: String? = null,
    enrichedPreviews: Map<String, MetaPreview> = emptyMap(),
    failedEnrichmentIds: Set<String> = emptySet(),
    onNavigateToDetail: (String, String, String) -> Unit,
    onLoadMoreCatalog: (String, String, String) -> Unit = { _, _, _ -> },
    onSelectTab: (Int) -> Unit = {},
    onLoadMoreForSelectedTab: () -> Unit = {},
    loadUsDigitalDate: (suspend (String) -> String?)? = null,
    onSaveFocusState: (Int, Int, String?, Map<String, String>, Map<String, Int>, Map<String, String>, Int, Int) -> Unit,
    onItemFocus: (MetaPreview) -> Unit = {},
    onPreloadAdjacentItem: (MetaPreview) -> Unit = {},
    onCatalogItemLongPress: (MetaPreview, String) -> Unit = { _, _ -> },
    trailerPreviewUrls: Map<String, String> = emptyMap(),
    trailerPreviewAudioUrls: Map<String, String> = emptyMap(),
    onRequestTrailerPreview: (String, String, String?, String) -> Unit = { _, _, _, _ -> },
    onFocusedRowKeyChanged: (String?) -> Unit = {},
    scrollToTopTrigger: Int = 0
) {
    val homeState = uiState.followLayoutHomeState

    if (homeState == null || (homeState.isLoading && homeState.catalogRows.isEmpty())) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        return
    }

    val posterCardStyle = remember {
        PosterCardStyle(
            width = homeState.posterCardWidthDp.dp,
            height = homeState.posterCardHeightDp.dp,
            cornerRadius = homeState.posterCardCornerRadiusDp.dp
        )
    }
    val noOpCwClick: (ContinueWatchingItem) -> Unit = remember { { } }
    val noOpRemoveCw: (String, Int?, Int?, Boolean) -> Unit = remember { { _, _, _, _ -> } }
    val noOpFolderDetail: (String, String) -> Unit = remember { { _, _ -> } }
    val isItemWatched: (MetaPreview) -> Boolean = remember(homeState.movieWatchedStatus) {
        { item -> homeState.movieWatchedStatus[com.nuvio.tv.ui.screens.home.homeItemStatusKey(item.id, item.apiType)] == true }
    }
    val loadMoreLabel = stringResource(R.string.action_load_more)

    when (uiState.homeLayout) {
        HomeLayout.CLASSIC -> {
            val classicPosterCardStyle = remember(posterCardStyle) {
                val scale = 1.35f // matches CLASSIC_CATALOG_POSTER_SCALE in ClassicHomeContent
                posterCardStyle.copy(
                    width = posterCardStyle.width * scale,
                    height = posterCardStyle.height * scale
                )
            }
            var focusedArtwork by remember { mutableStateOf<ClassicFocusArtwork?>(null) }
            val classicFocusGradientEnabled = homeState.classicFocusGradientEnabled
            val focusedPosterBackdropExpandEnabled = homeState.focusedPosterBackdropExpandEnabled

            LaunchedEffect(classicFocusGradientEnabled) {
                if (!classicFocusGradientEnabled) {
                    focusedArtwork = null
                }
            }

            val handleItemFocus: (MetaPreview) -> Unit = remember(classicFocusGradientEnabled, focusedPosterBackdropExpandEnabled) {
                { item ->
                    if (classicFocusGradientEnabled) {
                        focusedArtwork = item.toClassicFocusArtwork(focusedPosterBackdropExpandEnabled)
                    }
                    onItemFocus(item)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                ClassicFocusGradientBackdrop(
                    artworkProvider = { focusedArtwork },
                    enabled = classicFocusGradientEnabled,
                    modifier = Modifier.fillMaxSize()
                )
                RowsContent(
                    uiState = uiState,
                    focusState = focusState,
                    onNavigateToDetail = onNavigateToDetail,
                    onLoadMoreCatalog = onLoadMoreCatalog,
                    onSaveFocusState = onSaveFocusState,
                    isItemWatched = isItemWatched,
                    onItemFocus = handleItemFocus,
                    onItemLongPress = onCatalogItemLongPress,
                    posterCardStyle = classicPosterCardStyle,
                    focusedPosterBackdropExpandEnabled = focusedPosterBackdropExpandEnabled,
                    focusedPosterBackdropExpandDelaySeconds = homeState.focusedPosterBackdropExpandDelaySeconds,
                    focusedPosterBackdropTrailerEnabled = homeState.focusedPosterBackdropTrailerEnabled,
                    focusedPosterBackdropTrailerMuted = homeState.focusedPosterBackdropTrailerMuted,
                    trailerPreviewUrls = trailerPreviewUrls,
                    trailerPreviewAudioUrls = trailerPreviewAudioUrls
                )
            }
        }
        HomeLayout.GRID -> {
            Column(modifier = Modifier.fillMaxSize()) {
                TabbedGridContent(
                    uiState = uiState,
                    folder = uiState.folder ?: return,
                    tabFocusState = FolderDetailGridFocusState(),
                    onSelectTab = onSelectTab,
                    loadUsDigitalDate = loadUsDigitalDate,
                    onNavigateToDetail = onNavigateToDetail,
                    isItemWatched = isItemWatched,
                    onLoadMore = onLoadMoreForSelectedTab,
                    onSaveFocusState = { _, _, _ -> },
                    onItemLongPress = onCatalogItemLongPress
                )
            }
        }
        HomeLayout.MODERN -> ModernHomeContent(
            fadeCollectionBottomRow = true,
            uiState = homeState,
            modernPresentation = homeState.modernHomePresentation,
            focusState = focusState,
            enrichingItemId = enrichingItemId,
            enrichedPreviews = enrichedPreviews,
            failedEnrichmentIds = failedEnrichmentIds,
            trailerPreviewUrls = trailerPreviewUrls,
            trailerPreviewAudioUrls = trailerPreviewAudioUrls,
            onNavigateToDetail = onNavigateToDetail,
            onContinueWatchingClick = noOpCwClick,
            onRequestTrailerPreview = onRequestTrailerPreview,
            onLoadMoreCatalog = onLoadMoreCatalog,
            onRemoveContinueWatching = noOpRemoveCw,
            isCatalogItemWatched = isItemWatched,
            onCatalogItemLongPress = onCatalogItemLongPress,
            onNavigateToFolderDetail = noOpFolderDetail,
            onItemFocus = onItemFocus,
            onPreloadAdjacentItem = onPreloadAdjacentItem,
            onSaveFocusState = onSaveFocusState,
            onFocusedRowKeyChanged = onFocusedRowKeyChanged,
            scrollToTopTrigger = scrollToTopTrigger,
            blockLeftOnFirstExpandedItem = true
        )
    }
}
