package com.puzzleblockblast.game.core.engine.level

/** A themed set of levels. [id] like `E01`; level ids like `E01L03`. */
data class EventDefinition(val id: String, val title: String, val levels: List<LevelDefinition>)

/**
 * The event running this week. Events rotate weekly (Monday, device-local calendar) through the
 * bundled list, so there is always one running, offline. [instance] identifies this particular run:
 * progress is keyed by it, so each run starts fresh.
 */
data class ActiveEvent(val event: EventDefinition, val instance: Long, val startEpochDay: Long, val endEpochDay: Long) {
    fun daysLeft(today: Long): Int = (endEpochDay - today).toInt().coerceAtLeast(0)

    /** Progress / route id for one of this run's levels: `E01L03@2915`. */
    fun runLevelId(level: LevelDefinition): String = "${level.id}$RUN_SEPARATOR$instance"

    companion object {
        const val RUN_SEPARATOR = '@'
    }
}

object EventSchedule {
    /** 1970-01-05 was a Monday (epoch day 4): weeks run Monday..Sunday. */
    private const val FIRST_MONDAY = 4L
    const val DAYS = 7

    fun active(events: List<EventDefinition>, today: Long): ActiveEvent? {
        if (events.isEmpty()) return null
        val week = Math.floorDiv(today - FIRST_MONDAY, DAYS.toLong())
        val start = FIRST_MONDAY + week * DAYS
        return ActiveEvent(events[Math.floorMod(week, events.size.toLong()).toInt()], week, start, start + DAYS)
    }

    /** `E01L03@2915` -> (`E01L03`, 2915); null for anything that isn't an event run id. */
    fun parseRunLevelId(id: String): Pair<String, Long>? {
        val at = id.indexOf(ActiveEvent.RUN_SEPARATOR)
        if (at <= 0 || !id.startsWith("E")) return null
        val instance = id.substring(at + 1).toLongOrNull() ?: return null
        return id.substring(0, at) to instance
    }

    fun isEventLevelId(id: String): Boolean = EVENT_LEVEL_ID.matches(id)

    private val EVENT_LEVEL_ID = Regex("E\\d{2}L\\d{2}")
}
