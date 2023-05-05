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
    IDLE, SELECT1, SELECT3
}

class GameViewModel : ViewModel() {
    val state = GameViewModelState.IDLE

    private val _history: MutableList<History> = mutableListOf<History>()
    private val _forwardHistory: MutableList<History> = mutableListOf<History>()
    var historyCall = false

    private val _ai = AI(PieceColor.Red)
    private var _aiEnabled = true

    private var _promotionListIndexes: MutableList<Int> = mutableListOf()
    var piecesToPromote: MutableList<Position> = mutableListOf()
        private set
    var pieceTypeToPlay: PieceType? = null
        private set

    private var _game: Game = Game()
    var currentBoard: Board = _game.board
        private set
    var currentPlayer: PieceColor = _game.currentPlayer
        private set

    private var _gameState = MutableStateFlow<GameState>(_game.gameState)
    val gameState = _gameState.asStateFlow()

    var canGoBack = if (_aiEnabled) _history.size > 1 else _history.isNotEmpty()
    var canGoForward = _forwardHistory.isNotEmpty()

    val displayGameState: String  = _game.displayGameState

    fun newGame(aiEnabled: Boolean) {
        this._aiEnabled = aiEnabled

        _forwardHistory.clear()
        _history.clear()
        _game = Game()
        _gameState.tryEmit(GameState.INIT)
    }

    fun goBackMove() {
        _forwardHistory.add(History(currentBoard,currentPlayer))
        var last = _history.removeLast()
        if (_aiEnabled) {
            last = _history.removeLast()
            _forwardHistory.add(last)
        }
        _game.changeWithHistory(last)
        currentBoard = last.board
        currentPlayer = last.player
        historyCall = true

        canGoBack = if (_aiEnabled) _history.size > 1 else _history.isNotEmpty()
        canGoForward = _forwardHistory.isNotEmpty()
        _gameState.tryEmit(GameState.PLAY)
    }

    fun goForwardMove() {
        _history.add(History(currentBoard,currentPlayer))
        var last = _forwardHistory.removeLast()
        if (_aiEnabled) {
            last = _forwardHistory.removeLast()
            _history.add(last)
        }
        _game.changeWithHistory(last)
        currentBoard = last.board
        currentPlayer = last.player
        historyCall = true

        canGoBack = if (_aiEnabled) _history.size > 1 else _history.isNotEmpty()
        canGoForward = _forwardHistory.isNotEmpty()
        _gameState.tryEmit(GameState.PLAY)
    }

    fun cancelPieceSelection() {
        val newState = GameState.SELECTPIECE
        _game.gameState = newState
        _gameState.tryEmit(newState)
    }

    fun goToNextState() {
        val newState = _game.nextGameState()
        _game.gameState = newState
        canGoBack = if (_aiEnabled) _history.size > 1 else _history.isNotEmpty()
        canGoForward = _forwardHistory.isNotEmpty()
        _gameState.tryEmit(newState)
    }

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
        historyCall = false

        val piece = when(pieceTypeToPlay) {
            PieceType.Po -> Piece.createPo(currentPlayer)
            PieceType.Bo -> Piece.createBo(currentPlayer)
            null -> Piece.createFromType(currentPlayer, currentBoard.getPlayerPool(currentPlayer).first().type)
        }
        pieceTypeToPlay = null
        val move = Move(piece, it)

        if(!_game.canPlay(move)) return

        _history.add(History(currentBoard, currentPlayer))
        var newBoard = currentBoard.playAt(move)
        newBoard = _game.doPush(newBoard, move)

        val victory = _game.checkVictory(newBoard)
        if(!victory) {
            val graduable = _game.getGraduations(newBoard)
            if( !graduable.isEmpty() ) {
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

    fun twoTypesInPool(): Boolean = currentBoard.hasTwoTypesInPool(currentPlayer)
}
