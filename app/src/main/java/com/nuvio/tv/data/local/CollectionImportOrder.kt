package com.nuvio.tv.data.local

// Imported collections lead; omitted collections follow. Other row slots stay put.
internal fun applyImportedCollectionOrder(saved: List<String>, importedIds: List<String>): List<String> {
    val savedKeys = saved.distinct()
    val collectionKeys = savedKeys.filter { it.startsWith("collection_") }
    if (collectionKeys.isEmpty()) return saved
    val importedKeys = importedIds.distinct().map { "collection_$it" }
    val orderedKeys = importedKeys + collectionKeys.filterNot { it in importedKeys }
    val remaining = orderedKeys.iterator()
    val reordered = savedKeys.map { key ->
        if (key.startsWith("collection_")) remaining.next() else key
    }
    return reordered + remaining.asSequence().toList()
}
