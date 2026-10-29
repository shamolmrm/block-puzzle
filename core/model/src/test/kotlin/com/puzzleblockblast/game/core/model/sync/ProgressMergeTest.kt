package com.puzzleblockblast.game.core.model.sync

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.model.progress.StatKeys
import org.junit.Test
import kotlin.random.Random

class ProgressMergeTest {

    private val phone = CloudProgress(
        levels = mapOf("L0001" to ResultRecord(3, 90), "L0002" to ResultRecord(1, 40)),
        daily = mapOf(20_745L to ResultRecord(2, 120)),
        achievements = mapOf("first_game" to 100L),
        bests = mapOf(StatKeys.BEST_CLASSIC_SCORE to 800L, StatKeys.BEST_COMBO to 3L),
        counters = mapOf("phone-0001" to mapOf(StatKeys.XP to 300L, StatKeys.LINES_CLEARED to 50L)),
    )
    private val tablet = CloudProgress(
        levels = mapOf("L0002" to ResultRecord(2, 30), "L0003" to ResultRecord(1, 10)),
        daily = mapOf(20_746L to ResultRecord(1, 60)),
        achievements = mapOf("first_game" to 50L, "combo_4" to 70L),
        bests = mapOf(StatKeys.BEST_CLASSIC_SCORE to 1_200L, StatKeys.BEST_COMBO to 2L),
        counters = mapOf("tablet-0001" to mapOf(StatKeys.XP to 100L, StatKeys.LINES_CLEARED to 20L)),
    )

    @Test
    fun mergeKeepsTheBestOfEverythingAndSumsDevices() {
        val merged = ProgressMerge.merge(phone, tablet)

        assertThat(merged.levels).containsExactly(
            "L0001", ResultRecord(3, 90),
            "L0002", ResultRecord(2, 40), // stars from the tablet, score from the phone
            "L0003", ResultRecord(1, 10),
        )
        assertThat(merged.daily.keys).containsExactly(20_745L, 20_746L)
        assertThat(merged.achievements).containsExactly("first_game", 50L, "combo_4", 70L)
        assertThat(merged.bests).containsExactly(StatKeys.BEST_CLASSIC_SCORE, 1_200L, StatKeys.BEST_COMBO, 3L)
        assertThat(merged.total(StatKeys.XP)).isEqualTo(400)
        assertThat(merged.total(StatKeys.LINES_CLEARED)).isEqualTo(70)
    }

    @Test
    fun syncingTheSameDataTwiceDoesNotDoubleCount() {
        val once = ProgressMerge.merge(phone, tablet)
        val twice = ProgressMerge.merge(ProgressMerge.merge(once, phone), tablet)

        assertThat(twice).isEqualTo(once)
        assertThat(twice.total(StatKeys.XP)).isEqualTo(400)
    }

    @Test
    fun aDeviceThatEarnedMoreSinceLastSyncWins() {
        val later = phone.copy(counters = mapOf("phone-0001" to mapOf(StatKeys.XP to 350L, StatKeys.LINES_CLEARED to 55L)))

        val merged = ProgressMerge.merge(ProgressMerge.merge(phone, tablet), later)

        assertThat(merged.total(StatKeys.XP)).isEqualTo(450)
    }

    /** Property check over random documents: commutative, associative and idempotent. */
    @Test
    fun mergeIsACrdt() {
        val random = Random(7)
        repeat(300) {
            val a = randomProgress(random)
            val b = randomProgress(random)
            val c = randomProgress(random)
            assertThat(ProgressMerge.merge(a, b)).isEqualTo(ProgressMerge.merge(b, a))
            assertThat(ProgressMerge.merge(ProgressMerge.merge(a, b), c)).isEqualTo(ProgressMerge.merge(a, ProgressMerge.merge(b, c)))
            assertThat(ProgressMerge.merge(a, a)).isEqualTo(a)
        }
    }

    @Test
    fun sanitizeRejectsTamperedValues() {
        val tampered = CloudProgress(
            levels = mapOf("L0001" to ResultRecord(99, -5), "../evil" to ResultRecord(3, 10)),
            daily = mapOf(-1L to ResultRecord(3, 10), 20_745L to ResultRecord(-2, 30)),
            achievements = mapOf("first_game" to 10L, "BAD ID!" to 1L, "combo_4" to -3L),
            bests = mapOf(StatKeys.BEST_CLASSIC_SCORE to -1L, "made_up" to 5L, StatKeys.BEST_COMBO to 4L),
            counters = mapOf("x" to mapOf(StatKeys.XP to 5L), "tablet-0001" to mapOf(StatKeys.XP to -9L, "coins" to 99L, StatKeys.LINES_CLEARED to 3L)),
        )

        val clean = ProgressMerge.sanitize(tampered)

        assertThat(clean.levels).containsExactly("L0001", ResultRecord(3, 0))
        assertThat(clean.daily).containsExactly(20_745L, ResultRecord(0, 30))
        assertThat(clean.achievements).containsExactly("first_game", 10L)
        assertThat(clean.bests).containsExactly(StatKeys.BEST_COMBO, 4L)
        assertThat(clean.counters).containsExactly("tablet-0001", mapOf(StatKeys.LINES_CLEARED to 3L))
    }

    @Test
    fun `only pack levels sync, not weekly event runs`() {
        assertThat(ProgressMerge.isSyncedLevelId("L0042")).isTrue()
        assertThat(ProgressMerge.isSyncedLevelId("E01L03@2915")).isFalse()
        assertThat(ProgressMerge.isSyncedLevelId("E01L03")).isFalse()
    }

    private fun randomProgress(random: Random) = CloudProgress(
        levels = (1..random.nextInt(4)).associate { "L000${random.nextInt(1, 6)}" to ResultRecord(random.nextInt(4), random.nextInt(200)) },
        daily = (1..random.nextInt(3)).associate { random.nextLong(20_740, 20_750) to ResultRecord(random.nextInt(4), random.nextInt(200)) },
        achievements = (1..random.nextInt(3)).associate { listOf("a", "b", "c")[random.nextInt(3)] to random.nextLong(1_000) },
        bests = mapOf(StatKeys.BEST_COMBO to random.nextLong(10)),
        counters = (1..random.nextInt(3)).associate { "dev-000${random.nextInt(3)}" to mapOf(StatKeys.XP to random.nextLong(500)) },
    )
}
