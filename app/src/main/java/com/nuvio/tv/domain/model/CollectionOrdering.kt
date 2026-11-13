package com.nuvio.tv.domain.model

/**
 * Collections setup owns collection row order. Saved addon layout positions must
 * not override it. Pinning and visibility remain independent of that order.
 */
internal fun collectionsForHome(
    collections: List<Collection>,
    disabledKeys: Set<String>
): List<Collection> {
    val visible = collections.associateBy { it.id }.values
        .filterNot { "collection_${it.id}" in disabledKeys }
    return visible.filter { it.pinToTop } + visible.filterNot { it.pinToTop }
}

/** Imported entries lead in JSON order; omitted local collections are retained. */
internal fun mergeCollectionImport(
    current: List<Collection>,
    imported: List<Collection>
): List<Collection> {
    val ordered = imported.associateBy { it.id }
    return ordered.values.toList() + current.associateBy { it.id }.values
        .filterNot { it.id in ordered }
}
