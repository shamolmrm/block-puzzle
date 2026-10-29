package com.puzzleblockblast.game.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.puzzleblockblast.game.core.designsystem.theme.LocalReducedMotion
import com.puzzleblockblast.game.core.engine.level.DailyChallenge
import com.puzzleblockblast.game.feature.game.navigation.ClassicGameRoute
import com.puzzleblockblast.game.feature.stats.navigation.StatsDestination
import com.puzzleblockblast.game.feature.stats.navigation.statsScreen
import com.puzzleblockblast.game.feature.game.navigation.LevelPlayRoute
import com.puzzleblockblast.game.feature.game.navigation.levelPlayScreen
import com.puzzleblockblast.game.feature.levels.navigation.EventDestination
import com.puzzleblockblast.game.feature.levels.navigation.LevelSelectDestination
import com.puzzleblockblast.game.feature.levels.navigation.eventScreen
import com.puzzleblockblast.game.feature.levels.navigation.levelSelectScreen
import com.puzzleblockblast.game.feature.game.navigation.TutorialDestination
import com.puzzleblockblast.game.feature.game.navigation.classicGameScreen
import com.puzzleblockblast.game.feature.game.navigation.tutorialScreen
import com.puzzleblockblast.game.feature.home.navigation.HomeRoute
import com.puzzleblockblast.game.feature.home.navigation.homeScreen
import com.puzzleblockblast.game.feature.settings.navigation.SettingsDestination
import com.puzzleblockblast.game.feature.settings.navigation.settingsScreen

@Composable
fun PbbNavHost(tutorialCompleted: Boolean, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val animate = !LocalReducedMotion.current
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        // Screens zoom in slightly and fade; "Reduced motion" switches transitions off.
        enterTransition = { if (animate) fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.96f) else EnterTransition.None },
        exitTransition = { if (animate) fadeOut(tween(200)) else ExitTransition.None },
        popEnterTransition = { if (animate) fadeIn(tween(250)) else EnterTransition.None },
        popExitTransition = { if (animate) fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.96f) else ExitTransition.None },
    ) {
        homeScreen(
            onPlayClassic = {
                // First run goes through the tutorial, which then starts Classic.
                val destination: Any = if (tutorialCompleted) ClassicGameRoute else TutorialDestination(thenPlay = true)
                navController.navigate(destination) { launchSingleTop = true }
            },
            onPlayLevels = { navController.navigate(LevelSelectDestination) { launchSingleTop = true } },
            // The Daily Challenge is a generated level with id D<epoch day>, played on the level screen.
            onPlayDaily = { day -> navController.navigate(LevelPlayRoute(DailyChallenge.idFor(day))) { launchSingleTop = true } },
            onPlayEvents = { navController.navigate(EventDestination) { launchSingleTop = true } },
            onOpenStats = { navController.navigate(StatsDestination) { launchSingleTop = true } },
            onOpenSettings = { navController.navigate(SettingsDestination) { launchSingleTop = true } },
            onHowToPlay = { navController.navigate(TutorialDestination(thenPlay = false)) { launchSingleTop = true } },
        )
        levelSelectScreen(
            onOpenLevel = { id -> navController.navigate(LevelPlayRoute(id)) { launchSingleTop = true } },
            onBack = navController::popBackStack,
        )
        // Event levels are played on the level screen with run ids like E01L03@2915.
        eventScreen(
            onOpenLevel = { runId -> navController.navigate(LevelPlayRoute(runId)) { launchSingleTop = true } },
            onBack = navController::popBackStack,
        )
        statsScreen(onBack = navController::popBackStack)
        levelPlayScreen(
            // Back to whatever list opened the level (Levels or Events); from a daily, Home.
            onLevels = navController::popBackStack,
            // "Next level" replaces the finished level, so Back still returns to the list it came from.
            onNextLevel = { id ->
                val current = navController.currentBackStackEntry?.destination?.id
                navController.navigate(LevelPlayRoute(id)) {
                    if (current != null) popUpTo(current) { inclusive = true }
                }
            },
        )
        tutorialScreen(
            onDone = { thenPlay ->
                if (thenPlay) {
                    navController.navigate(ClassicGameRoute) {
                        popUpTo<TutorialDestination> { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            },
        )
        classicGameScreen(onBack = navController::popBackStack)
        settingsScreen(onBack = navController::popBackStack)
    }
}
