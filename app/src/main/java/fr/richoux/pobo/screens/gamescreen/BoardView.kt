package fr.richoux.pobo.screens.gamescreen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.richoux.pobo.engine.*
import fr.richoux.pobo.ui.BoardColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "pobotag BoardView"

@Composable
fun BoardView(
  viewModel: GameViewModel,
) {
  val boardViewState by viewModel.boardViewState.collectAsStateWithLifecycle()
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1f)
  ) {
    val squareSize = maxWidth / 6

    BoardBackground(
      board = boardViewState.board,
      boardViewState.lastMovePosition,
      boardViewState.tapAction,
      boardViewState.promotables.flatten(),
      boardViewState.promotionList,
      squareSize
    )

    BoardLayout(
      board = boardViewState.board,
      animations = viewModel.animations,
      squareSize = squareSize,
      isXP = viewModel.xp
    )
  }
}

@Composable
fun BoardBackground(
  board: Board,
  lastMove: Position?,
  onTap: (Position) -> Unit,
  promotionable: List<Position>,
  selected: List<Position>,
  squareSize: Dp
) {
  for(y in 0 until 6) {
    for(x in 0 until 6) {
      val position = Position(x, y)
      val white = y % 2 == x % 2
      val color = if(selected.contains(position)) {
        BoardColors.selected
      } else {
        if(position.isSame(lastMove)) {
          if(lastMove != null && board.getGridIDPosition(lastMove) != "") {
            if(white) BoardColors.lastMoveLight else BoardColors.lastMoveDark
          } else {
            if(white) BoardColors.lightSquare else BoardColors.darkSquare
          }
        } else {
          if(promotionable.contains(position))
            BoardColors.promotionable
          else
            if(white) BoardColors.lightSquare else BoardColors.darkSquare
        }
      }
      Box(
        modifier = Modifier
          .size( squareSize )
          .offset(
            Dp(x * squareSize.value),
            Dp(y * squareSize.value)
          )
          .background(color)
          .clickable(onClick = { onTap(position) })
      ) {
        if(y == 5) {
          Text(
            text = "${'a' + x}",
            modifier = Modifier.align(Alignment.BottomEnd),
            style = MaterialTheme.typography.caption,
            color = Color.Black.copy(0.5f)
          )
        }
        if(x == 0) {
          Text(
            text = "${6 - y}",
            modifier = Modifier.align(Alignment.TopStart),
            style = MaterialTheme.typography.caption,
            color = Color.Black.copy(0.5f)
          )
        }
      }
    }
  }
}

@Composable
private fun BoardLayout(
  board: Board,
  animations: MutableMap<Position, Pair<Position, Piece>>,
  squareSize: Dp,
  isXP: Boolean
) {
  val pieces = board.allPieces
  pieces.forEach { (position, piece) ->
    if(piece.id != "") {
      var willBeAnimated = false
      animations.forEach { (from, target) ->
        if(target.first == position) {
          willBeAnimated = true
        }
      }

      if(!willBeAnimated) {
        val offsetX = squareSize * position.x
        val offsetY = squareSize * position.y

        PieceView(
          piece = piece,
          modifier = Modifier.layoutId(piece.id).offset(offsetX, offsetY),
          squareSize
        )
      }
    }
  }
  animations.forEach { (from, target) ->
    val currentOffset = Offset(
      squareSize.value * from.x,
      squareSize.value * from.y
    )
    val targetOffset = Offset(
      squareSize.value * target.first.x,
      squareSize.value * target.first.y
    )
    val offset = remember { Animatable(currentOffset, Offset.VectorConverter) }
    // Hack to fix a bug where currentOffset remained to the last targetOffset value
    if(!offset.isRunning && offset.value != currentOffset && offset.value != targetOffset) {
      LaunchedEffect(currentOffset) {
        offset.snapTo(currentOffset)
      }
    }

    LaunchedEffect(targetOffset) {
        if(!isXP)
          offset.animateTo(targetOffset, tween(200, easing = LinearOutSlowInEasing))
        else
          offset.snapTo(targetOffset)
      }

    PieceView(
      piece = target.second,
      modifier = Modifier.offset(Dp(offset.value.x), Dp(offset.value.y)),
      squareSize
    )
  }
}

@Composable
fun PieceView(
  piece: Piece,
  modifier: Modifier = Modifier,
  squareSize: Dp
) {
  Image(
    painter = painterResource(id = piece.imageResource()),
    modifier = modifier
      .size( squareSize )
      .padding(4.dp),
    contentDescription = piece.id
  )
}

