package com.nuvio.tv.ui.screens.collection

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class TabbedGridPreview(val label: String, val columns: Int, val rows: Int) {
    ORIGINAL("Original", 6, 2),
    TWO_ROWS("Two full rows", 6, 2),
    THREE_ROWS("Three full rows", 8, 3),
    PAGES("Page by page", 6, 2),
    CENTERED("Centered preview", 6, 1);

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

/** Deliberately separate from production preferences and restricted to the Test package. */
internal class TabbedGridPreviewStore private constructor(context: Context) {
    private val preferences = context.getSharedPreferences("tabbed_grid_preview", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(runCatching {
        TabbedGridPreview.valueOf(preferences.getString("mode", null) ?: "ORIGINAL")
    }.getOrDefault(TabbedGridPreview.ORIGINAL))
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
        fun isTestPackage(packageName: String) = packageName == "com.nuvio.livetv.test"
        @Volatile private var instance: TabbedGridPreviewStore? = null
        fun get(context: Context): TabbedGridPreviewStore {
            check(isTestPackage(context.packageName))
            return instance ?: synchronized(this) {
                instance ?: TabbedGridPreviewStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
