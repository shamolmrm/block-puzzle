package com.puzzleblockblast.game.core.data.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Remotely tunable values. Every value has a bundled default and a clamped range, so a missing,
 * malformed or out-of-range remote value can never break the game. Never put secrets here: remote
 * config is readable by anyone with the app.
 */
data class AppConfig(
    /** Kill switch for the Daily Challenge, e.g. if a generator bug ships. */
    val dailyChallengeEnabled: Boolean = true,
    /** Hints per Classic game. Read once per app start, so a running game never changes rules. */
    val classicHintsPerGame: Int = DEFAULT_CLASSIC_HINTS,
) {
    companion object {
        const val KEY_DAILY_ENABLED = "daily_challenge_enabled"
        const val KEY_CLASSIC_HINTS = "classic_hints_per_game"
        const val DEFAULT_CLASSIC_HINTS = 3
        val CLASSIC_HINTS_RANGE = 1..5

        /** The bundled defaults, in the form remote config expects. */
        val DEFAULTS: Map<String, Any> = mapOf(KEY_DAILY_ENABLED to true, KEY_CLASSIC_HINTS to DEFAULT_CLASSIC_HINTS.toLong())

        /** Builds a config from raw values (null = not set remotely), clamping each into range. */
        fun from(dailyEnabled: Boolean?, classicHints: Long?): AppConfig = AppConfig(
            dailyChallengeEnabled = dailyEnabled ?: true,
            classicHintsPerGame = classicHints
                ?.coerceIn(CLASSIC_HINTS_RANGE.first.toLong(), CLASSIC_HINTS_RANGE.last.toLong())?.toInt()
                ?: DEFAULT_CLASSIC_HINTS,
        )
    }
}

interface ConfigRepository {
    /** Values active for this app run (fetched values activate on the next start). */
    val config: StateFlow<AppConfig>
}

/** Used when Firebase isn't configured: the bundled defaults. */
class DefaultConfigRepository @Inject constructor() : ConfigRepository {
    private val state = MutableStateFlow(AppConfig())
    override val config: StateFlow<AppConfig> = state.asStateFlow()
}
