package com.puzzleblockblast.game.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Dims the game and blocks touches to it while a dialog card is shown. */
@Composable
private fun OverlayScrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        val pop = remember { Animatable(0.85f) }
        LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        Card(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 360.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                },
        ) {
            Column(
                modifier = Modifier.padding(28.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { content() }
        }
    }
}

@Composable
internal fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onHome: () -> Unit) {
    var confirmingRestart by remember { mutableStateOf(false) }
    OverlayScrim {
        if (confirmingRestart) {
            Text(
                stringResource(R.string.game_restart_confirm),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { confirmingRestart = false }) { Text(stringResource(R.string.game_cancel)) }
                Button(onClick = onRestart) { Text(stringResource(R.string.game_restart)) }
            }
        } else {
            Text(
                stringResource(R.string.game_paused),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            OverlayButton(stringResource(R.string.game_resume), onResume, primary = true)
            OverlayButton(stringResource(R.string.game_restart), { confirmingRestart = true })
            OverlayButton(stringResource(R.string.game_home), onHome)
        }
    }
}

@Composable
internal fun GameOverOverlay(score: Int, best: Int, isNewBest: Boolean, onPlayAgain: () -> Unit, onHome: () -> Unit) {
    OverlayScrim {
        Text(
            stringResource(R.string.game_over_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.game_over_score, score),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary,
        )
        if (isNewBest) {
            Text(
                stringResource(R.string.game_over_new_best),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Text(stringResource(R.string.game_over_best, best), style = MaterialTheme.typography.titleMedium)
        }
        OverlayButton(stringResource(R.string.game_play_again), onPlayAgain, primary = true)
        OverlayButton(stringResource(R.string.game_home), onHome)
    }
}

@Composable
private fun OverlayButton(text: String, onClick: () -> Unit, primary: Boolean = false) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    if (primary) {
        Button(onClick = onClick, modifier = modifier) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
    }
}
