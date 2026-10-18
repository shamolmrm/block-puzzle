package com.puzzleblockblast.game.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.puzzleblockblast.game.feature.game.navigation.ClassicGameRoute
import com.puzzleblockblast.game.feature.game.navigation.classicGameScreen
import com.puzzleblockblast.game.feature.home.navigation.HomeRoute
import com.puzzleblockblast.game.feature.home.navigation.homeScreen
import com.puzzleblockblast.game.feature.settings.navigation.SettingsDestination
import com.puzzleblockblast.game.feature.settings.navigation.settingsScreen

@Composable
fun PbbNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        homeScreen(
            onPlayClassic = { navController.navigate(ClassicGameRoute) { launchSingleTop = true } },
            onOpenSettings = { navController.navigate(SettingsDestination) { launchSingleTop = true } },
        )
        classicGameScreen(onBack = navController::popBackStack)
        settingsScreen(onBack = navController::popBackStack)
    }
}
