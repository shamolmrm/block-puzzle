package com.puzzleblockblast.game.feature.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puzzleblockblast.game.core.designsystem.component.AchievementText
import com.puzzleblockblast.game.core.designsystem.component.PbbBackground
import com.puzzleblockblast.game.core.model.progress.Achievements
import com.puzzleblockblast.game.core.model.progress.PlayerSnapshot

@Composable
fun StatsRoute(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    StatsScreen(snapshot = snapshot, onBack = onBack)
}

@Composable
fun StatsScreen(snapshot: PlayerSnapshot?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    PbbBackground(modifier) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.stats_back)) }
                Text(
                    stringResource(R.string.stats_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(end = 64.dp).semantics { heading() },
                )
            }
            if (snapshot == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            LazyColumn(
                modifier = Modifier.widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { PlayerCard(snapshot) }
                item {
                    Section(R.string.stats_classic) {
                        StatRow(R.string.stats_games, snapshot.classicGames.toString())
                        StatRow(R.string.stats_best_score, snapshot.bestClassicScore.toString())
                        StatRow(R.string.stats_best_combo, if (snapshot.bestCombo > 0) "×${snapshot.bestCombo}" else "–")
                        StatRow(R.string.stats_lines, snapshot.linesCleared.toString())
                    }
                }
                item {
                    Section(R.string.stats_levels) {
                        StatRow(R.string.stats_levels_done, "${snapshot.levelsCompleted} / ${snapshot.levelCount}")
                        StatRow(R.string.stats_stars, "★ ${snapshot.totalStars} / ${snapshot.levelCount * 3}")
                    }
                }
                item {
                    Section(R.string.stats_daily) {
                        StatRow(R.string.stats_daily_done, snapshot.dailyCompleted.toString())
                        StatRow(R.string.stats_streak, snapshot.currentStreak.toString())
                        StatRow(R.string.stats_best_streak, snapshot.bestStreak.toString())
                    }
                }
                item {
                    Text(
                        stringResource(
                            R.string.stats_achievements,
                            snapshot.unlockedAchievements.size,
                            Achievements.all.size,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                    )
                }
                items(Achievements.all, key = { it.id }) { achievement ->
                    AchievementRow(
                        title = stringResource(AchievementText.title(achievement.id)),
                        description = stringResource(AchievementText.description(achievement.id)),
                        unlocked = achievement.id in snapshot.unlockedAchievements,
                        progress = achievement.progress(snapshot),
                        target = achievement.target,
                    )
                }
                item { Box(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun PlayerCard(snapshot: PlayerSnapshot) {
    val level = snapshot.playerLevel
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.stats_player_level, level.level),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
        )
        LinearProgressIndicator(
            progress = { level.fraction },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            drawStopIndicator = {},
        )
        Text(
            stringResource(R.string.stats_xp, level.xpIntoLevel, level.xpForNext, snapshot.xp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun StatRow(@StringRes label: Int, value: String) {
    val labelText = stringResource(label)
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$labelText: $value" }) {
        Text(labelText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun AchievementRow(title: String, description: String, unlocked: Boolean, progress: Long, target: Long) {
    val status = if (unlocked) stringResource(R.string.stats_unlocked) else stringResource(R.string.stats_progress, progress, target)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (unlocked) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.12f),
            )
            .padding(12.dp)
            .clearAndSetSemantics { contentDescription = "$title. $description. $status" },
    ) {
        Text(if (unlocked) "🏆" else "🔒", style = MaterialTheme.typography.headlineSmall)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            if (!unlocked && target > 1) {
                LinearProgressIndicator(
                    progress = { progress.toFloat() / target },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(50)),
                    drawStopIndicator = {},
                )
            }
        }
        Text(status, style = MaterialTheme.typography.labelLarge)
    }
}
