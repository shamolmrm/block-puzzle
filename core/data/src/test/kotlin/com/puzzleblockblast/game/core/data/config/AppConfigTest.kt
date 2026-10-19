package com.puzzleblockblast.game.core.data.config

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppConfigTest {

    @Test
    fun missingValuesUseBundledDefaults() {
        assertThat(AppConfig.from(null, null)).isEqualTo(AppConfig())
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        assertThat(AppConfig.from(true, 99).classicHintsPerGame).isEqualTo(5)
        assertThat(AppConfig.from(true, -4).classicHintsPerGame).isEqualTo(1)
        assertThat(AppConfig.from(false, 2)).isEqualTo(AppConfig(dailyChallengeEnabled = false, classicHintsPerGame = 2))
    }
}
