package com.nuvio.tv.ui.screens.home

/** Presentation only: addon catalogs stay installed and available as Collection sources. */
internal fun visibleCollectionHomeRows(rows: List<HomeRow>): List<HomeRow.CollectionRow> =
    rows.filterIsInstance<HomeRow.CollectionRow>()

/** Collection folder pages share the renderer but must keep their catalog rows. */
internal fun resolveClassicHomeRows(state: HomeUiState): List<HomeRow> = when {
    state.hideStandaloneCatalogRows -> visibleCollectionHomeRows(state.homeRows)
    state.homeRows.isNotEmpty() -> state.homeRows
    else -> state.catalogRows.filter { it.items.isNotEmpty() }.map(HomeRow::Catalog)
}
