package fr.richoux.pobo.screens.gamescreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.richoux.pobo.engine.*

private val PIECES_STOCK_SIZE = 48.dp

@Composable
fun PiecesStocksView(
  numberPo: Int,
  numberBo: Int,
  color: Color
) {
  Row(
    modifier = Modifier
      .background(MaterialTheme.colors.surface)
      .height(PIECES_STOCK_SIZE)
      .fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
  ) {
    PieceNumberView(
      piece = getPoInstanceOfColor(color),
      number = numberPo
    )
    Spacer(modifier = Modifier.width(32.dp))
    PieceNumberView(
      piece = getBoInstanceOfColor(color),
      number = numberBo
    )
  }
}

@Composable
fun PieceNumberView(piece: Piece, number: Int) {
  Row()
  {
    Image(
      painter = painterResource(id = piece.imageResource()),
      modifier = Modifier
        .width(PIECES_STOCK_SIZE)
        .height(PIECES_STOCK_SIZE)
        .padding(4.dp),
      contentDescription = piece.id
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = number.toString(),
      style = MaterialTheme.typography.h4
    )
  }
}