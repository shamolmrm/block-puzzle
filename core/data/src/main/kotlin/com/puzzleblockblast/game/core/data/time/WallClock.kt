package com.puzzleblockblast.game.core.data.time

import java.util.TimeZone
import javax.inject.Inject

/** Current time and the player's calendar day. Injected so tests control both. */
interface WallClock {
    fun nowMillis(): Long

    /** Days since 1970-01-01 in the device's time zone: the Daily Challenge changes at local midnight. */
    fun todayEpochDay(): Long {
        val now = nowMillis()
        return Math.floorDiv(now + TimeZone.getDefault().getOffset(now), MILLIS_PER_DAY)
    }

    companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

class SystemWallClock @Inject constructor() : WallClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
