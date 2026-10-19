package com.puzzleblockblast.game.core.testing

import com.puzzleblockblast.game.core.data.time.WallClock

class FakeWallClock(var now: Long = 42L, var today: Long = 20_745L) : WallClock {
    override fun nowMillis(): Long = now
    override fun todayEpochDay(): Long = today
}
