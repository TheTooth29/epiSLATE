package com.nuvio.tv.ui.screens.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GridReleaseDateTest {
    @Test fun dateOnlyMatchesMovieLabel() {
        assertEquals("13 Nov 2026", formatGridReleaseDate("2026-11-13"))
    }

    @Test fun timestampKeepsCatalogCalendarDate() {
        assertEquals("29 Feb 2024", formatGridReleaseDate("2024-02-29T23:30:00.000Z"))
    }

    @Test fun missingDateAllowsExistingYearFallback() {
        assertNull(formatGridReleaseDate(null))
        assertNull(formatGridReleaseDate(""))
        assertNull(formatGridReleaseDate("2026"))
    }

    @Test fun invalidDateDoesNotCreateMisleadingLabel() {
        assertNull(formatGridReleaseDate("2026-02-30"))
    }
}
