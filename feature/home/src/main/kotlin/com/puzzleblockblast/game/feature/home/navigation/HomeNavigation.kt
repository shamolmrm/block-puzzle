package com.puzzleblockblast.game.feature.home.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puzzleblockblast.game.feature.home.HomeScreen
import com.puzzleblockblast.game.feature.home.HomeViewModel
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(
    onPlayClassic: () -> Unit,
    onPlayLevels: () -> Unit,
    onPlayDaily: (epochDay: Long) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onHowToPlay: () -> Unit,
) {
    composable<HomeRoute> {
        val viewModel: HomeViewModel = hiltViewModel()
        val profile by viewModel.profile.collectAsStateWithLifecycle()
        HomeScreen(
            onPlayClassic = onPlayClassic,
            onPlayLevels = onPlayLevels,
            onPlayDaily = { onPlayDaily(viewModel.todayEpochDay()) },
            onOpenStats = onOpenStats,
            onOpenSettings = onOpenSettings,
            onHowToPlay = onHowToPlay,
            profile = profile,
        )
    }
}
