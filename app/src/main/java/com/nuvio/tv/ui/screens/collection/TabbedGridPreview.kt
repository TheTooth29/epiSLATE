package com.nuvio.tv.ui.screens.collection

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class TabbedGridPreview(val label: String, val columns: Int, val rows: Int) {
    ORIGINAL("Original", 6, 2),
    TWO_ROWS("Two full rows", 6, 2),
    PAGES("Page by page", 6, 2),
    CENTERED("Centered preview", 5, 1);

    companion object {
        fun fromStored(value: String?): TabbedGridPreview =
            if (value == null) TWO_ROWS else entries.firstOrNull { it.name == value } ?: ORIGINAL
    }

    fun firstRow(focusedRow: Int, currentFirstRow: Int): Int = when (this) {
        PAGES -> focusedRow / rows * rows
        CENTERED -> focusedRow
        else -> when {
            focusedRow < currentFirstRow -> focusedRow
            focusedRow >= currentFirstRow + rows -> focusedRow - rows + 1
            else -> currentFirstRow
        }
    }.coerceAtLeast(0)
}

internal data class TabbedGridPreviewGeometry(val cardWidth: Float, val rowHeight: Float, val gridHeight: Float)

internal fun tabbedGridPreviewGeometry(
    mode: TabbedGridPreview, width: Float, height: Float, metadataHeight: Float,
    landscape: Boolean, inset: Float = 48f, gap: Float = 16f
): TabbedGridPreviewGeometry {
    val centered = mode == TabbedGridPreview.CENTERED
    // Preserve the existing 12dp vertical inset; leave space outside the centered viewport for arrows.
    val maximumRowHeight = if (centered) height - 24f - 72f
        else (height - 24f - gap * (mode.rows - 1)) / mode.rows
    val columnWidth = (width - inset * 2 - gap * (mode.columns - 1)) / mode.columns
    val widthScale = if (landscape) 1.5f else 1f
    val heightScale = if (landscape) 1.5f / (16f / 9f) else 1.5f
    val cardWidth = minOf(columnWidth / widthScale, (maximumRowHeight - metadataHeight) / heightScale)
        .coerceAtLeast(1f)
    val rowHeight = if (centered) cardWidth * heightScale + metadataHeight else maximumRowHeight
    return TabbedGridPreviewGeometry(cardWidth, rowHeight, if (centered) rowHeight + 24f else height)
}

/** Layout choices are separate from profile data; existing named selections remain compatible. */
internal class TabbedGridPreviewStore private constructor(context: Context) {
    private val preferences = context.getSharedPreferences("tabbed_grid_preview", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(TabbedGridPreview.fromStored(preferences.getString("mode", null)))
    val mode = _mode.asStateFlow()

    fun select(mode: TabbedGridPreview) {
        preferences.edit().putString("mode", mode.name).apply()
        _mode.value = mode
    }

    fun selectedTab(folderId: String, mode: TabbedGridPreview, fallback: Int): Int =
        preferences.getInt("tab.$folderId.${mode.name}", fallback)

    fun saveSelectedTab(folderId: String, mode: TabbedGridPreview, index: Int) {
        val key = "tab.$folderId.${mode.name}"
        if (preferences.getInt(key, -1) != index) preferences.edit().putInt(key, index).apply()
    }

    fun position(key: String): FolderDetailGridFocusState = FolderDetailGridFocusState(
        verticalScrollIndex = preferences.getInt("$key.index", 0).coerceAtLeast(0),
        verticalScrollOffset = preferences.getInt("$key.offset", 0),
        focusedItemKey = preferences.getString("$key.focus", null),
        hasSavedFocus = preferences.contains("$key.focus")
    )

    fun save(key: String, index: Int, offset: Int, focus: String?) {
        preferences.edit().putInt("$key.index", index).putInt("$key.offset", offset)
            .putString("$key.focus", focus).apply()
    }

    companion object {
        fun supportsLayouts(packageName: String) = packageName in setOf("com.nuvio.livetv", "com.nuvio.livetv.test")
        @Volatile private var instance: TabbedGridPreviewStore? = null
        fun get(context: Context): TabbedGridPreviewStore {
            check(supportsLayouts(context.packageName))
            return instance ?: synchronized(this) {
                instance ?: TabbedGridPreviewStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
