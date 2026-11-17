package com.nuvio.tv.domain.model

enum class HomeDimmingMode { OFF, ALWAYS, SCHEDULED }

data class HomeDimmingSettings(
    val mode: HomeDimmingMode = HomeDimmingMode.OFF,
    val startMinute: Int = 21 * 60,
    val endMinute: Int = 7 * 60,
    val strengthPercent: Int = 50
) {
    fun alphaAt(minuteOfDay: Int): Float {
        val start = startMinute.coerceIn(0, 1439)
        val end = endMinute.coerceIn(0, 1439)
        val now = Math.floorMod(minuteOfDay, 1440)
        val active = when (mode) {
            HomeDimmingMode.OFF -> false
            HomeDimmingMode.ALWAYS -> true
            HomeDimmingMode.SCHEDULED -> when {
                start == end -> true // Same start/end means the full day.
                start < end -> now >= start && now < end
                else -> now >= start || now < end
            }
        }
        return if (active) strengthPercent.coerceIn(10, 70) / 100f else 0f
    }
}
