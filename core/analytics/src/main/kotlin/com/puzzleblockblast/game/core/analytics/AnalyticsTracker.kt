package com.puzzleblockblast.game.core.analytics

/**
 * A single analytics event. Names and param keys must be snake_case, at most 40 characters, and
 * must never contain personal data (names, emails, free text).
 */
data class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap(),
) {
    init {
        require(NAME_PATTERN.matches(name)) { "Invalid event name '$name'" }
        params.keys.forEach { require(NAME_PATTERN.matches(it)) { "Invalid param key '$it'" } }
        params.values.forEach {
            require(it is String || it is Long || it is Int || it is Double || it is Boolean) {
                "Unsupported param value type ${it::class.simpleName}"
            }
        }
    }

    private companion object {
        val NAME_PATTERN = Regex("[a-z][a-z0-9_]{0,39}")
    }
}

interface AnalyticsTracker {
    fun log(event: AnalyticsEvent)
}
