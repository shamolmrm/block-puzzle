package com.puzzleblockblast.game.core.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class AnalyticsEventTest {

    @Test
    fun acceptsCatalogueEvents() {
        val event = AnalyticsEvent(AnalyticsEvents.GAME_STARTED, mapOf(AnalyticsEvents.Params.MODE to "classic"))

        assertThat(event.params).containsEntry("mode", "classic")
    }

    @Test
    fun rejectsInvalidNamesAndValues() {
        assertThrows(IllegalArgumentException::class.java) { AnalyticsEvent("Game Started") }
        assertThrows(IllegalArgumentException::class.java) { AnalyticsEvent("x".repeat(41)) }
        assertThrows(IllegalArgumentException::class.java) { AnalyticsEvent("ok", mapOf("Bad-Key" to 1)) }
        assertThrows(IllegalArgumentException::class.java) { AnalyticsEvent("ok", mapOf("list" to listOf(1))) }
    }
}
