package com.puzzleblockblast.game.core.analytics.di

import com.puzzleblockblast.game.core.analytics.AnalyticsTracker
import com.puzzleblockblast.game.core.analytics.LogcatAnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(impl: LogcatAnalyticsTracker): AnalyticsTracker
}
