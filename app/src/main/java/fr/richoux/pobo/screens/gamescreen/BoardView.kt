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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import fr.richoux.pobo.engine.*
import fr.richoux.pobo.ui.BoardColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.jvm.internal.Ref.BooleanRef

private const val TAG = "pobotag BoardView"

@Composable
fun BoardView(
  board: Board,
  lastMove: Position?,
  onTap: (Position) -> Unit,
  promotionable: List<Position>,
  selected: List<Position>,
  animations: MutableMap<Position, Pair<Position,Piece>>,
  stringForDebug: String = ""
) {
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1f)
  ) {
    val squareSize = maxWidth / 6

    BoardBackground(lastMove, onTap, promotionable, selected, squareSize)
    BoardLayout(
      board = board,
      animations = animations,
      squareSize = squareSize,
      stringForDebug = stringForDebug
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
//      var xx = x
//      if(LocalLayoutDirection.current == LayoutDirection.Rtl) {
//        xx = 5 - x
//      }
      val position = Position(x, y)
      val white = y % 2 == x % 2
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

//TODO Rename to BoardLayoutAnimation, and make another BoardLayout?
@Composable
private fun BoardLayout(
  board: Board,
  animations: MutableMap<Position, Pair<Position,Piece>>,
  squareSize: Dp,
  stringForDebug: String = ""
) {
  stringForDebug
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
        //! TODO Is it really necessary?
//        val x = when(LocalLayoutDirection.current == LayoutDirection.Rtl) {
//          true -> 5 - position.x
//          false -> position.x
//        }
//        val y = position.y
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
//    val x = when(LocalLayoutDirection.current == LayoutDirection.Rtl) {
//      true -> 5 - from.x
//      false -> from.x
//    }
//    val y = from.y
    val currentOffset = Offset(squareSize.value * from.x, squareSize.value * from.y)

//    val toX = when(LocalLayoutDirection.current == LayoutDirection.Rtl) {
//      true -> 5 - target.first.x
//      false -> target.first.x
//    }
//    val toY = target.first.y
    val targetOffset =
      Offset(squareSize.value * target.first.x, squareSize.value * target.first.y)

    val offset = remember { Animatable(currentOffset, Offset.VectorConverter) }
    LaunchedEffect(targetOffset) {
//      while (isActive) {
      offset.animateTo(targetOffset, tween(200, easing = LinearOutSlowInEasing))
//      }
    }

    PieceView(
      piece = target.second,
//      modifier = Modifier.layoutId(target.second.id).offset(Dp(offset.value.x), Dp(offset.value.y)),
      modifier = Modifier.offset(Dp(offset.value.x), Dp(offset.value.y)),
      squareSize
    )
  }

}
