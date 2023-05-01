package fr.richoux.pobo.gamescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.richoux.pobo.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val currentBoard: Board = _game.board
    val currentPlayer: PieceColor = _game.currentPlayer

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
        _gameState.tryEmit(newState)
    }

    fun goToNextState() {
        val newState = _game.nextGameState()
        _game.gameState = newState
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
        val piece = when(pieceTypeToPlay) {
            PieceType.Po -> Piece.createPo(currentPlayer)
            PieceType.Bo -> Piece.createBo(currentPlayer)
        }
        _game.play(Move(piece, it))
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

}
