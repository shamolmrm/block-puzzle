package com.puzzleblockblast.game.feature.game.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.puzzleblockblast.game.feature.game.GameRoute
import com.puzzleblockblast.game.feature.game.tutorial.TutorialRoute
import kotlinx.serialization.Serializable

@Serializable
data object ClassicGameRoute

/** [thenPlay]: start a Classic game when the tutorial ends (first run) instead of going back. */
@Serializable
data class TutorialDestination(val thenPlay: Boolean)

fun NavGraphBuilder.classicGameScreen(onBack: () -> Unit) {
    composable<ClassicGameRoute> { GameRoute(onBack = onBack) }
}

fun NavGraphBuilder.tutorialScreen(onDone: (thenPlay: Boolean) -> Unit) {
    composable<TutorialDestination> { entry ->
        val route = entry.toRoute<TutorialDestination>()
        TutorialRoute(onDone = { onDone(route.thenPlay) })
    }
}
