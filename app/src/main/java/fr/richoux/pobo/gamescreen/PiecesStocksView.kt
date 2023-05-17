package fr.richoux.pobo.gamescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.richoux.pobo.engine.Piece
import fr.richoux.pobo.engine.PieceColor
import fr.richoux.pobo.engine.PieceType

private val PIECES_STOCK_SIZE = 48.dp
@Composable
fun PiecesStocksView(pool: List<Piece>, color: PieceColor,  modifier: Modifier = Modifier) {
    var numberPo = 0
    var numberBo = 0

    pool.forEach {
        if(it.type == PieceType.Po)
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
                piece = Piece.createPo(color),
                number = numberPo,
                modifier = Modifier
                    .width(PIECES_STOCK_SIZE)
                    .height(
                        PIECES_STOCK_SIZE
                    )
            )
            Spacer(modifier = Modifier.width(32.dp))
            PieceNumberView(
                piece = Piece.createBo(color),
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
