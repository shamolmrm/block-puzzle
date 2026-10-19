package com.puzzleblockblast.game.core.analytics

/** Catalogue of event names and param keys (docs/PLAN.md §9). Keep values stable once shipped. */
object AnalyticsEvents {
    const val TUTORIAL_STARTED = "tutorial_started"
    const val TUTORIAL_COMPLETED = "tutorial_completed"
    const val GAME_STARTED = "game_started"
    const val GAME_OVER = "game_over"
    const val LEVEL_STARTED = "level_started"
    const val LEVEL_COMPLETED = "level_completed"
    const val LEVEL_FAILED = "level_failed"
    const val DAILY_CHALLENGE_COMPLETED = "daily_challenge_completed"
    const val REWARDED_AD_COMPLETED = "rewarded_ad_completed"
    const val SETTINGS_CHANGED = "settings_changed"
    const val HINT_USED = "hint_used"
    const val UNDO_USED = "undo_used"

    object Params {
        const val MODE = "mode"
        const val SETTING = "setting"
        const val ENABLED = "enabled"
        const val RESUMED = "resumed"
        const val SCORE_BUCKET = "score_bucket"
        const val MOVES = "moves"
        const val SOURCE = "source"
    }

    /** Coarse score ranges: exact scores add nothing for retention analysis. */
    fun scoreBucket(score: Int): String = when {
        score < 100 -> "0_99"
        score < 500 -> "100_499"
        score < 1_000 -> "500_999"
        score < 2_500 -> "1000_2499"
        score < 5_000 -> "2500_4999"
        else -> "5000_plus"
    }
}

fun AnalyticsTracker.logSettingChanged(setting: String, enabled: Boolean) = log(
    AnalyticsEvent(
        AnalyticsEvents.SETTINGS_CHANGED,
        mapOf(AnalyticsEvents.Params.SETTING to setting, AnalyticsEvents.Params.ENABLED to enabled),
    ),
)
