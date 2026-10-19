package com.puzzleblockblast.game.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme

/** Full-screen brand gradient that every screen sits on. */
@Composable
fun PbbBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(PbbTheme.backgroundGradient)),
        content = content,
    )
}
