package com.puzzleblockblast.game.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puzzleblockblast.game.core.designsystem.component.PbbBackground
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.MoveOutcome
import com.puzzleblockblast.game.feature.game.board.BoardCanvas
import com.puzzleblockblast.game.feature.game.board.BoardGeometry
import com.puzzleblockblast.game.feature.game.board.ClearBurst
import com.puzzleblockblast.game.feature.game.board.DragGeometry
import com.puzzleblockblast.game.feature.game.board.PieceTray
import com.puzzleblockblast.game.feature.game.board.drawPiece

@Composable
fun GameRoute(
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Leaving the app mid-game pauses it; the game itself is already saved after every move.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onPause() }
    GameScreen(
        state = state,
        onPlace = viewModel::onPlace,
        onUndo = viewModel::onUndo,
        onHint = viewModel::onHint,
        onPause = viewModel::onPause,
        onResume = viewModel::onResume,
        onRestart = viewModel::onRestart,
        onMessageShown = viewModel::onMessageShown,
        onHome = onBack,
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    onPlace: (slot: Int, origin: GridPoint) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMessageShown: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PbbBackground(modifier = modifier) {
        val game = state.game
        if (state.isLoading || game == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@PbbBackground
        }

        val colors = PbbTheme.boardColors
        var rootOrigin by remember { mutableStateOf(Offset.Zero) }
        var geometry by remember { mutableStateOf<BoardGeometry?>(null) }
        var dragSlot by remember { mutableStateOf<Int?>(null) }
        var pointer by remember { mutableStateOf(Offset.Zero) }
        var dropColor by remember { mutableStateOf(Color.White) }
        val liftPx = with(LocalDensity.current) { DRAG_LIFT.toPx() }

        // Cancel any drag in progress when play stops (pause, game over, app backgrounded).
        LaunchedEffect(state.isPlaying) { if (!state.isPlaying) dragSlot = null }

        val dragPiece = dragSlot?.let { game.tray.getOrNull(it) }
        val boardGeometry = geometry
        val dragTopLeft = if (dragPiece != null && boardGeometry != null) {
            DragGeometry.pieceTopLeft(pointer, dragPiece.shape, boardGeometry.cellSize, liftPx)
        } else {
            null
        }
        val preview = if (dragPiece != null && boardGeometry != null && dragTopLeft != null) {
            DragGeometry.preview(game.board, dragSlot!!, dragPiece, DragGeometry.snapOrigin(dragTopLeft, boardGeometry))
        } else {
            null
        }
        val hintCells = state.hint?.let { hint -> game.tray.getOrNull(hint.slot)?.shape?.cellsAt(hint.origin) }.orEmpty()

        // Feedback for the last move: cleared cells burst, points float up over the board.
        val outcome = state.lastOutcome
        val feedback = remember { Animatable(1f) }
        LaunchedEffect(outcome, game.moves) {
            if (outcome != null && !outcome.clearedLines.isEmpty) {
                feedback.snapTo(0f)
                feedback.animateTo(1f, tween(FEEDBACK_MS, easing = FastOutSlowInEasing))
            }
        }
        val burst = outcome
            ?.takeIf { !it.clearedLines.isEmpty && feedback.value < 1f }
            ?.let { ClearBurst(it.clearedLines.cells(game.board).toList(), dropColor, feedback.value) }

        val snackbar = remember { SnackbarHostState() }
        val messageText = when (state.message) {
            GameMessage.NO_HINTS_LEFT -> stringResource(R.string.game_no_hints_left)
            GameMessage.NO_VALID_MOVE -> stringResource(R.string.game_no_valid_move)
            null -> null
        }
        LaunchedEffect(messageText) {
            if (messageText != null) {
                snackbar.showSnackbar(messageText)
                onMessageShown()
            }
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().onGloballyPositioned { rootOrigin = it.positionInRoot() },
        ) {
            // Board is as large as fits beside the HUD and tray, capped for tablets.
            val boardSize: Dp = minOf(maxWidth - 24.dp, maxHeight - 330.dp, 480.dp).coerceAtLeast(220.dp)

            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            ) {
                ScoreBar(
                    score = game.score,
                    best = state.displayBest,
                    combo = game.combo,
                    pauseEnabled = state.isPlaying,
                    onPause = onPause,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
                Box(contentAlignment = Alignment.Center) {
                    BoardCanvas(
                        board = game.board,
                        preview = preview,
                        hintCells = hintCells,
                        burst = burst,
                        onGeometry = { geometry = it },
                        modifier = Modifier.width(boardSize),
                    )
                    if (outcome != null && feedback.value < 1f) MovePopup(outcome, feedback.value)
                }
                PieceTray(
                    tray = game.tray,
                    slotsThatFit = state.slotsThatFit,
                    boardCellSize = geometry?.cellSize ?: 0f,
                    draggingSlot = dragSlot,
                    hintSlot = state.hint?.slot,
                    enabled = state.isPlaying,
                    onDragStart = { slot, p ->
                        dragSlot = slot
                        pointer = p
                    },
                    onDrag = { delta -> pointer += delta },
                    onDragEnd = {
                        val drop = preview
                        dragSlot = null
                        if (drop != null) {
                            dropColor = colors.tile(drop.tile)
                            onPlace(drop.slot, drop.origin)
                        }
                    },
                    onDragCancel = { dragSlot = null },
                    modifier = Modifier.widthIn(max = 480.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = onUndo, enabled = state.canUndo, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.game_undo, game.undosRemaining))
                    }
                    FilledTonalButton(onClick = onHint, enabled = state.canHint, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.game_hint, game.hintsRemaining))
                    }
                }
            }

            // The dragged piece floats above everything at board scale, with a soft drop shadow.
            if (dragPiece != null && dragTopLeft != null && boardGeometry != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val origin = dragTopLeft - rootOrigin
                    val shadow = boardGeometry.cellSize * 0.18f
                    translate(shadow, shadow) {
                        dragPiece.shape.cells.forEach { p ->
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.28f),
                                topLeft = origin + Offset(p.col * boardGeometry.cellSize, p.row * boardGeometry.cellSize),
                                size = Size(boardGeometry.cellSize, boardGeometry.cellSize),
                                cornerRadius = CornerRadius(boardGeometry.cellSize * 0.2f),
                            )
                        }
                    }
                    drawPiece(dragPiece.shape.cells, colors.tile(dragPiece.tile), boardGeometry.cellSize, origin)
                }
            }

            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding())

            when {
                game.isGameOver -> GameOverOverlay(
                    score = game.score,
                    best = state.displayBest,
                    isNewBest = state.isNewBest,
                    onPlayAgain = onRestart,
                    onHome = onHome,
                )
                state.isPaused -> PauseOverlay(onResume = onResume, onRestart = onRestart, onHome = onHome)
            }
        }
    }
}

private val DRAG_LIFT = 48.dp
private const val FEEDBACK_MS = 650

/** "+points" (plus combo / board-clear callouts) rising and fading over the board after a clear. */
@Composable
private fun MovePopup(outcome: MoveOutcome, progress: Float) {
    val rise = with(LocalDensity.current) { 56.dp.toPx() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            translationY = -rise * progress
            alpha = (1f - progress * progress)
            val pop = 0.8f + 0.4f * minOf(progress * 4f, 1f)
            scaleX = pop
            scaleY = pop
        },
    ) {
        val style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
        Text("+${outcome.score.total}", style = style, color = Color.White)
        if (outcome.score.combo >= 2) {
            Text(
                stringResource(R.string.game_combo, outcome.score.combo),
                style = MaterialTheme.typography.titleLarge,
                color = PbbTheme.boardColors.tile(2),
            )
        }
        if (outcome.score.boardClearBonus > 0) {
            Text(
                stringResource(R.string.game_board_clear),
                style = MaterialTheme.typography.titleLarge,
                color = PbbTheme.boardColors.tile(3),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ScoreBar(
    score: Int,
    best: Int,
    combo: Int,
    pauseEnabled: Boolean,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedScore by animateIntAsState(score, tween(400), label = "score")
    val pauseLabel = stringResource(R.string.game_pause)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = onPause,
            enabled = pauseEnabled,
            modifier = Modifier.size(52.dp).semantics { contentDescription = pauseLabel },
        ) {
            val iconColor = MaterialTheme.colorScheme.onSecondaryContainer
            Canvas(Modifier.size(18.dp)) {
                val bar = size.width * 0.32f
                drawRoundRect(iconColor, Offset.Zero, Size(bar, size.height), CornerRadius(bar / 3))
                drawRoundRect(iconColor, Offset(size.width - bar, 0f), Size(bar, size.height), CornerRadius(bar / 3))
            }
        }
        Column(
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.game_score),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Text(
                "$animatedScore",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (combo >= 2) {
                Text(
                    stringResource(R.string.game_combo, combo),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(min = 64.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                stringResource(R.string.game_best),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text("$best", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Preview
@Composable
private fun GameScreenPreview() {
    val game = ClassicGame()
    PbbTheme {
        GameScreen(
            state = GameUiState(isLoading = false, game = game.newGame(1L), slotsThatFit = listOf(true, true, true)),
            onPlace = { _, _ -> },
            onUndo = {},
            onHint = {},
            onPause = {},
            onResume = {},
            onRestart = {},
            onMessageShown = {},
            onHome = {},
        )
    }
}
