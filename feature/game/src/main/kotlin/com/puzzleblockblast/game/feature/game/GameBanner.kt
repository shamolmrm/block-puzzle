package com.puzzleblockblast.game.feature.game

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.puzzleblockblast.game.core.monetization.ads.BannerAd

/**
 * The gameplay banner: anchored at the bottom, below Undo / Hint. Pieces are dragged up from the tray
 * toward the board, so a drag never ends on it (accidental clicks are invalid traffic). It's hidden
 * whenever an overlay (level results) would cover it.
 */
@Composable
internal fun GameBanner(
    visible: Boolean,
    create: (Context, Int) -> BannerAd?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val banner = remember(widthDp) { create(context, widthDp) } ?: return@BoxWithConstraints
        DisposableEffect(banner) { onDispose { banner.destroy() } }
        LaunchedEffect(banner, visible) { banner.setVisible(visible) }
        AndroidView(
            factory = { banner.view.apply { (parent as? ViewGroup)?.removeView(this) } },
            modifier = Modifier.fillMaxWidth().height(banner.heightDp.dp),
        )
    }
}
