package com.puzzleblockblast.game.feature.game.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puzzleblockblast.game.feature.game.GameRoute
import kotlinx.serialization.Serializable

@Serializable
data object ClassicGameRoute

fun NavGraphBuilder.classicGameScreen(onBack: () -> Unit) {
    composable<ClassicGameRoute> { GameRoute(onBack = onBack) }
}
