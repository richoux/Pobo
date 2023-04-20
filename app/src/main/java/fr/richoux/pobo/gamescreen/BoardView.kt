package fr.richoux.pobo.gamescreen

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import fr.richoux.pobo.engine.Board
import fr.richoux.pobo.engine.Game
import fr.richoux.pobo.engine.Move
import fr.richoux.pobo.engine.Piece
import fr.richoux.pobo.engine.PieceColor
import fr.richoux.pobo.engine.Position
import fr.richoux.pobo.ui.BoardColors

@Composable
fun GameView(
    modifier: Modifier = Modifier,
    game: Game,
    selection: Position?,
    moves: List<Position>,
    didTap: (Position) -> Unit
){
    Box(modifier) {
        val board = game.board

        // Highlight king in check, could potentially highlight other things here
        val dangerPositions = listOf(PieceColor.White, PieceColor.Black).mapNotNull {
            if (game.kingIsInCheck(it))
                game.kingPosition(it)
            else
                null
        }

        BoardBackground(game.history.lastOrNull(), selection, dangerPositions, didTap)
        BoardLayout(
            pieces = board.allPieces,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.0f)
        )
        MovesView(board, moves)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MovesView(board: Board, moves: List<Position>) {
    Column {
        for (y in 0 until 6) {
            Row {
                for (x in 0 until 6) {
                    val position = Position(x, y)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.0f)
                    ) {
                        val piece = board.pieceAt(position)
                        val selected = moves.contains(position)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = selected,
                            modifier = Modifier.matchParentSize(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ){
                            val color = if (piece != null) BoardColors.attackColor else BoardColors.moveColor
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoardBackground(
    lastMove: Move?,
    selection: Position?,
    dangerPositions: List<Position>,
    didTap: (Position) -> Unit
){
    Column {
        for (y in 0 until 6) {
            Row {
                for (x in 0 until 6) {
                    val position = Position(x, y)
                    val white = y % 2 == x % 2
                    val color = if (lastMove?.contains(position) == true || position == selection) {
                        BoardColors.lastMoveColor
                    } else if (dangerPositions.contains(position)) {
                        BoardColors.checkColor
                    } else {
                        if (white) BoardColors.lightSquare else BoardColors.darkSquare
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(color)
                            .aspectRatio(1.0f)
                            .clickable(
                                onClick = { didTap(position) }
                            )
                    ) {
                        if (y == 5) {
                            Text(
                                text = "${'a' + x}",
                                modifier = Modifier.align(Alignment.BottomEnd),
                                style = MaterialTheme.typography.caption,
                                color = Color.Black.copy(0.5f)
                            )
                        }
                        if (x == 0) {
                            Text(
                                text = "${8 - y}",
                                modifier = Modifier.align(Alignment.TopStart),
                                style = MaterialTheme.typography.caption,
                                color = Color.Black.copy(0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceView(piece: Piece, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = piece.imageResource()),
        modifier = modifier.padding(4.dp),
        contentDescription = piece.id
    )
}

@Composable
private fun BoardLayout(
    modifier: Modifier = Modifier,
    pieces: List<Pair<Position, Piece>>
) {
    val constraints: ConstraintSet = constraintsFor(pieces)

    ConstraintLayout(
        modifier = modifier,
        animateChanges = true,
        animationSpec = spring(),
        constraintSet = constraints
    ) {
        pieces.forEach { (_, piece) ->
            PieceView(piece = piece, modifier = Modifier.layoutId(piece.id))
        }
    }
}

private fun constraintsFor(pieces: List<Pair<Position, Piece>>): ConstraintSet {
    return ConstraintSet {
        val horizontalGuidelines = (0..6).map { createGuidelineFromAbsoluteLeft(it.toFloat() / 6f) }
        val verticalGuidelines = (0..6).map { createGuidelineFromTop(it.toFloat() / 6f) }
        pieces.forEach { (position, piece) ->
            val pieceRef = createRefFor(piece.id)
            constrain(pieceRef) {
                top.linkTo(verticalGuidelines[position.y])
                bottom.linkTo(verticalGuidelines[position.y + 1])
                start.linkTo(horizontalGuidelines[position.x])
                end.linkTo((horizontalGuidelines[position.x + 1]))
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }
        }
    }
}
