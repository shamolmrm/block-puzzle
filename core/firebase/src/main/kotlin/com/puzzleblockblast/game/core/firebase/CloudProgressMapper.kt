package com.puzzleblockblast.game.core.firebase

import com.puzzleblockblast.game.core.model.sync.CloudProgress
import com.puzzleblockblast.game.core.model.sync.ResultRecord

/**
 * Converts between [CloudProgress] and the Firestore document at `users/{uid}/progress/main`
 * (docs/FIREBASE.md). Reading is defensive: anything malformed is skipped, never thrown, and the
 * result is sanitized again before it touches local data.
 */
internal object CloudProgressMapper {
    const val SCHEMA_VERSION = "schemaVersion"
    const val LEVELS = "levels"
    const val DAILY = "daily"
    const val ACHIEVEMENTS = "achievements"
    const val BESTS = "bests"
    const val COUNTERS = "counters"
    const val UPDATED_AT = "updatedAt"
    private const val STARS = "s"
    private const val BEST_SCORE = "b"

    /** All fields except [UPDATED_AT], which the writer sets to the server time. */
    fun toMap(progress: CloudProgress): Map<String, Any> = mapOf(
        SCHEMA_VERSION to CloudProgress.SCHEMA_VERSION.toLong(),
        LEVELS to progress.levels.mapValues { (_, r) -> record(r) },
        DAILY to progress.daily.entries.associate { (day, r) -> day.toString() to record(r) },
        ACHIEVEMENTS to progress.achievements,
        BESTS to progress.bests,
        COUNTERS to progress.counters,
    )

    /** Null for a document written by an unknown (newer) schema: never merge what we can't read. */
    fun fromMap(data: Map<String, Any?>): CloudProgress? {
        if ((data[SCHEMA_VERSION] as? Number)?.toInt() != CloudProgress.SCHEMA_VERSION) return null
        return CloudProgress(
            levels = map(data[LEVELS]).mapNotNull { (k, v) -> readRecord(v)?.let { k to it } }.toMap(),
            daily = map(data[DAILY]).mapNotNull { (k, v) -> k.toLongOrNull()?.let { day -> readRecord(v)?.let { day to it } } }.toMap(),
            achievements = longs(data[ACHIEVEMENTS]),
            bests = longs(data[BESTS]),
            counters = map(data[COUNTERS]).mapValues { (_, v) -> longs(v) },
        )
    }

    private fun record(r: ResultRecord) = mapOf(STARS to r.stars.toLong(), BEST_SCORE to r.bestScore.toLong())

    private fun readRecord(value: Any?): ResultRecord? {
        val m = value as? Map<*, *> ?: return null
        val stars = (m[STARS] as? Number)?.toInt() ?: return null
        val best = (m[BEST_SCORE] as? Number)?.toInt() ?: return null
        return ResultRecord(stars, best)
    }

    private fun map(value: Any?): Map<String, Any?> =
        (value as? Map<*, *>)?.entries?.mapNotNull { (k, v) -> (k as? String)?.let { it to v } }?.toMap().orEmpty()

    private fun longs(value: Any?): Map<String, Long> =
        map(value).mapNotNull { (k, v) -> (v as? Number)?.toLong()?.let { k to it } }.toMap()
}
