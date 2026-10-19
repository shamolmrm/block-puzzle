package com.puzzleblockblast.game.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme

/** Full-screen brand gradient that every screen sits on. Text on it uses the background's content colour. */
@Composable
fun PbbBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(PbbTheme.backgroundGradient)),
            content = content,
        )
    }
}
