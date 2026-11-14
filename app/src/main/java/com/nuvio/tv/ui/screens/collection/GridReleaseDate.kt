package com.nuvio.tv.ui.screens.collection

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val gridReleaseDateFormatter = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.US)

/** Matches the existing movie digital-date label while accepting date-only or ISO timestamps. */
internal fun formatGridReleaseDate(released: String?): String? =
    released?.takeIf { it.isNotBlank() }?.let { raw ->
        runCatching { LocalDate.parse(raw.take(10)).format(gridReleaseDateFormatter) }.getOrNull()
    }
