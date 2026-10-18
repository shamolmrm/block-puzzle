package com.puzzleblockblast.game.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puzzleblockblast.game.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

fun NavGraphBuilder.settingsScreen(onBack: () -> Unit) {
    composable<SettingsDestination> { SettingsRoute(onBack = onBack) }
}
