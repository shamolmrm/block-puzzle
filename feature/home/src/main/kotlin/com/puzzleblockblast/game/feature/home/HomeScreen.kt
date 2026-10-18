package com.puzzleblockblast.game.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.puzzleblockblast.game.core.designsystem.component.PbbMenuButton
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme

@Composable
fun HomeScreen(
    onPlayClassic: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val comingSoon = stringResource(R.string.home_coming_soon)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PbbMenuButton(text = stringResource(R.string.home_classic), onClick = onPlayClassic)
                // Implemented in later phases; visible but disabled so the menu layout is final.
                PbbMenuButton(stringResource(R.string.home_levels), {}, enabled = false, supportingText = comingSoon)
                PbbMenuButton(stringResource(R.string.home_daily), {}, enabled = false, supportingText = comingSoon)
                PbbMenuButton(stringResource(R.string.home_events), {}, enabled = false, supportingText = comingSoon)
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.home_settings))
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    PbbTheme { HomeScreen(onPlayClassic = {}, onOpenSettings = {}) }
}
