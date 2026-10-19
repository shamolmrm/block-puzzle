package com.puzzleblockblast.game.core.firebase

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.puzzleblockblast.game.core.data.level.PbbDatabase
import com.puzzleblockblast.game.core.data.level.RoomLevelProgressRepository
import com.puzzleblockblast.game.core.data.progress.RoomProgressionRepository
import com.puzzleblockblast.game.core.data.sync.SyncRepository
import com.puzzleblockblast.game.core.data.sync.SyncResult
import com.puzzleblockblast.game.core.model.sync.CloudProgress
import com.puzzleblockblast.game.core.model.sync.ResultRecord
import com.puzzleblockblast.game.core.testing.FakeLevelRepository
import com.puzzleblockblast.game.core.testing.FakeWallClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.net.Socket

/**
 * End-to-end against the local Firebase emulators (demo project, never real Firebase), reached
 * from the phone through `adb reverse`. Skipped unless the emulators are running: run it with
 * `firebase/scripts/android-emulator-test.cmd` (docs/FIREBASE.md). The emulator enforces
 * firebase/firestore.rules, so this also proves the app's writes pass the security rules.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseEmulatorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val auth: FirebaseAuth get() = emulatorAuth
    private val firestore: FirebaseFirestore get() = emulatorFirestore
    private val databases = mutableListOf<PbbDatabase>()

    @Before
    fun setUp() {
        assumeTrue("Firebase emulators not running (see docs/FIREBASE.md)", reachable(AUTH_PORT) && reachable(FIRESTORE_PORT))
        // Each test signs in as a brand-new anonymous user, so tests never see each other's data.
        auth.signOut()
    }

    @After
    fun tearDown() {
        databases.forEach { it.close() }
    }

    @Test
    fun anonymousUserCanWriteAndMergeTheirProgress() = runTest {
        val store = FirestoreCloudStore(auth, firestore)
        val uid = store.userId()

        val first = store.update(uid) { current ->
            assertThat(current).isNull()
            CloudProgress(levels = mapOf("L0001" to ResultRecord(2, 50)))
        }
        val second = store.update(uid) { current ->
            assertThat(current).isEqualTo(first)
            current!!.copy(levels = current.levels + ("L0002" to ResultRecord(1, 10)))
        }

        assertThat(second.levels.keys).containsExactly("L0001", "L0002")
    }

    @Test
    fun anotherUsersDocumentIsDenied() = runTest {
        FirestoreCloudStore(auth, firestore).userId()

        val error = runCatching {
            firestore.document("users/someone-else/progress/main").get().await()
        }.exceptionOrNull()

        assertThat((error as? FirebaseFirestoreException)?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    /** Two installs of one account (same user, different devices) end up with the union of both. */
    @Test
    fun twoDevicesSyncWithoutLosingOrDoubleCountingProgress() = runTest {
        val store = FirestoreCloudStore(auth, firestore)
        val levels = FakeLevelRepository(emptyList())
        val phone = device(store, levels)
        val tablet = device(store, levels)

        phone.progression.recordLevelEnd("L0001", 3, 90, linesCleared = 5, bestCombo = 2, atMillis = 1, today = 20_745)
        tablet.progression.recordLevelEnd("L0002", 2, 40, linesCleared = 3, bestCombo = 1, atMillis = 2, today = 20_745)
        tablet.progression.recordClassicGame(score = 700, linesCleared = 10, bestCombo = 4, atMillis = 3)

        assertThat(phone.sync.sync()).isInstanceOf(SyncResult.Synced::class.java)
        assertThat(tablet.sync.sync()).isInstanceOf(SyncResult.Synced::class.java)
        assertThat(phone.sync.sync()).isInstanceOf(SyncResult.Synced::class.java)
        // Syncing again changes nothing (no double counting).
        assertThat(tablet.sync.sync()).isInstanceOf(SyncResult.Synced::class.java)

        for (d in listOf(phone, tablet)) {
            val snapshot = d.progression.snapshot(20_745).first()
            assertThat(d.levelProgress.progress.first().mapValues { it.value.stars }).containsExactly("L0001", 3, "L0002", 2)
            assertThat(snapshot.linesCleared).isEqualTo(18)
            assertThat(snapshot.bestClassicScore).isEqualTo(700)
            assertThat(snapshot.classicGames).isEqualTo(1)
            assertThat(snapshot.xp).isEqualTo(phoneXp + tabletXp)
        }
    }

    private class Device(val progression: RoomProgressionRepository, val levelProgress: RoomLevelProgressRepository, val sync: SyncRepository)

    private fun device(store: FirestoreCloudStore, levels: FakeLevelRepository): Device {
        val db = Room.inMemoryDatabaseBuilder(context, PbbDatabase::class.java).build().also { databases += it }
        return Device(
            progression = RoomProgressionRepository(db, levels) {},
            levelProgress = RoomLevelProgressRepository(db.levelProgressDao()),
            sync = SyncRepository(db, store, levels, FakeWallClock()),
        )
    }

    private fun reachable(port: Int): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress(HOST, port), 500) }
    }.isSuccess

    private companion object {
        const val HOST = "127.0.0.1"
        const val AUTH_PORT = 9099
        const val FIRESTORE_PORT = 8080

        /**
         * One emulator-connected app for the whole class: `useEmulator` may only be called once per
         * instance, and Auth keeps internal state if an app is deleted and recreated.
         */
        private val emulatorApp: FirebaseApp by lazy {
            val options = FirebaseOptions.Builder()
                .setProjectId("demo-pbb")
                .setApplicationId("1:000000000000:android:0000000000000000")
                .setApiKey("demo-api-key-not-a-secret")
                .build()
            FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext, options, "emulator-test")
        }
        val emulatorAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance(emulatorApp).apply { useEmulator(HOST, AUTH_PORT) } }
        val emulatorFirestore: FirebaseFirestore by lazy {
            FirebaseFirestore.getInstance(emulatorApp).apply {
                useEmulator(HOST, FIRESTORE_PORT)
                firestoreSettings = firestoreSettings { setLocalCacheSettings(memoryCacheSettings {}) }
            }
        }

        /** Level L0001 at 3 stars: 3*20 + 20 first clear. */
        const val phoneXp = 80L

        /** L0002 at 2 stars (40 + 20) plus a Classic game scoring 700 (5 + 14). */
        const val tabletXp = 60L + 19L
    }
}
