package fr.richoux.pobo.gamescreen

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.richoux.pobo.R
import fr.richoux.pobo.engine.*

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
        gameState == GameState.SELECTGRADUATION
                && ( ( (viewModel.state == GameViewModelState.SELECT3 || viewModel.state == GameViewModelState.SELECT1OR3) && viewModel.piecesToPromote.size == 3)
                       || ( (viewModel.state == GameViewModelState.SELECT1 || viewModel.state == GameViewModelState.SELECT1OR3) && viewModel.piecesToPromote.size == 1) )
    IconButton(
        onClick = { viewModel.validateGraduationSelection() },
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
    viewModel: GameViewModel,
    lastMove: Position? = null,
    onTap: (Position) -> Unit = { _ -> },
    displayGameState: String = ""
) {
    val board = viewModel.currentBoard
    val player = viewModel.currentPlayer
    val promotionable = viewModel.getFlatPromotionable()
    val selected = viewModel.piecesToPromote.toList()
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            Column(Modifier.fillMaxHeight()) {
                BoardView(
                    board = board,
                    lastMove = lastMove,
                    onTap = onTap,
                    promotionable = promotionable,
                    selected = selected
                )
                Spacer(modifier = Modifier.height(8.dp))
                PiecesStocksView(
                    pool = board.getPlayerPool(PieceColor.Blue),
                    Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                PiecesStocksView(
                    pool = board.getPlayerPool(PieceColor.Red),
                    Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Player's turn: ",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                    )
                    val style = TextStyle(
                        color = if (player == PieceColor.Blue) Color.Blue else Color.Red,
                        fontSize = MaterialTheme.typography.body1.fontSize,
                        fontWeight = FontWeight.Bold,
                        fontStyle = MaterialTheme.typography.body1.fontStyle
                    )
                    Text(
                        text = player.toString(),
                        style = style,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = displayGameState,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                val gameState by viewModel.gameState.collectAsState()
                //val hasChoiceOfPiece = gameState == GameState.SELECTPIECE && viewModel.twoTypesInPool()
                val hasChoiceOfPiece = viewModel.twoTypesInPool()
                if(hasChoiceOfPiece) {
                    var check_po by remember {mutableStateOf(false)}
                    var check_bo by remember {mutableStateOf(false)}
                    val po = when(player) {
                        PieceColor.Blue -> R.drawable.blue_po
                        PieceColor.Red -> R.drawable.red_po
                    }
                    val bo = when(player) {
                        PieceColor.Blue -> R.drawable.blue_bo
                        PieceColor.Red -> R.drawable.red_bo
                    }
                    val po_check = when(player) {
                        PieceColor.Blue -> R.drawable.blue_po_check
                        PieceColor.Red -> R.drawable.red_po_check
                    }
                    val bo_check = when(player) {
                        PieceColor.Blue -> R.drawable.blue_bo_check
                        PieceColor.Red -> R.drawable.red_bo_check
                    }
                    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        if(check_po) {
                            IconButton(
                                onClick = {
                                    check_po = false
                                    viewModel.cancelPieceSelection()
                                }
                                //enabled = hasChoiceOfPiece
                            ) {
                                Icon(
                                    painterResource(po_check),
                                    contentDescription = "Select Po",
                                    tint = Color.Unspecified
                                )
                            }
                        }
                        else {
                            IconButton(
                                onClick = {
                                    check_po = true
                                    check_bo = false
                                    viewModel.selectPo()
                                }
                                //enabled = hasChoiceOfPiece
                            ) {
                                Icon(
                                    painterResource(po),
                                    contentDescription = "Select Po",
                                    tint = Color.Unspecified
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                        if(check_bo) {
                            IconButton(
                                onClick = {
                                    check_bo = false
                                    viewModel.cancelPieceSelection()
                                }
                                //enabled = hasChoiceOfPiece
                            ) {
                                Icon(
                                    painterResource(bo_check),
                                    contentDescription = "Select Bo",
                                    tint = Color.Unspecified
                                )
                            }
                        }
                        else {
                            IconButton(
                                onClick = {
                                    check_po = false
                                    check_bo = true
                                    viewModel.selectBo()
                                }
                                //enabled = hasChoiceOfPiece
                            ) {
                                Icon(
                                    painterResource(bo),
                                    contentDescription = "Select Bo",
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    }
                }

            }
        }
        Configuration.ORIENTATION_LANDSCAPE -> {
            Row(Modifier.fillMaxHeight()) {
                Column(Modifier.fillMaxHeight().width(IntrinsicSize.Min)) {
                    PiecesStocksView(
                        pool = board.getPlayerPool(PieceColor.Blue),
                        Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PiecesStocksView(
                        pool = board.getPlayerPool(PieceColor.Red),
                        Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Player's turn: ",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                        )
                        val style = TextStyle(
                            color = if (player == PieceColor.Blue) Color.Blue else Color.Red,
                            fontSize = MaterialTheme.typography.body1.fontSize,
                            fontWeight = FontWeight.Bold,
                            fontStyle = MaterialTheme.typography.body1.fontStyle
                        )
                        Text(
                            text = player.toString(),
                            style = style,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = displayGameState,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
                BoardView(
                    board = board,
                    lastMove = lastMove,
                    onTap = onTap,
                    promotionable = promotionable,
                    selected = selected
                )
                Column(Modifier.fillMaxHeight().width(IntrinsicSize.Min)) {
                    PiecesStocksView(
                        pool = board.getPlayerPool(PieceColor.Blue),
                        Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PiecesStocksView(
                        pool = board.getPlayerPool(PieceColor.Red),
                        Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Player's turn: ",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                        )
                        val style = TextStyle(
                            color = if (player == PieceColor.Blue) Color.Blue else Color.Red,
                            fontSize = MaterialTheme.typography.body1.fontSize,
                            fontWeight = FontWeight.Bold,
                            fontStyle = MaterialTheme.typography.body1.fontStyle
                        )
                        Text(
                            text = player.toString(),
                            style = style,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = displayGameState,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun GameView(viewModel: GameViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    val player = viewModel.currentPlayer
    var lastMove: Position? by remember { mutableStateOf(null) }

    when (gameState) {
        GameState.INIT -> {
            MainView(
                viewModel,
                displayGameState = viewModel.displayGameState
            )
            viewModel.goToNextState()
        }
        GameState.PLAY -> {
            if(viewModel.historyCall)
                lastMove = null

            MainView(
                viewModel,
                lastMove = lastMove,
                displayGameState = viewModel.displayGameState
            )
            if(!viewModel.historyCall)
                viewModel.nextTurn()
            viewModel.goToNextState()
        }
        GameState.SELECTPIECE -> {
            MainView(
                viewModel,
                lastMove = lastMove,
                displayGameState = viewModel.displayGameState)
        }
        GameState.SELECTPOSITION -> {
            val onSelect: (Position) -> Unit = {
                if (viewModel.canPlayAt(it)) {
                    lastMove = it
                    viewModel.playAt(it)
                }
            }
            MainView(
                viewModel,
                lastMove = lastMove,
                onTap = onSelect,
                displayGameState = viewModel.displayGameState
            )
        }
        GameState.CHECKGRADUATION -> {
            MainView(
                viewModel,
                displayGameState = viewModel.displayGameState
            )
            viewModel.checkGraduation()
        }
        GameState.AUTOGRADUATION -> {
            MainView(
                viewModel,
                displayGameState = viewModel.displayGameState
            )
            viewModel.autograduation()
        }
        GameState.SELECTGRADUATION -> {
            lastMove = null
            val onSelect: (Position) -> Unit = {
                viewModel.selectForGraduationOrCancel(it)
            }
            MainView(
                viewModel,
                lastMove = lastMove,
                onTap = onSelect,
                displayGameState = viewModel.displayGameState
            )
        }
        GameState.REFRESHSELECTGRADUATION -> {
            lastMove = null
            MainView(
                viewModel,
                lastMove = lastMove,
                displayGameState = viewModel.displayGameState
            )
            viewModel.goToNextState()
        }
        GameState.END -> {
//            Dialog(
//                onDismissRequest = {},
//            ) {
//                (LocalView.current.parent as DialogWindowProvider)?.window?.setDimAmount(0f)
//                Text(
//                    text = "$player wins!"
//                )
//            }
            val style = TextStyle(
                color = if(player == PieceColor.Blue) Color.Blue else Color.Red,
                fontSize = MaterialTheme.typography.body1.fontSize,
                fontWeight = FontWeight.Bold,
                fontStyle = MaterialTheme.typography.body1.fontStyle
            )
            val acceptNewGame: () -> Unit = {
                viewModel.newGame(viewModel.aiEnabled)
            }
            val declineNewGame: () -> Unit = {
                viewModel.resume()
            }
            AlertDialog(
                onDismissRequest = {},
                buttons = {
                    Row() {
                        Button(
                            { acceptNewGame() }
                        ) {
                            Text(text = "Sure!")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            { declineNewGame() }
                        ) {
                            Text(text = "Next time")
                        }
                    }
                },
                title = {
                    Row()
                    {
                        Text(
                            text = "$player",
                            style = style
                        )
                        Text(
                            text = " wins!"
                        )
                    }
                },
                text = {
                    Text(
                        text = "New game?"
                    )
                }
            )
        }
    }
}
