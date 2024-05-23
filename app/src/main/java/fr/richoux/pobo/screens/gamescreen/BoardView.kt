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
  selected: List<Position>,
  landscapeMode: Boolean = false
) {
  // Get local density from composable
  val localDensity = LocalDensity.current

  // Create element height in dp state
  var columnHeightDp by remember { mutableStateOf(0.dp) }

  var modifier = modifier.aspectRatio(1.0f)
  if(landscapeMode)
    modifier = modifier.fillMaxHeight()
  else
    modifier = modifier.fillMaxWidth()

  modifier = modifier.onGloballyPositioned { coordinates ->
    // Set column height using the LayoutCoordinates
    columnHeightDp = with(localDensity) { coordinates.size.height.toDp() }
  }

  Box(modifier) {
    BoardBackground(lastMove, onTap, promotionable, selected)
    BoardLayout(
      pieces = board.allPieces,
      modifier = modifier
    )
  }
}

@Composable
fun BoardBackground(
  lastMove: Position?,
  onTap: (Position) -> Unit,
  promotionable: List<Position>,
  selected: List<Position>
) {
  Column {
    for(y in 0 until 6) {
      Row {
        for(x in 0 until 6) {
          var xx = x
          if( LocalLayoutDirection.current == LayoutDirection.Rtl ) {
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
              .weight(1f)
              .background(color)
              .aspectRatio(1.0f)
              .clickable(
                onClick = { onTap(position) }
              )
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
  pieces: List<Pair<Position, Piece>>
) {
  val constraints: ConstraintSet = constraintsFor(pieces, LocalLayoutDirection.current == LayoutDirection.Rtl)

  ConstraintLayout(
    modifier = modifier,
    animateChanges = true,
    animationSpec = spring(),
    constraintSet = constraints
  ) {
    pieces.forEach { (_, piece) ->
      if(piece.id != "")
        PieceView(piece = piece, modifier = Modifier.layoutId(piece.id))
    }
  }
}

private fun constraintsFor(pieces: List<Pair<Position, Piece>>, rtl: Boolean): ConstraintSet {
  return ConstraintSet {
    val horizontalGuidelines = (0..6).map { createGuidelineFromAbsoluteLeft(it.toFloat() / 6f) }
    val verticalGuidelines = (0..6).map { createGuidelineFromTop(it.toFloat() / 6f) }
    pieces.forEach { (position, piece) ->
      val pieceRef = createRefFor(piece.id)
      constrain(pieceRef) {
        top.linkTo(verticalGuidelines[position.y])
        bottom.linkTo(verticalGuidelines[position.y + 1])
        if( rtl ) {
          start.linkTo(horizontalGuidelines[position.x + 1])
          end.linkTo((horizontalGuidelines[position.x]))
        }
        else {
          start.linkTo(horizontalGuidelines[position.x])
          end.linkTo((horizontalGuidelines[position.x + 1]))
        }
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
      }
    }
  }
}
