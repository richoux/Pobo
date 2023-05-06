package fr.richoux.pobo.gamescreen

import androidx.lifecycle.ViewModel
import fr.richoux.pobo.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "pobotag GameViewModel"

enum class GameViewModelState {
    IDLE, SELECT1, SELECT3, SELECT1OR3
}

class GameViewModel : ViewModel() {
    var state = GameViewModelState.IDLE
        private set

    private val _history: MutableList<History> = mutableListOf<History>()
    private val _forwardHistory: MutableList<History> = mutableListOf<History>()
    var historyCall = false

    private val _ai = AI(PieceColor.Red)
    var aiEnabled = true
        private set

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
    var gameState = _gameState.asStateFlow()

    var canGoBack = if (aiEnabled) _history.size > 1 else _history.isNotEmpty()
    var canGoForward = _forwardHistory.isNotEmpty()

    var displayGameState: String  = _game.displayGameState
    var hasStarted: Boolean = false

    fun newGame(aiEnabled: Boolean) {
        this.aiEnabled = aiEnabled

        _forwardHistory.clear()
        _history.clear()
        _game = Game()
        currentBoard = _game.board
        currentPlayer = _game.currentPlayer
        hasStarted = false
        _gameState.tryEmit(GameState.INIT)
        displayGameState  = _game.displayGameState
        gameState = _gameState.asStateFlow()
    }

    fun goBackMove() {
        _forwardHistory.add(History(currentBoard,currentPlayer))
        var last = _history.removeLast()
        if (aiEnabled) {
            last = _history.removeLast()
            _forwardHistory.add(last)
        }
        _game.changeWithHistory(last)
        currentBoard = last.board
        currentPlayer = last.player
        historyCall = true

        canGoBack = if (aiEnabled) _history.size > 1 else _history.isNotEmpty()
        canGoForward = _forwardHistory.isNotEmpty()
        displayGameState  = _game.displayGameState

        _gameState.tryEmit(GameState.PLAY)
    }

    fun goForwardMove() {
        _history.add(History(currentBoard,currentPlayer))
        var last = _forwardHistory.removeLast()
        if (aiEnabled) {
            last = _forwardHistory.removeLast()
            _history.add(last)
        }
        _game.changeWithHistory(last)
        currentBoard = last.board
        currentPlayer = last.player
        historyCall = true

        canGoBack = if (aiEnabled) _history.size > 1 else _history.isNotEmpty()
        canGoForward = _forwardHistory.isNotEmpty()
        displayGameState  = _game.displayGameState
        _gameState.tryEmit(GameState.PLAY)
    }

    fun cancelPieceSelection() {
        val newState = GameState.SELECTPIECE
        _game.gameState = newState
        displayGameState  = _game.displayGameState
        _gameState.tryEmit(newState)
    }

    fun goToNextState() {
        if(_game.victory) {
            _gameState.tryEmit(GameState.END)
            return
        }

        val newState = _game.nextGameState()
        hasStarted = true
        _game.gameState = newState
        canGoBack = if (aiEnabled) _history.size > 1 else _history.isNotEmpty()
        canGoForward = _forwardHistory.isNotEmpty()
        displayGameState  = _game.displayGameState
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

        _game.checkVictory(newBoard)
        _game.board = newBoard
        currentBoard = _game.board
        goToNextState()
    }

    fun getFlatPromotionable(): List<Position> = _game.getGraduations(currentBoard).flatten()

    fun checkGraduation() {
        val groups = _game.getGraduations(currentBoard)
        var groupOfAtLeast3 = false
        if(groups.isEmpty()) {
            state = GameViewModelState.IDLE
        }
        else {
            if(groups.size >= 8) {
                for( group in groups ) {
                    if(group.size >= 3)
                        groupOfAtLeast3 = true
                }
                if(groupOfAtLeast3)
                    state = GameViewModelState.SELECT1OR3
                else
                    state = GameViewModelState.SELECT1
            }
            else
                state = GameViewModelState.SELECT3
        }
        goToNextState()
    }

    fun autograduation() {
        val graduable = _game.getGraduations(currentBoard)
        if (graduable.size == 1) {
            graduable[0].forEach {
                currentBoard = currentBoard.removePieceAndPromoteIt(it)
            }
        }
        _game.board = currentBoard
        goToNextState()
    }

    fun selectForGraduationOrCancel(it: Position) {


        // if the player taps a selected piece, unselect it
        if(piecesToPromote.contains(it)) {
            piecesToPromote.remove(it)
            return
        }

        val removable = _game.getGraduations(currentBoard)

        // _promotionListIndexes is a tedious way to check if a selected piece (in particular the 1st one)
        // belongs to different graduable groups, to make sure we don't select pieces from different groups
        // TODO: to simplify
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

        // no more than 1 or 3 selections, regarding the situation
        if(piecesToPromote.size == 3 || (state == GameViewModelState.SELECT1 && piecesToPromote.size == 1)) {
            validateGraduationSelection()
            return
        }
    }

    fun validateGraduationSelection() {
        _game.promoteOrRemovePieces(piecesToPromote)
        currentBoard = _game.board
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

    fun resume() = _gameState.tryEmit(_game.gameState)
}

