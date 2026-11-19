package com.nuvio.tv.core.trial

/** Presentation only: numbering follows the filtered, sorted visible stream list. */
object TrialStreamLabel {
    fun format(original: String, number: Int): String {
        require(number > 0)
        val cacheSymbol = listOf("⚡", "☠", "⏳", "⌛")
            .firstOrNull { original.trimStart().startsWith(it) }
        val prefix = cacheSymbol?.let { "$it " } ?: ""
        return "${prefix}epiSLATE | $number"
    }
}
