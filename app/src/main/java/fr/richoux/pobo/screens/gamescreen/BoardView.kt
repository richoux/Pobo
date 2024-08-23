package fr.richoux.pobo.screens.gamescreen

import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import fr.richoux.pobo.engine.*
import fr.richoux.pobo.ui.BoardColors

private const val TAG = "pobotag BoardView"

@Composable
fun BoardView(
  modifier: Modifier = Modifier,
  board: Board,
  lastMove: Position?,
  onTap: (Position) -> Unit,
  promotionable: List<Position>,
  selected: List<Position>
) {
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1f)
  ) {
    val squareSize = maxWidth / 6

    BoardBackground(lastMove, onTap, promotionable, selected, squareSize)
    BoardLayout(
      pieces = board.allPieces,
      modifier = modifier,
      squareSize = squareSize
    )
  }
}

@Composable
fun BoardBackground(
  lastMove: Position?,
  onTap: (Position) -> Unit,
  promotionable: List<Position>,
  selected: List<Position>,
  squareSize: Dp
) {
  for(y in 0 until 6) {
    for(x in 0 until 6) {
      var xx = x
      if(LocalLayoutDirection.current == LayoutDirection.Rtl) {
        xx = 5 - x
      }
      val position = Position(xx, y)
      val white = y % 2 == xx % 2
      val color = if(position.isSame(lastMove)) {
        if(white) BoardColors.lastMoveLight else BoardColors.lastMoveDark
      } else {
        if(selected.contains(position))
          BoardColors.selected
        else {
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
            Dp(xx * squareSize.value),
            Dp(y * squareSize.value)
          )
          .background(color)
          .clickable(onClick = { onTap(position) })
      ) {
        if(y == 5) {
          Text(
            text = "${'a' + xx}",
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
fun PieceView(
  piece: Piece,
  modifier: Modifier = Modifier,
  squareSize: Dp,
  offsetX: Dp,
  offsetY: Dp
) {
  Image(
    painter = painterResource(id = piece.imageResource()),
    modifier = modifier
      .size( squareSize )
      .padding(4.dp)
      .offset(offsetX, offsetY),
    contentDescription = piece.id
  )
}

@Composable
fun PieceNumberView(piece: Piece, number: Int, modifier: Modifier = Modifier) {
  Row()
  {
    Image(
      painter = painterResource(id = piece.imageResource()),
      modifier = modifier.padding(4.dp),
      contentDescription = piece.id
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = number.toString(),
      style = MaterialTheme.typography.h4
    )
  }
}

@Composable
private fun BoardLayout(
  modifier: Modifier = Modifier,
  pieces: List<Pair<Position, Piece>>,
  squareSize: Dp
) {
  //  val fromPosition = properties.fromState.board.find(piece)?.position
//  val currentOffset = fromPosition
//    ?.toCoordinate(properties.isFlipped)
//    ?.toOffset(properties.squareSize)
//
//  val targetOffset = toPosition
//    .toCoordinate(properties.isFlipped)
//    .toOffset(properties.squareSize)

  pieces.forEach { (position, piece) ->
    if(piece.id != "") {
      val x = when(LocalLayoutDirection.current == LayoutDirection.Rtl) {
        true -> 5 - position.x
        false -> position.x
      }
      val y = position.y
      val offsetX = squareSize * x
      val offsetY = squareSize * y
      PieceView(
        piece = piece,
        modifier = Modifier.layoutId(piece.id),
        squareSize,
        offsetX,
        offsetY
      )
    }
  }
}
