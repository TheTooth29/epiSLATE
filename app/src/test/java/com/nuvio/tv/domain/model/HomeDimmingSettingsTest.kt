package com.nuvio.tv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDimmingSettingsTest {
    @Test fun overnightScheduleHasExclusiveEnd() {
        val settings = HomeDimmingSettings(mode = HomeDimmingMode.SCHEDULED)
        for (minute in listOf(1260, 1439, 0, 419)) assertEquals(0.5f, settings.alphaAt(minute))
        for (minute in listOf(420, 900, 1259)) assertEquals(0f, settings.alphaAt(minute))
    }
    @Test fun daytimeAndFullDaySchedules() {
        val settings = HomeDimmingSettings(HomeDimmingMode.SCHEDULED, 480, 600, 30)
        assertEquals(0f, settings.alphaAt(479))
        assertEquals(0.3f, settings.alphaAt(480))
        assertEquals(0f, settings.alphaAt(600))
        assertEquals(0.3f, settings.copy(endMinute = 480).alphaAt(700))
    }
    @Test fun offAlwaysAndStrengthBounds() {
        assertEquals(0f, HomeDimmingSettings().alphaAt(0))
        assertEquals(0.7f, HomeDimmingSettings(HomeDimmingMode.ALWAYS, strengthPercent = 999).alphaAt(800))
        assertEquals(0.1f, HomeDimmingSettings(HomeDimmingMode.ALWAYS, strengthPercent = -5).alphaAt(800))
    }
}
