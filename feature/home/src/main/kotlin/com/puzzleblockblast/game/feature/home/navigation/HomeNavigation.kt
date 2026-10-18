package com.puzzleblockblast.game.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puzzleblockblast.game.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(
    onPlayClassic: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<HomeRoute> {
        HomeScreen(onPlayClassic = onPlayClassic, onOpenSettings = onOpenSettings)
    }
}
