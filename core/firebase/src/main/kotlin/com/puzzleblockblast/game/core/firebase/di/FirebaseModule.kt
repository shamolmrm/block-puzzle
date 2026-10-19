package com.puzzleblockblast.game.core.firebase.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.puzzleblockblast.game.core.analytics.AnalyticsTracker
import com.puzzleblockblast.game.core.analytics.CrashReporter
import com.puzzleblockblast.game.core.analytics.LogcatAnalyticsTracker
import com.puzzleblockblast.game.core.analytics.LogcatCrashReporter
import com.puzzleblockblast.game.core.data.config.ConfigRepository
import com.puzzleblockblast.game.core.data.config.DefaultConfigRepository
import com.puzzleblockblast.game.core.data.sync.CloudStore
import com.puzzleblockblast.game.core.data.sync.NoCloudStore
import com.puzzleblockblast.game.core.firebase.FirebaseAnalyticsTracker
import com.puzzleblockblast.game.core.firebase.FirebaseConfigRepository
import com.puzzleblockblast.game.core.firebase.FirebaseCrashReporter
import com.puzzleblockblast.game.core.firebase.FirebaseStatus
import com.puzzleblockblast.game.core.firebase.FirestoreCloudStore
import com.puzzleblockblast.game.core.firebase.CallablePurchaseVerifier
import com.puzzleblockblast.game.core.firebase.FirestoreEntitlementSource
import com.puzzleblockblast.game.core.monetization.billing.EntitlementSource
import com.puzzleblockblast.game.core.monetization.billing.NoEntitlementSource
import com.puzzleblockblast.game.core.monetization.billing.NoPurchaseVerifier
import com.puzzleblockblast.game.core.monetization.billing.PurchaseVerifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Picks the Firebase implementation when Firebase is configured, otherwise the offline one. */
@Module
@InstallIn(SingletonComponent::class)
internal object FirebaseModule {

    @Provides
    @Singleton
    fun analytics(@ApplicationContext context: Context, offline: LogcatAnalyticsTracker): AnalyticsTracker =
        if (FirebaseStatus.isConfigured(context)) FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context)) else offline

    @Provides
    @Singleton
    fun crashReporter(@ApplicationContext context: Context, offline: LogcatCrashReporter): CrashReporter =
        if (FirebaseStatus.isConfigured(context)) FirebaseCrashReporter(FirebaseCrashlytics.getInstance()) else offline

    @Provides
    @Singleton
    fun cloudStore(@ApplicationContext context: Context): CloudStore =
        if (FirebaseStatus.isConfigured(context)) {
            FirestoreCloudStore(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        } else {
            NoCloudStore
        }

    @Provides
    @Singleton
    fun config(@ApplicationContext context: Context, offline: DefaultConfigRepository): ConfigRepository =
        if (FirebaseStatus.isConfigured(context)) FirebaseConfigRepository(FirebaseRemoteConfig.getInstance()) else offline

    @Provides
    @Singleton
    fun purchaseVerifier(@ApplicationContext context: Context, cloud: CloudStore): PurchaseVerifier =
        if (FirebaseStatus.isConfigured(context)) CallablePurchaseVerifier(FirebaseFunctions.getInstance(), cloud) else NoPurchaseVerifier

    @Provides
    @Singleton
    fun entitlementSource(@ApplicationContext context: Context): EntitlementSource =
        if (FirebaseStatus.isConfigured(context)) {
            FirestoreEntitlementSource(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        } else {
            NoEntitlementSource
        }
}