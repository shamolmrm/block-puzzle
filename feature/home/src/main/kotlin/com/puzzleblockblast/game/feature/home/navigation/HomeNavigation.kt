package com.puzzleblockblast.game.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puzzleblockblast.game.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(
    onPlayClassic: () -> Unit,
    onPlayLevels: () -> Unit,
    onOpenSettings: () -> Unit,
    onHowToPlay: () -> Unit,
) {
    composable<HomeRoute> {
        HomeScreen(
            onPlayClassic = onPlayClassic,
            onPlayLevels = onPlayLevels,
            onOpenSettings = onOpenSettings,
            onHowToPlay = onHowToPlay,
        )
    }
}
