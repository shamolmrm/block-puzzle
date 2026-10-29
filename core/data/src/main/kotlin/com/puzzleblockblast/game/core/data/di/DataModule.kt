package com.puzzleblockblast.game.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.puzzleblockblast.game.core.data.classic.ClassicDataStore
import com.puzzleblockblast.game.core.data.classic.ClassicGameRepository
import com.puzzleblockblast.game.core.data.classic.DataStoreClassicGameRepository
import androidx.room.Room
import com.puzzleblockblast.game.core.data.level.BundledEventRepository
import com.puzzleblockblast.game.core.data.level.BundledLevelRepository
import com.puzzleblockblast.game.core.data.level.EventRepository
import com.puzzleblockblast.game.core.data.level.LevelProgressDao
import com.puzzleblockblast.game.core.data.level.LevelProgressRepository
import com.puzzleblockblast.game.core.data.level.LevelRepository
import com.puzzleblockblast.game.core.data.level.PbbDatabase
import com.puzzleblockblast.game.core.data.level.RoomLevelProgressRepository
import com.puzzleblockblast.game.core.data.progress.ProgressListener
import com.puzzleblockblast.game.core.data.progress.ProgressionRepository
import com.puzzleblockblast.game.core.data.sync.CloudBackup
import com.puzzleblockblast.game.core.data.sync.SyncRepository
import com.puzzleblockblast.game.core.data.sync.SyncScheduler
import com.puzzleblockblast.game.core.data.progress.RoomProgressionRepository
import com.puzzleblockblast.game.core.data.time.SystemWallClock
import com.puzzleblockblast.game.core.data.time.WallClock
import com.puzzleblockblast.game.core.data.settings.DataStoreSettingsRepository
import com.puzzleblockblast.game.core.data.settings.SettingsDataStore
import com.puzzleblockblast.game.core.data.settings.SettingsRepository
import com.puzzleblockblast.game.core.data.config.ConfigRepository
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.ClassicRules
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

    @Binds
    abstract fun bindLevelRepository(impl: BundledLevelRepository): LevelRepository

    @Binds
    abstract fun bindEventRepository(impl: BundledEventRepository): EventRepository

    @Binds
    abstract fun bindLevelProgressRepository(impl: RoomLevelProgressRepository): LevelProgressRepository

    @Binds
    abstract fun bindProgressionRepository(impl: RoomProgressionRepository): ProgressionRepository

    @Binds
    abstract fun bindWallClock(impl: SystemWallClock): WallClock

    @Binds
    abstract fun bindProgressListener(impl: SyncScheduler): ProgressListener

    @Binds
    abstract fun bindCloudBackup(impl: SyncRepository): CloudBackup

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): PbbDatabase =
            Room.databaseBuilder(context, PbbDatabase::class.java, PbbDatabase.NAME).build()

        @Provides
        fun provideLevelProgressDao(db: PbbDatabase): LevelProgressDao = db.levelProgressDao()

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
        fun provideClassicGame(config: ConfigRepository): ClassicGame =
            ClassicGame(ClassicRules(hintsPerGame = config.config.value.classicHintsPerGame))

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
