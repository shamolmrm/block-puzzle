package com.puzzleblockblast.game.core.monetization.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.puzzleblockblast.game.core.data.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** In-app product ids. Must match the Play Console and the verification function. */
object Products {
    const val REMOVE_ADS = "remove_ads"
}

/**
 * Server-verified entitlements (Firestore `users/{uid}/entitlements/main`, writable only by the
 * verification function). Null while unknown (offline, not signed in, Firebase not configured).
 */
interface EntitlementSource {
    val removeAds: Flow<Boolean?>
}

/** Used when Firebase isn't configured: no verified entitlements exist. */
object NoEntitlementSource : EntitlementSource {
    override val removeAds: Flow<Boolean?> = kotlinx.coroutines.flow.flowOf(null)
}

/**
 * Whether ads are removed. Only a server-verified value is ever written to the local cache; the
 * cache just lets an offline start honour a purchase the server already confirmed. Nothing on the
 * device can grant the entitlement by itself.
 */
interface Entitlements {
    val adsRemoved: StateFlow<Boolean>
}

@Singleton
class CachedEntitlements @Inject constructor(
    @ApplicationContext context: Context,
    source: EntitlementSource,
    @param:ApplicationScope private val scope: CoroutineScope,
) : Entitlements {
    private val cache: DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("entitlements_cache") }

    override val adsRemoved: StateFlow<Boolean> = cache.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[REMOVE_ADS_VERIFIED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch {
            source.removeAds.filterNotNull().collect { verified -> cache.edit { it[REMOVE_ADS_VERIFIED] = verified } }
        }
    }

    private companion object {
        val REMOVE_ADS_VERIFIED = booleanPreferencesKey("remove_ads_server_verified")
    }
}
