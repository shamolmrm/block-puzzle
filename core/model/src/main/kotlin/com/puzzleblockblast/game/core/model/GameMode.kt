package com.puzzleblockblast.game.core.model

/** Game modes. [analyticsName] is a stable value used in analytics params; never rename it. */
enum class GameMode(val analyticsName: String) {
    CLASSIC("classic"),
    LEVEL("level"),
    DAILY("daily"),
    EVENT("event"),
}
