package com.puzzleblockblast.game.core.audio.di

import com.puzzleblockblast.game.core.audio.SoundEffects
import com.puzzleblockblast.game.core.audio.SoundPoolSoundEffects
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    @Binds
    abstract fun bindSoundEffects(impl: SoundPoolSoundEffects): SoundEffects
}
