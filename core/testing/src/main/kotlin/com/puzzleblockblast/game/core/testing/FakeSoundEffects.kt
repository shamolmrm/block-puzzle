package com.puzzleblockblast.game.core.testing

import com.puzzleblockblast.game.core.audio.Sound
import com.puzzleblockblast.game.core.audio.SoundEffects

class FakeSoundEffects : SoundEffects {
    val played = mutableListOf<Sound>()

    override fun play(sound: Sound) {
        played += sound
    }
}
