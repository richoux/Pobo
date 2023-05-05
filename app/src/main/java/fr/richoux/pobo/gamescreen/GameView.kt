package fr.richoux.pobo.gamescreen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.richoux.pobo.engine.*
import kotlinx.coroutines.flow.count

private const val TAG = "pobotag GameView"

@Composable
fun GameActions(viewModel: GameViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    val hasChoiceOfPiece = gameState == GameState.SELECTPIECE && viewModel.twoTypesInPool()
    IconButton(
        onClick = { viewModel.selectPo() },
        enabled = hasChoiceOfPiece
    ) {
        Icon(Icons.Filled.Face, contentDescription = "Select Po")
    }
    IconButton(
        onClick = { viewModel.selectBo() },
        enabled = hasChoiceOfPiece
    ) {
        Icon(Icons.Filled.Person, contentDescription = "Select Bo")
    }

    val completeSelectionForRemoval =
        gameState == GameState.SELECTREMOVAL
                && ( ( viewModel.state == GameViewModelState.SELECT3 && viewModel.piecesToPromote.size == 3)
                       || (viewModel.state == GameViewModelState.SELECT1 && viewModel.piecesToPromote.size == 1) )
    IconButton(
        onClick = { viewModel.validateRemoval() },
        enabled = completeSelectionForRemoval
    ) {
        Icon(Icons.Filled.Done, contentDescription = "OK")
    }
    IconButton(
        onClick = { viewModel.cancelPieceSelection() },
        enabled = gameState == GameState.SELECTPOSITION && viewModel.twoTypesInPool()
    ) {
        Icon(Icons.Filled.Clear, contentDescription = "Return to piece selection")
    }
    Spacer(modifier = Modifier.width(48.dp))
    IconButton(
        onClick = { viewModel.goBackMove() },
        enabled = viewModel.canGoBack
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Undo Move")
    }

    IconButton(
        onClick = { viewModel.goForwardMove() },
        enabled = viewModel.canGoForward
    ) {
        Icon(Icons.Filled.ArrowForward, contentDescription = "Redo Move")
    }
}

@Composable
fun MainView(
    board: Board,
    lastMove: Position? = null,
    onTap: (Position) -> Unit = { _ -> },
    displayGameState: String =  ""
) {
    Column(Modifier.fillMaxHeight()) {
        BoardView(
            board = board,
            lastMove = lastMove,
            onTap = onTap
        )
        PiecesStocksView(
            pool = board.getPlayerPool(PieceColor.Blue),
            Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        PiecesStocksView(
            pool = board.getPlayerPool(PieceColor.Red),
            Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = displayGameState,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun GameView(viewModel: GameViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    val board = viewModel.currentBoard
    val player = viewModel.currentPlayer
    var lastMove: Position? by remember { mutableStateOf(null) }

    when (gameState) {
        GameState.INIT -> {
            MainView(board, displayGameState = viewModel.displayGameState)
            viewModel.goToNextState()
        }
        GameState.PLAY -> {
            if(viewModel.historyCall)
                lastMove = null
            MainView(board, lastMove = lastMove, displayGameState = viewModel.displayGameState)
            viewModel.nextTurn()
            viewModel.goToNextState()
        }
        GameState.SELECTPIECE -> {
            MainView(board, lastMove = lastMove, displayGameState = viewModel.displayGameState)
        }
        GameState.SELECTPOSITION -> {
            val onSelect: (Position) -> Unit = {
                if (viewModel.canPlayAt(it)) {
                    lastMove = it
                    viewModel.playAt(it)
                }
            }
            MainView(board, lastMove = lastMove, onTap = onSelect, displayGameState = viewModel.displayGameState)
        }
        GameState.CHECKGRADUATION -> {
            MainView(board, displayGameState = viewModel.displayGameState)
            viewModel.goToNextState()
        }
        GameState.SELECTREMOVAL -> {
            val onSelect: (Position) -> Unit = {
                viewModel.selectForRemovalOrCancel(it)
            }
            MainView(board, lastMove = lastMove, onTap = onSelect, displayGameState = viewModel.displayGameState)
        }
        GameState.END -> {
            AlertDialog(
                onDismissRequest = {},
                buttons = {},
                title = {
                    Text(text = "$player wins!")
                },
                text = {
                    Text(text = "Winner: $player")
                }
            )
        }
    }
}
