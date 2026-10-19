package com.puzzleblockblast.game.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.puzzleblockblast.game.core.data.classic.ClassicDataStore
import com.puzzleblockblast.game.core.data.classic.ClassicGameRepository
import com.puzzleblockblast.game.core.data.classic.DataStoreClassicGameRepository
import com.puzzleblockblast.game.core.data.settings.DataStoreSettingsRepository
import com.puzzleblockblast.game.core.data.settings.SettingsDataStore
import com.puzzleblockblast.game.core.data.settings.SettingsRepository
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindClassicGameRepository(impl: DataStoreClassicGameRepository): ClassicGameRepository

    companion object {
        @Provides
        @Singleton
        @SettingsDataStore
        fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

        @Provides
        @Singleton
        @ClassicDataStore
        fun provideClassicDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("classic_game") }

        @Provides
        @Singleton
        fun provideClassicGame(): ClassicGame = ClassicGame()

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
