package com.puzzleblockblast.game.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.puzzleblockblast.game.core.designsystem.component.PbbBackground
import com.puzzleblockblast.game.core.designsystem.component.PbbMenuButton
import com.puzzleblockblast.game.core.designsystem.component.drawBlock
import com.puzzleblockblast.game.core.designsystem.theme.LocalReducedMotion
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.model.progress.PlayerLevel

@Composable
fun HomeScreen(
    onPlayClassic: () -> Unit,
    onPlayLevels: () -> Unit,
    onPlayDaily: () -> Unit,
    onPlayEvents: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onHowToPlay: () -> Unit,
    modifier: Modifier = Modifier,
    profile: HomeProfile? = null,
) {
    val comingSoon = stringResource(R.string.home_coming_soon)
    val branding: @Composable () -> Unit = {
        BlockLogo()
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
    }
    val menu: @Composable () -> Unit = {
        Column(
                modifier = Modifier.widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PbbMenuButton(text = stringResource(R.string.home_classic), onClick = onPlayClassic, hero = true)
                PbbMenuButton(stringResource(R.string.home_levels), onPlayLevels)
                PbbMenuButton(
                    stringResource(R.string.home_daily),
                    onPlayDaily,
                    enabled = profile?.dailyEnabled != false,
                    supportingText = when {
                        profile == null -> null
                        !profile.dailyEnabled -> stringResource(R.string.home_daily_unavailable)
                        profile.dailyDoneToday -> stringResource(R.string.home_daily_done)
                        else -> stringResource(R.string.home_daily_new)
                    },
                )
                PbbMenuButton(
                    stringResource(R.string.home_events),
                    onPlayEvents,
                    enabled = profile?.eventDaysLeft != null,
                    supportingText = when {
                        profile == null -> null
                        profile.eventDaysLeft == null -> comingSoon
                        else -> pluralStringResource(R.plurals.home_event_days_left, profile.eventDaysLeft, profile.eventDaysLeft)
                    },
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onHowToPlay) {
                        Text(stringResource(R.string.home_how_to_play), style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = onOpenStats) {
                        Text(stringResource(R.string.home_stats), style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.home_settings), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
    }

    PbbBackground(modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            // Landscape phones and tablets: branding on the left, menu on the right, so the main
            // buttons are never below the fold.
            if (maxWidth > maxHeight * 1.15f) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (profile != null) ProfileBadge(profile, onOpenStats)
                        branding()
                    }
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { menu() }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    if (profile != null) ProfileBadge(profile, onOpenStats)
                    branding()
                    Spacer(Modifier.height(24.dp))
                    menu()
                }
            }
        }
    }
}

/** Decorative stack of blocks that gently bobs, one block at a time. */
@Composable
private fun BlockLogo() {
    val colors = PbbTheme.boardColors
    val reducedMotion = LocalReducedMotion.current
    val bob by rememberInfiniteTransition(label = "logo").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave",
    )
    val wave = if (reducedMotion) 0f else bob
    // (row, col, tile) on a 3x4 grid.
    val blocks = listOf(
        Triple(0, 1, 1), Triple(0, 2, 2),
        Triple(1, 0, 4), Triple(1, 1, 3), Triple(1, 2, 5), Triple(1, 3, 6),
        Triple(2, 0, 7), Triple(2, 1, 2), Triple(2, 3, 4),
    )
    Canvas(Modifier.size(width = 160.dp, height = 120.dp)) {
        val cell = size.width / 4
        val gap = cell * 0.07f
        blocks.forEachIndexed { i, (r, c, tile) ->
            val phase = ((wave * blocks.size) - i).coerceIn(0f, 1f)
            val lift = cell * 0.12f * (1f - kotlin.math.abs(phase * 2f - 1f))
            drawBlock(Offset(c * cell + gap, r * cell + gap - lift), cell - 2 * gap, colors.tile(tile))
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    PbbTheme { HomeScreen(
            onPlayClassic = {},
            onPlayLevels = {},
            onPlayDaily = {},
            onPlayEvents = {},
            onOpenStats = {},
            onOpenSettings = {},
            onHowToPlay = {},
            profile = HomeProfile(PlayerLevel(4, 60, 250), dailyDoneToday = false, streak = 3),
        ) }
}

/** Player level, XP towards the next level and daily streak; tapping opens Stats. */
@Composable
private fun ProfileBadge(profile: HomeProfile, onOpenStats: () -> Unit) {
    val level = profile.level
    val description = stringResource(R.string.home_profile_description, level.level, level.xpIntoLevel, level.xpForNext, profile.streak)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
            .clickable(onClick = onOpenStats)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
    ) {
        Text(
            stringResource(R.string.home_level_badge, level.level),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { level.fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                drawStopIndicator = {},
            )
            Text(
                stringResource(R.string.home_xp, level.xpIntoLevel, level.xpForNext),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (profile.streak > 0) {
            Text(stringResource(R.string.home_streak, profile.streak), style = MaterialTheme.typography.titleMedium)
        }
    }
}