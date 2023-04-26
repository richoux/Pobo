package fr.richoux.pobo.gamescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.richoux.pobo.engine.Piece

private val PIECES_STOCK_SIZE = 32.dp
@Composable
fun PiecesStocksView(pool: List<Piece>, reserve: List<Piece>, modifier: Modifier = Modifier) {
    Column() {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .height(PIECES_STOCK_SIZE)
                .then(modifier)
        ) {
            pool.forEach {
                PieceView(
                    piece = it, modifier = Modifier
                        .width(PIECES_STOCK_SIZE)
                        .height(
                            PIECES_STOCK_SIZE
                        )
                )
            }
        }
        Row(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .height(PIECES_STOCK_SIZE)
                .then(modifier)
        ) {
            reserve.forEach {
                PieceView(
                    piece = it, modifier = Modifier
                        .width(PIECES_STOCK_SIZE)
                        .height(
                            PIECES_STOCK_SIZE
                        )
                )
            }
        }
    }
}
