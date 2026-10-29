package com.puzzleblockblast.game.core.engine.level

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EventScheduleTest {

    private val events = listOf("E01", "E02", "E03").map { EventDefinition(it, it, emptyList()) }

    /** 2026-10-19 is a Monday. */
    private val monday = 20_745L

    @Test
    fun eventsRunMondayToSundayAndRotate() {
        val thisWeek = EventSchedule.active(events, monday)!!
        val sunday = EventSchedule.active(events, monday + 6)!!
        val nextWeek = EventSchedule.active(events, monday + 7)!!

        assertThat(thisWeek.startEpochDay).isEqualTo(monday)
        assertThat(thisWeek.endEpochDay).isEqualTo(monday + 7)
        assertThat(sunday).isEqualTo(thisWeek)
        assertThat(nextWeek.instance).isEqualTo(thisWeek.instance + 1)
        assertThat(nextWeek.event).isNotEqualTo(thisWeek.event)
        assertThat(EventSchedule.active(events, monday + 21)!!.event).isEqualTo(thisWeek.event) // 3-week cycle
        assertThat(thisWeek.daysLeft(monday + 5)).isEqualTo(2)
    }

    @Test
    fun runIdsRoundTripAndRejectOtherIds() {
        val active = EventSchedule.active(events, monday)!!
        val level = testLevel().copy(id = "E01L03")

        val runId = active.runLevelId(level)

        assertThat(EventSchedule.parseRunLevelId(runId)).isEqualTo("E01L03" to active.instance)
        assertThat(EventSchedule.parseRunLevelId("L0001")).isNull()
        assertThat(EventSchedule.parseRunLevelId("D20745")).isNull()
        assertThat(EventSchedule.isEventLevelId("E01L03")).isTrue()
        assertThat(EventSchedule.isEventLevelId("L0001")).isFalse()
    }

    @Test
    fun noEventsMeansNoActiveEvent() {
        assertThat(EventSchedule.active(emptyList(), monday)).isNull()
    }
}
