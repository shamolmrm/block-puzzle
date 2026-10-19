package com.puzzleblockblast.game.core.firebase

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.model.progress.StatKeys
import com.puzzleblockblast.game.core.model.sync.CloudProgress
import com.puzzleblockblast.game.core.model.sync.ResultRecord
import org.junit.Test

class CloudProgressMapperTest {

    private val progress = CloudProgress(
        levels = mapOf("L0001" to ResultRecord(3, 90)),
        daily = mapOf(20_745L to ResultRecord(2, 120)),
        achievements = mapOf("first_game" to 100L),
        bests = mapOf(StatKeys.BEST_CLASSIC_SCORE to 800L),
        counters = mapOf("device-0001" to mapOf(StatKeys.XP to 300L)),
    )

    @Test
    fun roundTrips() {
        assertThat(CloudProgressMapper.fromMap(CloudProgressMapper.toMap(progress))).isEqualTo(progress)
    }

    @Test
    fun readsFirestoreNumberTypesDefensively() {
        // Firestore may return any Number type; malformed entries are skipped, not thrown.
        val data = mapOf(
            "schemaVersion" to 1.0,
            "levels" to mapOf("L0001" to mapOf("s" to 3.0, "b" to 90), "L0002" to "garbage", "L0003" to mapOf("s" to 1)),
            "daily" to mapOf("20745" to mapOf("s" to 2L, "b" to 120L), "not-a-day" to mapOf("s" to 1L, "b" to 1L)),
            "achievements" to mapOf("first_game" to 100, "bad" to "x"),
            "bests" to "not a map",
            "counters" to mapOf("device-0001" to mapOf("xp" to 300.0)),
            "updatedAt" to Any(),
        )

        val read = CloudProgressMapper.fromMap(data)!!

        assertThat(read.levels).containsExactly("L0001", ResultRecord(3, 90))
        assertThat(read.daily).containsExactly(20_745L, ResultRecord(2, 120))
        assertThat(read.achievements).containsExactly("first_game", 100L)
        assertThat(read.bests).isEmpty()
        assertThat(read.counters).containsExactly("device-0001", mapOf("xp" to 300L))
    }

    @Test
    fun unknownSchemaIsNotRead() {
        assertThat(CloudProgressMapper.fromMap(mapOf("schemaVersion" to 2L))).isNull()
        assertThat(CloudProgressMapper.fromMap(emptyMap())).isNull()
    }
}
