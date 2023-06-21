package fr.richoux.pobo.gamescreen

import androidx.lifecycle.ViewModel
import fr.richoux.pobo.engine.*
import fr.richoux.pobo.engine.ai.AI
import fr.richoux.pobo.engine.ai.MCTS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "pobotag GameViewModel"

enum class GameViewModelState {
    IDLE, SELECT1, SELECT3, SELECT1OR3
}

class GameViewModel : ViewModel() {
    var state = GameViewModelState.IDLE
        private set

    private val _history: MutableList<History> = mutableListOf()
    private val _forwardHistory: MutableList<History> = mutableListOf()
    private val _moveHistory: MutableList<Move> = mutableListOf()
    private val _forwardMoveHistory: MutableList<Move> = mutableListOf()
    var historyCall = false
    var moveNumber: Int = 0
        private set

    private val _ai = AI(Color.Red)
    var aiEnabled = true
        private set
    private var _mcts = MCTS()

    private var _promotionListIndex: MutableList<Int> = mutableListOf()
    private var _promotionListMask: MutableList<Boolean> = mutableListOf()
    private var _piecesToPromoteIndex: HashMap<Position, MutableList<Int>> = hashMapOf()
    var piecesToPromote: MutableList<Position> = mutableListOf()
        private set

    var pieceTypeToPlay: PieceType? = null
        private set

    private var _game: Game = Game()
    var currentBoard: Board = _game.board
        private set
    var currentPlayer: Color = _game.currentPlayer
        private set

    private var _gameState = MutableStateFlow<GameState>(_game.gameState)
    var gameState = _gameState.asStateFlow()

    var canGoBack = MutableStateFlow<Boolean>(if (aiEnabled) _history.size > 1 else _history.isNotEmpty())
    var canGoForward = MutableStateFlow<Boolean>(_forwardHistory.isNotEmpty())

    var displayGameState: String  = _game.displayGameState
    var hasStarted: Boolean = false

    var selectedValue = MutableStateFlow<String>("")

    fun reset() {
        _promotionListIndex = mutableListOf()
        _promotionListMask = mutableListOf()
        _piecesToPromoteIndex = hashMapOf()
        piecesToPromote = mutableListOf()
        displayGameState  = _game.displayGameState
        canGoBack.tryEmit(if (aiEnabled) _history.size > 1 else _history.isNotEmpty())
        canGoForward.tryEmit(_forwardHistory.isNotEmpty())
        pieceTypeToPlay = null
    }

    fun newGame(aiEnabled: Boolean) {
        this.aiEnabled = aiEnabled
        if( aiEnabled )
            _mcts = MCTS()

        _history.clear()
        _forwardHistory.clear()
        _moveHistory.clear()
        _forwardMoveHistory.clear()
        moveNumber = 0
        _game = Game()
        currentBoard = _game.board
        currentPlayer = _game.currentPlayer
        reset()
        hasStarted = false
        _gameState.tryEmit(GameState.INIT)
        gameState = _gameState.asStateFlow()
    }

    fun goBackMove() {
        _forwardHistory.add(History(currentBoard, currentPlayer, moveNumber))
        var last = _history.removeLast()

        var lastMove = _moveHistory.removeLast()
        _forwardMoveHistory.add(lastMove)

        if (aiEnabled) {
            _forwardHistory.add(last)
            last = _history.removeLast()

            lastMove = _moveHistory.removeLast()
            _forwardMoveHistory.add(lastMove)
        }

        _game.changeWithHistory(last)
        currentBoard = last.board
        currentPlayer = last.player
        moveNumber = last.moveNumber
        reset()
        historyCall = true
        _gameState.tryEmit(GameState.PLAY)
    }

    fun goForwardMove() {
        _history.add(History(currentBoard, currentPlayer, moveNumber))
        var last = _forwardHistory.removeLast()

        var lastMove = _forwardMoveHistory.removeLast()
        _moveHistory.add(lastMove)

        if (aiEnabled) {
            _history.add(last)
            last = _forwardHistory.removeLast()

            lastMove = _forwardMoveHistory.removeLast()
            _moveHistory.add(lastMove)
        }

        _game.changeWithHistory(last)
        currentBoard = last.board
        currentPlayer = last.player
        moveNumber = last.moveNumber
        reset()
        historyCall = true
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
        canGoBack.tryEmit(if (aiEnabled) _history.size > 1 else _history.isNotEmpty())
        canGoForward.tryEmit(_forwardHistory.isNotEmpty())
        displayGameState  = _game.displayGameState
        _gameState.tryEmit(newState)
    }

    fun selectPo() {
        selectedValue.tryEmit("Po")
        pieceTypeToPlay = PieceType.Po
        goToNextState()
    }

    fun selectBo() {
        selectedValue.tryEmit("Bo")
        pieceTypeToPlay = PieceType.Bo
        goToNextState()
    }

    fun canPlayAt(it: Position): Boolean = _game.canPlayAt(it)

    fun playAt(it: Position) {
        _forwardHistory.clear()
        _forwardMoveHistory.clear()
        historyCall = false
        _history.add(History(currentBoard, currentPlayer, moveNumber))

        val piece = when(pieceTypeToPlay) {
            PieceType.Po -> Piece.createPo(currentPlayer)
            PieceType.Bo -> Piece.createBo(currentPlayer)
            null -> Piece.createFromByte(currentPlayer, currentBoard.getPlayerPool(currentPlayer).first() )
        }
        pieceTypeToPlay = null
        val move = Move(piece, it)

        if(!_game.canPlay(move)) return
        _moveHistory.add(move)
        moveNumber++
        var newBoard = currentBoard.playAt(move)
        newBoard = _game.doPush(newBoard, move)
        selectedValue.tryEmit("")

        _game.checkVictoryFor(newBoard, currentPlayer)
        _game.board = newBoard
        currentBoard = _game.board
        goToNextState()
    }

    fun getFlatPromotionable(): List<Position> = _game.getGraduations(currentBoard).flatten()

    fun checkGraduation() {
        val groups = _game.getGraduations(currentBoard)
        var groupOfAtLeast3 = false
        if(groups.isEmpty() || historyCall) {
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
        if (graduable.size == 1 && state != GameViewModelState.IDLE) {
            graduable[0].forEach {
                currentBoard = currentBoard.removePieceAndPromoteIt(it)
            }
        }
        _game.board = currentBoard
        goToNextState()
    }

    fun selectForGraduationOrCancel(position: Position) {
        val removable = _game.getGraduations(currentBoard)

        if(state != GameViewModelState.IDLE) {
            // if the player taps a selected piece, unselect it
            if (piecesToPromote.contains(position)) {
                piecesToPromote.remove(position)
                _piecesToPromoteIndex[position] = mutableListOf()
                _promotionListIndex.clear()
                if (piecesToPromote.isEmpty()) {
                    _promotionListMask.clear()
                } else {
                    _promotionListMask = MutableList(removable.size) { false }
                    for (piece in piecesToPromote) {
                        _piecesToPromoteIndex[piece] = mutableListOf()
                        for ((index, list) in removable.withIndex())
                            if (list.contains(piece)) {
                                _promotionListMask[index] = true
                                _promotionListIndex.add(index)
                                _piecesToPromoteIndex[piece]?.add(index)
                            }
                        for (indexValue in _promotionListIndex)
                            for (otherPiece in piecesToPromote)
                                if (_piecesToPromoteIndex[otherPiece]?.contains(indexValue) != true) {
                                    _promotionListIndex.remove(indexValue)
                                    _promotionListMask[indexValue] = false
                                }
                    }
                }
                val toRemove: MutableList<Position> = mutableListOf()
                for ((key, list) in _piecesToPromoteIndex)
                    if (list.isEmpty())
                        toRemove.add(key)
                for (removePiece in toRemove)
                    _piecesToPromoteIndex.remove(removePiece)
            } else {
                // _promotionListIndexes is a tedious way to check if a selected piece (in particular the 1st one)
                // belongs to different graduable groups, to make sure we don't select pieces from different groups
                // TODO: to simplify
                if (_promotionListIndex.isEmpty()) {
                    _promotionListMask = MutableList(removable.size) { false }
                    for ((index, list) in removable.withIndex()) {
                        if (list.contains(position)) {
                            _promotionListMask[index] = true
                            _promotionListIndex.add(index)
                            if (!_piecesToPromoteIndex.containsKey(position))
                                _piecesToPromoteIndex[position] = mutableListOf()
                            _piecesToPromoteIndex[position]?.add(index)
                        }
                    }
                    if (_promotionListIndex.isEmpty())
                        return
                    else {
                        piecesToPromote.add(position)
                    }
                } else {
                    for ((index, list) in removable.withIndex()) {
                        if (list.contains(position)) {
                            if (_promotionListMask[index]) {
                                if (!piecesToPromote.contains(position))
                                    piecesToPromote.add(position)
                                if (!_piecesToPromoteIndex.containsKey(position))
                                    _piecesToPromoteIndex[position] = mutableListOf()
                                _piecesToPromoteIndex[position]?.add(index)
                            }
                        }
                    }
                    // shrink the index list
                    val toRemove: MutableList<Int> = mutableListOf()
                    for (indexValue in _promotionListIndex)
                        for (piece in piecesToPromote)
                            if (_piecesToPromoteIndex[piece]?.contains(indexValue) != true) {
                                toRemove.add(indexValue)
                                _promotionListMask[indexValue] = false
                                for ((key, list) in _piecesToPromoteIndex) {
                                    if (list.isNotEmpty())
                                        list.remove(indexValue)
                                }
                            }
                    for (indexValue in toRemove)
                        _promotionListIndex.remove(indexValue)
                }
            }
        }

        // no more than 1 or 3 selections, regarding the situation
        if (piecesToPromote.size == 3 || (state == GameViewModelState.SELECT1 && piecesToPromote.size == 1)) {
            validateGraduationSelection()
        } else {
            _game.unfinishSelection()
            goToNextState()
        }
    }

    fun validateGraduationSelection() {
        _game.finishSelection()
        _game.promoteOrRemovePieces(piecesToPromote)
        currentBoard = _game.board
        _game.checkVictory()
        _promotionListIndex.clear()
        piecesToPromote.clear()
        state = GameViewModelState.IDLE
        goToNextState()
    }

    fun nextTurn() {
        _game.changePlayer()
        currentPlayer = _game.currentPlayer

        // if we play against an AI and it is its turn
        if(aiEnabled && currentPlayer == Color.Red) {
            // val move = randomPlay(_game)
            val move = _mcts.run( _game, _moveHistory.last(), 1500 )
            pieceTypeToPlay = move.piece.getType()
            playAt( move.to )
        }
    }

    fun twoTypesInPool(): Boolean = currentBoard.hasTwoTypesInPool(currentPlayer)

    fun resume() = _gameState.tryEmit(_game.gameState)
}

