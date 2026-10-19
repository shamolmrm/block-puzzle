package com.puzzleblockblast.game.core.audio

enum class Sound { PLACE, CLEAR, CLEAR_MULTI, COMBO, BOARD_CLEAR, GAME_OVER }

/** Plays short game sound effects. Callers decide whether sound is enabled. */
fun interface SoundEffects {
    fun play(sound: Sound)
}
