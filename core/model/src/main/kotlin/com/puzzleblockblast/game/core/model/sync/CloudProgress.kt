package com.puzzleblockblast.game.core.model.sync

import com.puzzleblockblast.game.core.model.progress.StatKeys

/** Best result on one level or daily date. */
data class ResultRecord(val stars: Int, val bestScore: Int)

/**
 * The synced progress document (docs/PLAN.md §6). Merging never loses data, whatever order devices
 * sync in, because every field merges with a commutative, associative, idempotent rule:
 *
 * - [levels], [daily]: union; per entry the max stars and max best score.
 * - [achievements]: union; the earliest unlock time wins.
 * - [bests]: max per key (best Classic score, best combo).
 * - [counters]: a grow-only counter per device (`deviceId -> name -> value`), merged by max per
 *   device and key; totals are the sum over devices, so nothing is double counted.
 */
data class CloudProgress(
    val levels: Map<String, ResultRecord> = emptyMap(),
    val daily: Map<Long, ResultRecord> = emptyMap(),
    val achievements: Map<String, Long> = emptyMap(),
    val bests: Map<String, Long> = emptyMap(),
    val counters: Map<String, Map<String, Long>> = emptyMap(),
) {
    fun total(counter: String): Long = counters.values.sumOf { it[counter] ?: 0 }

    companion object {
        const val SCHEMA_VERSION = 1

        /** Counters summed across devices (each device only ever increases its own). */
        val ADDITIVE = setOf(StatKeys.XP, StatKeys.CLASSIC_GAMES, StatKeys.LINES_CLEARED)

        /** Values merged by max. */
        val MAXIMA = setOf(StatKeys.BEST_CLASSIC_SCORE, StatKeys.BEST_COMBO)
    }
}

object ProgressMerge {

    fun merge(a: CloudProgress, b: CloudProgress): CloudProgress = CloudProgress(
        levels = mergeMaps(a.levels, b.levels, ::maxRecord),
        daily = mergeMaps(a.daily, b.daily, ::maxRecord),
        achievements = mergeMaps(a.achievements, b.achievements) { x, y -> minOf(x, y) },
        bests = mergeMaps(a.bests, b.bests) { x, y -> maxOf(x, y) },
        counters = mergeMaps(a.counters, b.counters) { x, y -> mergeMaps(x, y) { p, q -> maxOf(p, q) } },
    )

    private fun maxRecord(x: ResultRecord, y: ResultRecord) =
        ResultRecord(maxOf(x.stars, y.stars), maxOf(x.bestScore, y.bestScore))

    private fun <K, V> mergeMaps(a: Map<K, V>, b: Map<K, V>, combine: (V, V) -> V): Map<K, V> {
        val out = a.toMutableMap()
        for ((key, value) in b) out[key] = out[key]?.let { combine(it, value) } ?: value
        return out
    }

    /**
     * Cleans a document that came from the network before it touches local data: never trust it.
     * Clamps stars and drops negative values, unknown counter names and oversize maps.
     */
    fun sanitize(remote: CloudProgress): CloudProgress = CloudProgress(
        levels = remote.levels.entries.take(MAX_LEVELS)
            .filter { (id, _) -> LEVEL_ID.matches(id) }
            .associate { (id, r) -> id to ResultRecord(r.stars.coerceIn(0, 3), r.bestScore.coerceAtLeast(0)) },
        daily = remote.daily.entries.take(MAX_DAYS)
            .filter { (day, _) -> day in 0..MAX_EPOCH_DAY }
            .associate { (day, r) -> day to ResultRecord(r.stars.coerceIn(0, 3), r.bestScore.coerceAtLeast(0)) },
        achievements = remote.achievements.entries.take(MAX_ACHIEVEMENTS)
            .filter { (id, at) -> ACHIEVEMENT_ID.matches(id) && at >= 0 }
            .associate { it.key to it.value },
        bests = remote.bests.filter { (k, v) -> k in CloudProgress.MAXIMA && v >= 0 },
        counters = remote.counters.entries.take(MAX_DEVICES)
            .filter { (device, _) -> DEVICE_ID.matches(device) }
            .associate { (device, values) -> device to values.filter { (k, v) -> k in CloudProgress.ADDITIVE && v >= 0 } },
    )

    /** Only pack levels sync; weekly event runs (`E01L03@2915`) stay on the device. */
    fun isSyncedLevelId(id: String): Boolean = LEVEL_ID.matches(id)

    const val MAX_LEVELS = 1_000
    const val MAX_DAYS = 5_000
    const val MAX_ACHIEVEMENTS = 200
    const val MAX_DEVICES = 20
    private const val MAX_EPOCH_DAY = 200_000L
    private val LEVEL_ID = Regex("L\\d{4}")
    private val ACHIEVEMENT_ID = Regex("[a-z0-9_]{1,40}")
    private val DEVICE_ID = Regex("[A-Za-z0-9-]{8,64}")
}
