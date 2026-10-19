package com.puzzleblockblast.game.feature.levels.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puzzleblockblast.game.feature.levels.LevelSelectRoute
import kotlinx.serialization.Serializable

@Serializable
data object LevelSelectDestination

fun NavGraphBuilder.levelSelectScreen(onOpenLevel: (levelId: String) -> Unit, onBack: () -> Unit) {
    composable<LevelSelectDestination> { LevelSelectRoute(onOpenLevel = onOpenLevel, onBack = onBack) }
}
