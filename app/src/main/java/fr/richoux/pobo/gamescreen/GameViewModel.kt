package fr.richoux.pobo.gamescreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.richoux.pobo.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "pobotag GameViewModel"

enum class GameViewModelState {
    IDLE, SELECT1, SELECT3, CANCEL, OK
}

class GameViewModel : ViewModel() {
    val state = GameViewModelState.IDLE

    private val _history: MutableList<History> = mutableListOf<History>()
    private val _forwardHistory: MutableList<History> = mutableListOf<History>()

    private val _ai = AI(PieceColor.Red)
    private var _aiEnabled = true

    private var _promotionListIndexes: MutableList<Int> = mutableListOf()
    var piecesToPromote: MutableList<Position> = mutableListOf()
        private set
    var pieceTypeToPlay: PieceType = PieceType.Po
        private set

    private var _game: Game = Game()
    var currentBoard: Board = _game.board
        private set
    var currentPlayer: PieceColor = _game.currentPlayer
        private set

    private var _gameState = MutableStateFlow<GameState>(_game.gameState)
    val gameState = _gameState.asStateFlow()

    val canGoBack = if (_aiEnabled) _history.size > 1 else _history.isNotEmpty()
    val canGoForward = _forwardHistory.isNotEmpty()

    val displayGameState: String  = _game.displayGameState

    fun updateState(aiCanPlay: Boolean = true) {
        if (_aiEnabled
            && aiCanPlay
            && _game.currentPlayer == PieceColor.Red
            && _game.gameState == GameState.PLAY
        ) {
            viewModelScope.launch {
                val nextMove = _ai.calculateNextMove(_game, PieceColor.Red)
                if (nextMove != null) {
//                    val finalAiResult = when (aiResult) {
//                        is MoveResult.Played -> aiResult
//                        is MoveResult.Promotion -> aiResult.onPieceSelection(PieceType.Queen)
//                    }
                    //updateLoop(aiResult)
                }
            }
        }
    }

    fun newGame(aiEnabled: Boolean) {
        this._aiEnabled = aiEnabled

        _forwardHistory.clear()
        _history.clear()
        _game = Game()
        Log.d(TAG, "GameViewModel.newGame, tryEmit GameState.INIT")
        _gameState.tryEmit(GameState.INIT)
    }

    fun goBackMove() {
        var last = _history.removeLast()
        _forwardHistory.add(last)
        if (_aiEnabled) {
            last = _history.removeLast()
            _forwardHistory.add(last)
        }
        _game.changeWithHistory(last)
    }

    fun goForwardMove() {
        var last = _forwardHistory.removeLast()
        _history.add(last)
        if (_aiEnabled) {
            last = _forwardHistory.removeLast()
            _history.add(last)
        }
        _game.changeWithHistory(last)
    }

    fun cancelPieceSelection() {
        val newState = GameState.SELECTPIECE
        _game.gameState = newState
        Log.d(TAG, "GameViewModel.cancelPieceSelection, tryEmit ${newState}")
        _gameState.tryEmit(newState)
    }

    fun goToNextState() {
        Log.d(TAG, "GameViewModel.goToNextState call")
        val newState = _game.nextGameState()
        Log.d(TAG, "GameViewModel.goToNextState, old state = ${_game.gameState}, new state = ${newState}")
        _game.gameState = newState
        Log.d(TAG, "GameViewModel.goToNextState, tryEmit ${newState}")
        _gameState.tryEmit(newState)
    }

//    fun updates() {
//        currentBoard = _game.board
//        currentPlayer = _game.currentPlayer
//    }

    fun selectPo() {
        pieceTypeToPlay = PieceType.Po
        goToNextState()
    }

    fun selectBo() {
        pieceTypeToPlay = PieceType.Bo
        goToNextState()
    }

    fun canPlayAt(it: Position): Boolean = _game.canPlayAt(it)

    fun playAt(it: Position) {
        _forwardHistory.clear()
        val piece = when(pieceTypeToPlay) {
            PieceType.Po -> Piece.createPo(currentPlayer)
            PieceType.Bo -> Piece.createBo(currentPlayer)
        }

        val move = Move(piece, it)

        if(!_game.canPlay(move)) {
            Log.d(TAG, "GameViewModel.playAt: player ${currentPlayer}, can't play ${move}")
            return
        }

        var newBoard = currentBoard.playAt(move)
        newBoard = _game.doPush(newBoard, move)

        val victory = _game.checkVictory(newBoard)
        if(!victory) {
            Log.d(TAG, "GameViewModel.playAt, player ${currentPlayer}, not a victory yet")
            val graduable = _game.getGraduations(newBoard)
            if( !graduable.isEmpty() ) {
                Log.d(TAG, "GameViewModel.playAt, player ${currentPlayer}, there is promotion in the air!")
                Log.d(TAG, "GameViewModel.playAt, player ${currentPlayer}, graduable.size=${graduable.size}")
                if(graduable.size == 1 ) {
                    graduable[0].forEach {
                        newBoard = newBoard.removePieceAndPromoteIt(it)
                    }
                }
                else {
                    // ???
                    // need to ask the user to select one of these groups
                }
            }
        }

        _game.board = newBoard
        currentBoard = _game.board
        goToNextState()
    }

    fun selectForRemovalOrCancel(it: Position) {
        val removable = _game.getGraduations(currentBoard)

        // if the player taps a selected piece, unselect it
        if(piecesToPromote.contains(it)) {
            // sure it compares the position content, not its address?
            piecesToPromote.remove(it)
            return
        }

        if(_promotionListIndexes.isEmpty()) {
            for( (index, list) in removable.withIndex() ) {
                if(list.contains(it)) {
                    _promotionListIndexes.add(index)
                }
            }
            if(_promotionListIndexes.isEmpty())
                return
            else {
                piecesToPromote.add(it)
            }
        }
        else {
            for( (index, list) in removable.withIndex() ) {
                if(list.contains(it)) {
                    if(_promotionListIndexes.contains(index)) {
                        piecesToPromote.add(it)
                        if (_promotionListIndexes.size > 1) {
                            _promotionListIndexes.clear()
                            _promotionListIndexes.add(index)
                        }
                    }
                }
            }
        }

        // need to emit GameState.SelectPiecesToRemove again?
    }

    fun validateRemoval() {
        _game.promoteOrRemovePieces(piecesToPromote)
        _game.checkVictory()
        _promotionListIndexes.clear()
        piecesToPromote.clear()
        goToNextState()
    }

    fun nextTurn() {
        _game.changePlayer()
        currentPlayer = _game.currentPlayer
    }

}
