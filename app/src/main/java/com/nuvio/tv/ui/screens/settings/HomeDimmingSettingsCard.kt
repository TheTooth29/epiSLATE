package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.HomeDimmingMode
import java.util.Calendar

@Composable
internal fun HomeDimmingSettingsCard(viewModel: HomeDimmingViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf<String?>(null) }
    val off = stringResource(R.string.home_dimming_off)
    val always = stringResource(R.string.home_dimming_always)
    val scheduled = stringResource(R.string.home_dimming_scheduled)
    val modes = listOf(off, always, scheduled)
    val start = stringResource(R.string.home_dimming_start)
    val end = stringResource(R.string.home_dimming_end)
    val strength = stringResource(R.string.home_dimming_strength)
    val modeTitle = stringResource(R.string.home_dimming_mode)
    val context = androidx.compose.ui.platform.LocalContext.current
    fun timeLabel(minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minute / 60); set(Calendar.MINUTE, minute % 60)
        }
        return android.text.format.DateFormat.getTimeFormat(context).format(calendar.time)
    }
    SettingsGroupCard(
        title = stringResource(R.string.home_dimming_title),
        subtitle = stringResource(R.string.home_dimming_subtitle)
    ) {
        SettingsActionRow(subtitle = null, title = modeTitle, value = modes[settings.mode.ordinal], onClick = { picker = "mode" })
        if (settings.mode == HomeDimmingMode.SCHEDULED) {
            SettingsActionRow(subtitle = null, title = start, value = timeLabel(settings.startMinute), onClick = { picker = "start" })
            SettingsActionRow(subtitle = null, title = end, value = timeLabel(settings.endMinute), onClick = { picker = "end" })
        }
        if (settings.mode != HomeDimmingMode.OFF) {
            SettingsActionRow(subtitle = null, title = strength, value = "${settings.strengthPercent}%", onClick = { picker = "strength" })
        }
    }
    when (picker) {
        "mode" -> SettingsSingleChoiceDialog(
            title = modeTitle,
            options = HomeDimmingMode.entries.map { SettingsPickerOption(it, modes[it.ordinal]) },
            selectedValue = settings.mode,
            onOptionSelected = { viewModel.save(settings.copy(mode = it)); picker = null },
            onDismiss = { picker = null }
        )
        "start", "end" -> {
            val isStart = picker == "start"
            SettingsSingleChoiceDialog(
                title = if (isStart) start else end,
                options = (0 until 1440 step 30).map { SettingsPickerOption(it, timeLabel(it)) },
                selectedValue = if (isStart) settings.startMinute else settings.endMinute,
                onOptionSelected = {
                    viewModel.save(if (isStart) settings.copy(startMinute = it) else settings.copy(endMinute = it))
                    picker = null
                },
                onDismiss = { picker = null }
            )
        }
        "strength" -> SettingsSingleChoiceDialog(
            title = strength,
            options = (10..70 step 10).map { SettingsPickerOption(it, "$it%") },
            selectedValue = settings.strengthPercent,
            onOptionSelected = { viewModel.save(settings.copy(strengthPercent = it)); picker = null },
            onDismiss = { picker = null }
        )
    }
}
