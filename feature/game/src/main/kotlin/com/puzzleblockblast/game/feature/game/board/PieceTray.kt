package com.puzzleblockblast.game.feature.game.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.engine.classic.Piece
import com.puzzleblockblast.game.feature.game.R

/**
 * The offered pieces. Each slot is a large touch target; dragging starts after touch slop and
 * reports pointer positions in root coordinates.
 */
@Composable
fun PieceTray(
    tray: List<Piece?>,
    slotsThatFit: List<Boolean>,
    boardCellSize: Float,
    draggingSlot: Int?,
    hintSlot: Int?,
    enabled: Boolean,
    onDragStart: (slot: Int, pointerInRoot: Offset) -> Unit,
    onDrag: (delta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(PbbTheme.boardColors.boardBackground.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
    ) {
        tray.forEachIndexed { slot, piece ->
            TraySlot(
                piece = piece,
                slot = slot,
                fits = slotsThatFit.getOrElse(slot) { true },
                boardCellSize = boardCellSize,
                hidden = draggingSlot == slot,
                highlighted = hintSlot == slot,
                enabled = enabled && piece != null,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TraySlot(
    piece: Piece?,
    slot: Int,
    fits: Boolean,
    boardCellSize: Float,
    hidden: Boolean,
    highlighted: Boolean,
    enabled: Boolean,
    onDragStart: (Int, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PbbTheme.boardColors
    var slotOrigin by remember { mutableStateOf(Offset.Zero) }
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    val description = when {
        piece == null -> stringResource(R.string.game_slot_empty, slot + 1)
        fits -> stringResource(R.string.game_slot_piece, slot + 1, piece.shape.size)
        else -> stringResource(R.string.game_slot_piece_no_fit, slot + 1, piece.shape.size)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { slotOrigin = it.positionInRoot() }
            .then(
                if (highlighted) Modifier.border(3.dp, colors.ghostValid.copy(alpha = 1f), RoundedCornerShape(20.dp)) else Modifier,
            )
            .semantics { contentDescription = description }
            .then(
                if (enabled) {
                    Modifier.pointerInput(slot, piece) {
                        detectDragGestures(
                            onDragStart = { local -> currentOnDragStart(slot, slotOrigin + local) },
                            onDrag = { change, amount ->
                                change.consume()
                                currentOnDrag(amount)
                            },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragCancel() },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        if (piece != null && !hidden) {
            // A newly dealt piece pops in; keyed on the piece so a returned drag doesn't replay it.
            val pop = remember(piece) { Animatable(0.4f) }
            LaunchedEffect(piece) {
                pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
            }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                        alpha = if (fits) 1f else 0.3f
                    },
            ) {
                // Preview scale: fit a 5x5 piece in the slot, never larger than 75% of a board cell.
                val cell = minOf(size.width, size.height) / 5.2f
                val trayCell = if (boardCellSize > 0f) minOf(cell, boardCellSize * 0.75f) else cell
                val origin = Offset(
                    (size.width - piece.shape.width * trayCell) / 2f,
                    (size.height - piece.shape.height * trayCell) / 2f,
                )
                drawPiece(piece.shape.cells, colors.tile(piece.tile), trayCell, origin)
            }
        }
    }
}
