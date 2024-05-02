package fr.richoux.pobo.screens.gamescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.richoux.pobo.engine.*

private val PIECES_STOCK_SIZE = 48.dp

@Composable
fun PiecesStocksView(pool: List<Byte>, color: Color, modifier: Modifier = Modifier) {
  var numberPo = 0
  var numberBo = 0

  pool.forEach {
    if(it == PieceType.Po.value)
      numberPo++
    else
      numberBo++
  }

  //Column() {
  Row(
    modifier = Modifier
      .background(MaterialTheme.colors.surface)
      .height(PIECES_STOCK_SIZE)
      .then(modifier),
    horizontalArrangement = Arrangement.Center
  ) {
//            pool.forEach {
//                PieceView(
//                    piece = it, modifier = Modifier
//                        .width(PIECES_STOCK_SIZE)
//                        .height(
//                            PIECES_STOCK_SIZE
//                        )
//                )
//        }
    PieceNumberView(
      piece = getPoInstanceOfColor(color),
      number = numberPo,
      modifier = Modifier
        .width(PIECES_STOCK_SIZE)
        .height(
          PIECES_STOCK_SIZE
        )
    )
    Spacer(modifier = Modifier.width(32.dp))
    PieceNumberView(
      piece = getBoInstanceOfColor(color),
      number = numberBo,
      modifier = Modifier
        .width(PIECES_STOCK_SIZE)
        .height(
          PIECES_STOCK_SIZE
        )
    )
  }
  //}
}
