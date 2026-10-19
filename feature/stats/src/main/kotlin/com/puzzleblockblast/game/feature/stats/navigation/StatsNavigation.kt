package com.puzzleblockblast.game.feature.stats.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puzzleblockblast.game.feature.stats.StatsRoute
import kotlinx.serialization.Serializable

@Serializable
data object StatsDestination

fun NavGraphBuilder.statsScreen(onBack: () -> Unit) {
    composable<StatsDestination> { StatsRoute(onBack = onBack) }
}
