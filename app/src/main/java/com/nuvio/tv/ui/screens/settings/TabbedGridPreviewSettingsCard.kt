package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.ui.screens.collection.TabbedGridPreview
import com.nuvio.tv.ui.screens.collection.TabbedGridPreviewStore

@Composable
internal fun TabbedGridPreviewSettingsCard() {
    val context = LocalContext.current
    if (!TabbedGridPreviewStore.supportsLayouts(context.packageName)) return
    val store = remember(context) { TabbedGridPreviewStore.get(context) }
    val mode by store.mode.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    SettingsGroupCard(
        title = "Collection layout",
        subtitle = "Choose a collection layout. Original preserves the previous layout. Other layouts reopen at the first heading."
    ) {
        SettingsActionRow(title = "Layout", subtitle = null, value = mode.label, onClick = { showPicker = true })
    }
    if (showPicker) SettingsSingleChoiceDialog(
        title = "Tabbed Grid layout",
        options = TabbedGridPreview.entries.map { SettingsPickerOption(it, it.label) },
        selectedValue = mode,
        onOptionSelected = { store.select(it); showPicker = false },
        onDismiss = { showPicker = false }
    )
}
