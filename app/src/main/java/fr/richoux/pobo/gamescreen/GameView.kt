package fr.richoux.pobo.gamescreen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.richoux.pobo.engine.*

private const val TAG = "pobotag GameView"

@Composable
fun GameActions(viewModel: GameViewModel = viewModel()) {
    val mustSelectPiece by viewModel.mustSelectPiece.collectAsState(initial = false)
    IconButton(onClick = { viewModel.selectPo() }, enabled = mustSelectPiece) {
        Icon(Icons.Filled.Phone, contentDescription = "Select Po")
    }
    IconButton(onClick = { viewModel.selectBo() }, enabled = mustSelectPiece) {
        Icon(Icons.Filled.Email, contentDescription = "Select Bo")
    }
    Spacer(modifier = Modifier.width(48.dp))
    val canGoBack by viewModel.canGoBack.collectAsState(initial = false)
    IconButton(onClick = { viewModel.goBackMove() }, enabled = canGoBack) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Undo Move")
    }

    val canGoForward by viewModel.canGoForward.collectAsState(initial = false)
    IconButton(onClick = { viewModel.goForwardMove() }, enabled = canGoForward) {
        Icon(Icons.Filled.ArrowForward, contentDescription = "Redo Move")
    }
}

@Composable
fun MainView(game: Game, selection: Position? = null, didTap: (Position) -> Unit = {_ -> Unit}) {
    Log.d(TAG, "MainView call")
    Column(Modifier.fillMaxHeight()) {
        BoardView(
            game = game,
            selection = selection,
            didTap = didTap
        )
        PiecesStocksView(
            pool = getPlayerPool(PieceColor.Blue),
            reserve = getPlayerReserve(PieceColor.Blue),
            Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        PiecesStocksView(
            pool = getPlayerPool(PieceColor.Red),
            reserve = getPlayerReserve(PieceColor.Red),
            Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = game.displayGameState,
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
    var pieceSelection: PieceType? by remember { mutableStateOf(null) }
    var positionSelection: Position? by remember { mutableStateOf(null) }

    val gameLoopStep by viewModel.gameLoopStep.collectAsState(initial = GameLoopStep.Init(Game()))

    when (val loopStep = gameLoopStep) {
        is GameLoopStep.Init -> {
            Log.d(TAG, "In GameLoopStep.Init")
            val game = loopStep.game
            viewModel.updateLoop(game.nextGameLoopStep(loopStep, fakeMove), game)

            MainView(game)
        }

        is GameLoopStep.Play -> {
            Log.d(TAG, "In GameLoopStep.Play")
            val game = loopStep.game

            if (hasTwoTypesInPool(game.turn))
                viewModel.updateLoop(GameLoopStep.SelectPiece(game), game)
            else
                viewModel.updateLoop(GameLoopStep.SelectPosition(game), game)

            MainView(game)
        }

        is GameLoopStep.SelectPiece -> {
            Log.d(TAG, "In GameLoopStep.SelectPiece")
            val game = loopStep.game
            val onSelectPiece = loopStep.onSelectPiece

//            val onSelect: (PieceType) -> Unit = {
//                val sel = pieceSelection
//                if (game.board.hasPieceInPool(game.turn, it))
//                    pieceSelection = it
//                else if(sel != null && game.board.hasPieceInPool(game.turn, sel)) {
//                    viewModel.updateLoop(onSelectPiece(sel))
//                    pieceSelection = null
//                    viewModel.clearForwardHistory()
//                }
//            }
            val onButtonClicked: (PieceType) -> Unit = {
                viewModel.updateLoop(onSelectPiece(it), game)
            }
            AlertDialog(
                onDismissRequest = {},
                buttons = {
                    Button({ onButtonClicked(PieceType.Po) }) { Text(text = "Po") }
                    Button({ onButtonClicked(PieceType.Bo) }) { Text(text = "Bo") }
                },
                title = {
                    Text(text = "Piece selection")
                },
                text = {
                    Text(text = "Choose if you want to play a Po or a Bo")
                }
            )

            MainView(game)
        }

        is GameLoopStep.SelectPosition -> {
            Log.d(TAG, "In GameLoopStep.SelectPosition")
            val game = loopStep.game
            val onSelectPosition = loopStep.onSelectPosition

            val onSelect: (Position) -> Unit = {
                val sel = positionSelection
                Log.d(TAG, "GameLoopStep.SelectPosition: positionSelection=(${positionSelection?.x},${positionSelection?.y})")
                if (game.canPlayAt(it))
                    positionSelection = it
                else if(sel != null && game.canPlayAt(sel)) {
                    viewModel.updateLoop(onSelectPosition(sel), game)
                    Log.d(TAG, "GameLoopStep.SelectPosition: play sel=(${sel.x},${sel.y}) and got ${onSelectPosition(sel)}")
                    pieceSelection = null
                    viewModel.clearForwardHistory()
                }
            }
            val onButtonClicked: (Position) -> Unit = {
                Log.d(TAG, "GameLoopStep.SelectPosition: button clicked on (${it.x},${it.y})")
                viewModel.updateLoop(onSelectPosition(it), game)
            }

            MainView(game, positionSelection, onSelect)
        }

        is GameLoopStep.CheckGraduationCriteria -> {
            Log.d(TAG, "In GameLoopStep.CheckGraduationCriteria")
            val game = loopStep.game
            val graduation = loopStep.positions

            if (graduation.isEmpty())
                viewModel.updateLoop(GameLoopStep.Play(game), game)
            else if(graduation.size == 1) {
                for (position in graduation[0]) {
                    //viewModel.updateLoop(game.nextGameLoopStep())
                }
            }
//            else
//                viewModel.updateLoop(GameLoopStep.SelectPiecesToRemove(game, ...)

            MainView(game)
        }

        is GameLoopStep.SelectPiecesToRemove -> {
            Log.d(TAG, "In GameLoopStep.SelectPiecesToRemove")
            val game = loopStep.game
            val onSelectPiece = loopStep.onSelectPiece

//            if(graduableList.size == 1)
//                viewModel.updateLoop(GameLoopStep.Play(Game()))
//            else {
//                val onButtonClicked: (PieceType) -> Unit = {
//                    viewModel.updateResult(onPieceSelection(it))
//                }
//                AlertDialog(
//                    onDismissRequest = {},
//                    buttons = {
//                        Button({ onButtonClicked(PieceType.Queen) }) { Text(text = "Queen") }
//                        //Button({ onButtonClicked(PieceType.Rook) }) { Text(text = "Rook") }
//                        Button({ onButtonClicked(PieceType.Knight) }) { Text(text = "Knight") }
//                        Button({ onButtonClicked(PieceType.Bishop) }) { Text(text = "Bishop") }
//                    },
//                    title = {
//                        Text(text = "Promote to")
//                    },
//                    text = {
//                        Text(text = "Please choose a piece type to promote the pawn to")
//                    }
//                )
            MainView(game)
        }
    }
}
