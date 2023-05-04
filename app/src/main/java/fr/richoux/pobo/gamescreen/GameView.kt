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
private var countGameActions: Int = 0
private var countGameView: Int = 0

@Composable
fun GameActions(viewModel: GameViewModel = viewModel()) {
    Log.d(TAG, "GameActions call")
    countGameActions++
    val gameState by viewModel.gameState.collectAsState()
    IconButton(
        onClick = { viewModel.selectPo() },
        enabled = gameState == GameState.SELECTPIECE
    ) {
        Icon(Icons.Filled.Face, contentDescription = "Select Po")
    }
    IconButton(
        onClick = { viewModel.selectBo() },
        enabled = gameState == GameState.SELECTPIECE
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
        enabled = gameState == GameState.SELECTPOSITION
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
    Log.d(TAG, "MainView call")
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
    Log.d(TAG, "GameView call")
    val gameState by viewModel.gameState.collectAsState()
    countGameView++
    val board = viewModel.currentBoard
    val player = viewModel.currentPlayer
    var lastMove: Position? by remember { mutableStateOf(null) }

    when (gameState) {
        GameState.INIT -> {
            Log.d(TAG, "GameState.Init, player $player")
            MainView(board, displayGameState = viewModel.displayGameState)
            viewModel.goToNextState()
        }
        GameState.PLAY -> {
            Log.d(TAG, "GameState.Play, player $player")
            MainView(board, lastMove = lastMove, displayGameState = viewModel.displayGameState)
            viewModel.nextTurn()
            viewModel.goToNextState()
        }
        GameState.SELECTPIECE -> {
            Log.d(TAG, "GameState.SelectPiece, player $player")
            MainView(board, lastMove = lastMove, displayGameState = viewModel.displayGameState)
        }
        GameState.SELECTPOSITION -> {
            Log.d(TAG, "GameState.SelectPosition, player $player")
            val onSelect: (Position) -> Unit = {
                if (viewModel.canPlayAt(it)) {
                    lastMove = it
                    Log.d(TAG, "GameState.SelectPosition: player $player, position=$it")
                    viewModel.playAt(it)
                }
            }
            MainView(board, lastMove = lastMove, onTap = onSelect, displayGameState = viewModel.displayGameState)
        }
        GameState.CHECKGRADUATION -> {
            Log.d(TAG, "GameState.CheckGraduation, player $player")
            MainView(board, displayGameState = viewModel.displayGameState)
            viewModel.goToNextState()
        }
        GameState.SELECTREMOVAL -> {
            Log.d(TAG, "GameState.SelectPiecesToRemove, player $player")
            val onSelect: (Position) -> Unit = {
                Log.d(TAG, "GameState.SelectPiecesToRemove: player $player, position=$it")
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
