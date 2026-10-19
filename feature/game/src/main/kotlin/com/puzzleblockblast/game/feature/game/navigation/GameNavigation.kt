package com.puzzleblockblast.game.feature.game.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.puzzleblockblast.game.feature.game.GameRoute
import com.puzzleblockblast.game.feature.game.level.LevelGameRoute
import com.puzzleblockblast.game.feature.game.tutorial.TutorialRoute
import kotlinx.serialization.Serializable

@Serializable
data object ClassicGameRoute

/** [thenPlay]: start a Classic game when the tutorial ends (first run) instead of going back. */
@Serializable
data class TutorialDestination(val thenPlay: Boolean)

/** Plays one level. The property name must match `LevelGameViewModel.LEVEL_ID_KEY`. */
@Serializable
data class LevelPlayRoute(val levelId: String)

fun NavGraphBuilder.levelPlayScreen(onLevels: () -> Unit, onNextLevel: (levelId: String) -> Unit) {
    composable<LevelPlayRoute> { LevelGameRoute(onLevels = onLevels, onNextLevel = onNextLevel) }
}

fun NavGraphBuilder.classicGameScreen(onBack: () -> Unit) {
    composable<ClassicGameRoute> { GameRoute(onBack = onBack) }
}

fun NavGraphBuilder.tutorialScreen(onDone: (thenPlay: Boolean) -> Unit) {
    composable<TutorialDestination> { entry ->
        val route = entry.toRoute<TutorialDestination>()
        TutorialRoute(onDone = { onDone(route.thenPlay) })
    }
}
