package fr.richoux.pobo.gamescreen

import android.util.Log
import androidx.lifecycle.ViewModel
import fr.richoux.pobo.engine.*
import fr.richoux.pobo.engine.ai.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "pobotag GameViewModel"

enum class GameViewModelState {
  IDLE, SELECT1, SELECT3, SELECT1OR3
}

enum class GameState {
  INIT, HISTORY, SELECTPIECE, SELECTPOSITION, CHECKGRADUATION, AUTOGRADUATION, SELECTGRADUATION, REFRESHSELECTGRADUATION, END
}

class GameViewModel : ViewModel() {
  /*** AI ***/
  var p1IsAI = true
    private set
  var p2IsAI = true
    private set
  private var aiP1: AI = RandomPlay(Color.Blue) //SimpleHeuristics(Color.Blue)
  private var aiP2: AI = RandomPlay(Color.Red) //SimpleHeuristics(Color.Red)

  /*** Game History ***/
  private val _history: MutableList<History> = mutableListOf()
  private val _forwardHistory: MutableList<History> = mutableListOf()
  private val _moveHistory: MutableList<Move> = mutableListOf()
  private val _forwardMoveHistory: MutableList<Move> = mutableListOf()
  var historyCall = false
  var moveNumber: Int = 0
    private set
  var canGoBack = MutableStateFlow<Boolean>(if(p2IsAI) _history.size > 1 else _history.isNotEmpty())
  var canGoForward = MutableStateFlow<Boolean>(_forwardHistory.isNotEmpty())
  var lastMovePosition: Position? = null
    private set

  /*** Piece Promotions ***/
  private var _promotionListIndex: MutableList<Int> = mutableListOf()
  private var _promotionListMask: MutableList<Boolean> = mutableListOf()
  private var _piecesToPromoteIndex: HashMap<Position, MutableList<Int>> = hashMapOf()
  var piecesToPromote: MutableList<Position> = mutableListOf()
    private set
  var stateSelection = GameViewModelState.IDLE
    private set
  private var _finishPieceSelection: Boolean = false

  /*** Game States ***/
  private var _gameState = MutableStateFlow<GameState>(GameState.SELECTPOSITION)
  var gameState = _gameState.asStateFlow()
  val displayGameState: String
    get() {
      return when(_gameState.value) {
        GameState.INIT -> ""
        GameState.HISTORY -> ""
        GameState.SELECTPIECE -> "Select a small or a large piece:"
        GameState.SELECTPOSITION -> ""
        GameState.CHECKGRADUATION -> ""
        GameState.AUTOGRADUATION -> ""
        GameState.SELECTGRADUATION -> "Select small pieces to graduate or large pieces to remove"
        GameState.REFRESHSELECTGRADUATION -> ""
        GameState.END -> ""
      }
    }

  /*** Other Variables ***/
  private var _game: Game = Game()
  var pieceTypeToPlay: PieceType? = null
    private set
  var hasStarted: Boolean = false
  var selectedValue = MutableStateFlow<String>("")

  /*** Member functions ***/
  fun aiP1(): String = aiP1.toString()
  fun aiP2(): String = aiP2.toString()

  fun getBoard(): Board = _game.board
  fun getPlayer(): Color = _game.currentPlayer

  fun reset() {
    _promotionListIndex = mutableListOf()
    _promotionListMask = mutableListOf()
    _piecesToPromoteIndex = hashMapOf()
    piecesToPromote = mutableListOf()
    if(IsAIToPLay()) {
      canGoBack.tryEmit(false)
      canGoForward.tryEmit(false)
    } else {
      canGoBack.tryEmit(if(p2IsAI) _history.size > 1 else _history.isNotEmpty())
      canGoForward.tryEmit(_forwardHistory.isNotEmpty())
    }
    pieceTypeToPlay = null
  }

  fun newGame(p1IsAI: Boolean, p2IsAI: Boolean) {
    this.p1IsAI = p1IsAI
    this.p2IsAI = p2IsAI

    if(p1IsAI) {
//      aiP1 = MCTS_GHOST(Color.Blue, number_preselected_actions = 0, first_n_strategy = 0) // Vanilla-MCTS
//      aiP1 = MCTS_GHOST(Color.Blue, number_preselected_actions = 0) // MCTS with GHOST-playouts
//      aiP1 = MCTS_GHOST(Color.Blue, first_n_strategy = 0) // MCTS with GHOST-masking
      aiP1 = MCTS_GHOST(Color.Blue) // full MCTS GHOST
//      aiP1 = PureHeuristics(Color.Blue)
      Log.d(TAG, "Blue: ${aiP1.toString()}")
    }
    if(p2IsAI) {
      aiP2 =
        MCTS_GHOST(Color.Red, number_preselected_actions = 0, first_n_strategy = 0) // Vanilla-MCTS
//      aiP2 = MCTS_GHOST(Color.Red, number_preselected_actions = 0) // MCTS with GHOST-playouts
//      aiP2 = MCTS_GHOST(Color.Red, first_n_strategy = 0) // MCTS with GHOST-masking
//      aiP2 = MCTS_GHOST(Color.Red) // full MCTS GHOST
//      aiP2 = PureHeuristics(Color.Red)
      Log.d(TAG, "Red: ${aiP2.toString()}")
    }

    _history.clear()
    _forwardHistory.clear()
    _moveHistory.clear()
    _forwardMoveHistory.clear()
    moveNumber = 0
    _game = Game()
    reset()
    lastMovePosition = null
    hasStarted = false
    _gameState.tryEmit(GameState.INIT)
    gameState = _gameState.asStateFlow()
  }

  fun IsAIToPLay(): Boolean {
    return (p1IsAI && _game.currentPlayer == Color.Blue)
    ||
    (p2IsAI && _game.currentPlayer == Color.Red)
  }

  fun goBackMove() {
    _forwardHistory.add(History(_game.board, _game.currentPlayer, moveNumber))
    var last = _history.removeLast()

    var lastMove = _moveHistory.removeLast()
    _forwardMoveHistory.add(lastMove)

    if(p1IsAI || p2IsAI) {
      _forwardHistory.add(last)
      last = _history.removeLast()

      lastMove = _moveHistory.removeLast()
      _forwardMoveHistory.add(lastMove)
    }

    _game.changeWithHistory(last)
    _game.board = last.board
    _game.currentPlayer = last.player
    lastMovePosition = lastMove.to
    moveNumber = last.moveNumber
    reset()
    historyCall = true

    _gameState.value = GameState.HISTORY
  }

  fun goForwardMove() {
    _history.add(History(_game.board, _game.currentPlayer, moveNumber))
    var last = _forwardHistory.removeLast()

    var lastMove = _forwardMoveHistory.removeLast()
    _moveHistory.add(lastMove)

    if(p1IsAI || p2IsAI) {
      _history.add(last)
      last = _forwardHistory.removeLast()

      lastMove = _forwardMoveHistory.removeLast()
      _moveHistory.add(lastMove)
    }

    _game.changeWithHistory(last)
    _game.board = last.board
    _game.currentPlayer = last.player
    lastMovePosition = lastMove.to
    moveNumber = last.moveNumber
    reset()
    historyCall = true

    _gameState.value = GameState.HISTORY
  }

  fun cancelPieceSelection() {
    val newState = GameState.SELECTPIECE
    _gameState.value = newState
    _gameState.tryEmit(newState)
  }

  fun nextGameState(): GameState {
    if(_game.victory) {
      return GameState.END
    }
    return when(_gameState.value) {
      GameState.INIT -> {
        GameState.SELECTPOSITION
      }
      GameState.SELECTPIECE -> {
        GameState.SELECTPOSITION
      }
      GameState.SELECTPOSITION -> {
        _game.checkVictory()
        if(_game.victory)
          GameState.END
        else
          GameState.CHECKGRADUATION
      }
      GameState.CHECKGRADUATION -> {
        val graduations = _game.getGraduations(_game.board)
        var currentPlayerCanGraduate: Boolean = false
        graduations.forEach {
          it.forEach {
            if(_game.board.getGridPosition(it) * _game.currentPlayer.value > 0)
              currentPlayerCanGraduate = true
          }
        }

        if(currentPlayerCanGraduate && stateSelection != GameViewModelState.IDLE) {
          if(_game.getGraduations(_game.board).size == 1)
            GameState.AUTOGRADUATION
          else
            GameState.SELECTGRADUATION
        } else {
          if(twoTypesInPool()) {
            GameState.SELECTPIECE
          } else {
            GameState.SELECTPOSITION
          }
        }
      }
      GameState.AUTOGRADUATION -> {
        if(twoTypesInPool()) {
          GameState.SELECTPIECE
        } else {
          GameState.SELECTPOSITION
        }
      }
      GameState.SELECTGRADUATION -> {
        if(_finishPieceSelection) {
          if(twoTypesInPool()) {
            GameState.SELECTPIECE
          } else {
            GameState.SELECTPOSITION
          }
        } else
          GameState.REFRESHSELECTGRADUATION
      }
      GameState.REFRESHSELECTGRADUATION -> {
        GameState.SELECTGRADUATION
      }
      GameState.HISTORY -> {
        if(twoTypesInPool()) {
          GameState.SELECTPIECE
        } else {
          GameState.SELECTPOSITION
        }
      }
      else -> { //GameState.END, but it should be caught before the when
        GameState.END
      }
    }
  }

  fun goToNextState() {
    if(_game.victory) {
      _gameState.tryEmit(GameState.END)
      return
    }

    var newState: GameState = _gameState.value

    newState = nextGameState()
//        Log.d(TAG, "Change to next state: ${_game.gameState} -> ${newState}")
    _gameState.value = newState

    if(newState == GameState.SELECTPIECE && IsAIToPLay() ) {
      newState = nextGameState()
//            Log.d(TAG, "Change to next state again because AI: ${_game.gameState} -> ${newState}")
      _gameState.value = newState
//            Log.d(TAG, "New game state 2: $newState")
    }

    hasStarted = true
    if(IsAIToPLay()) {
      canGoBack.tryEmit(false)
      canGoForward.tryEmit(false)
    } else {
      canGoBack.tryEmit(if(p1IsAI || p2IsAI) _history.size > 1 else _history.isNotEmpty())
      canGoForward.tryEmit(_forwardHistory.isNotEmpty())
    }

    if(IsAIToPLay() && _gameState.value == GameState.SELECTGRADUATION ) {
      if(p1IsAI && _game.currentPlayer == Color.Blue)
        piecesToPromote = aiP1.select_graduation(_game).toMutableList()
      else
        piecesToPromote = aiP2.select_graduation(_game).toMutableList()
      validateGraduationSelection()
    } else {
//            Log.d(TAG, "Emitting ${newState}, color=${currentPlayer}")
      _gameState.tryEmit(newState)
    }
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
    _history.add(History(_game.board, _game.currentPlayer, moveNumber))

    val piece = when(pieceTypeToPlay) {
      PieceType.Po -> Piece.createPo(_game.currentPlayer)
      PieceType.Bo -> Piece.createBo(_game.currentPlayer)
      null -> Piece.createFromByte(_game.currentPlayer, _game.board.getPlayerPool(_game.currentPlayer).first())
    }
    pieceTypeToPlay = null
    val move = Move(piece, it)
    if(!_game.canPlay(move)) return

    // Print move
    Log.d(TAG, "${move}")
    lastMovePosition = it

    _moveHistory.add(move)
    moveNumber++
    var newBoard = _game.board.playAt(move)
    newBoard = _game.doPush(newBoard, move)
    selectedValue.tryEmit("")

    _game.checkVictoryFor(newBoard, _game.currentPlayer)
    _game.board = newBoard
    goToNextState()
  }

  fun getFlatPromotionable(): List<Position> = _game.getGraduations(_game.board).flatten()

  fun checkGraduation() {
    val groups = _game.getGraduations(_game.board)
    var groupOfAtLeast3 = false
    if(groups.isEmpty() || historyCall) {
      stateSelection = GameViewModelState.IDLE
//            Log.d(TAG, "call changePlayer in checkGraduation")
      _game.changePlayer()
    } else {
      if(groups.size >= 8) {
        for(group in groups) {
          if(group.size >= 3)
            groupOfAtLeast3 = true
        }
        if(groupOfAtLeast3)
          stateSelection = GameViewModelState.SELECT1OR3
        else
          stateSelection = GameViewModelState.SELECT1
      } else
        stateSelection = GameViewModelState.SELECT3
    }
    goToNextState()
  }

  fun autograduation() {
    val graduable = _game.getGraduations(_game.board)
    if(graduable.size == 1 && stateSelection != GameViewModelState.IDLE) {
      graduable[0].forEach {
        _game.board = _game.board.removePieceAndPromoteIt(it)
      }
    }
//        Log.d(TAG, "call changePlayer in autograduation")
    _game.changePlayer()
    goToNextState()
  }

  fun selectForGraduationOrCancel(position: Position) {
    val removable = _game.getGraduations(_game.board)

    if(stateSelection != GameViewModelState.IDLE) {
      // if the player taps a selected piece, unselect it
      if(piecesToPromote.contains(position)) {
        piecesToPromote.remove(position)
        _piecesToPromoteIndex[position] = mutableListOf()
        _promotionListIndex.clear()
        if(piecesToPromote.isEmpty()) {
          _promotionListMask.clear()
        } else {
          _promotionListMask = MutableList(removable.size) { false }
          for(piece in piecesToPromote) {
            _piecesToPromoteIndex[piece] = mutableListOf()
            for((index, list) in removable.withIndex())
              if(list.contains(piece)) {
                _promotionListMask[index] = true
                _promotionListIndex.add(index)
                _piecesToPromoteIndex[piece]?.add(index)
              }
            for(indexValue in _promotionListIndex)
              for(otherPiece in piecesToPromote)
                if(_piecesToPromoteIndex[otherPiece]?.contains(indexValue) != true) {
                  _promotionListIndex.remove(indexValue)
                  _promotionListMask[indexValue] = false
                }
          }
        }
        val toRemove: MutableList<Position> = mutableListOf()
        for((key, list) in _piecesToPromoteIndex)
          if(list.isEmpty())
            toRemove.add(key)
        for(removePiece in toRemove)
          _piecesToPromoteIndex.remove(removePiece)
      } else {
        // _promotionListIndexes is a tedious way to check if a selected piece (in particular the 1st one)
        // belongs to different graduable groups, to make sure we don't select pieces from different groups
        if(_promotionListIndex.isEmpty()) {
          _promotionListMask = MutableList(removable.size) { false }
          for((index, list) in removable.withIndex()) {
            if(list.contains(position)) {
              _promotionListMask[index] = true
              _promotionListIndex.add(index)
              if(!_piecesToPromoteIndex.containsKey(position))
                _piecesToPromoteIndex[position] = mutableListOf()
              _piecesToPromoteIndex[position]?.add(index)
            }
          }
          if(_promotionListIndex.isEmpty())
            return
          else {
            piecesToPromote.add(position)
          }
        } else {
          for((index, list) in removable.withIndex()) {
            if(list.contains(position)) {
              if(_promotionListMask[index]) {
                if(!piecesToPromote.contains(position))
                  piecesToPromote.add(position)
                if(!_piecesToPromoteIndex.containsKey(position))
                  _piecesToPromoteIndex[position] = mutableListOf()
                _piecesToPromoteIndex[position]?.add(index)
              }
            }
          }
          // shrink the index list
          val toRemove: MutableList<Int> = mutableListOf()
          for(indexValue in _promotionListIndex)
            for(piece in piecesToPromote)
              if(_piecesToPromoteIndex[piece]?.contains(indexValue) != true) {
                toRemove.add(indexValue)
                _promotionListMask[indexValue] = false
                for((key, list) in _piecesToPromoteIndex) {
                  if(list.isNotEmpty())
                    list.remove(indexValue)
                }
              }
          for(indexValue in toRemove)
            _promotionListIndex.remove(indexValue)
        }
      }
    }

    // no more than 1 or 3 selections, regarding the situation
    if(piecesToPromote.size == 3 || (stateSelection == GameViewModelState.SELECT1 && piecesToPromote.size == 1)) {
      validateGraduationSelection()
    } else {
      _finishPieceSelection = false
      goToNextState()
    }
  }

  fun validateGraduationSelection() {
    _finishPieceSelection = true
    _game.promoteOrRemovePieces(piecesToPromote)
    _game.checkVictory()
    _promotionListIndex.clear()
    piecesToPromote.forEach {
      Log.d(TAG, "[${it}]")
    }
    piecesToPromote.clear()
    stateSelection = GameViewModelState.IDLE
//        Log.d(TAG, "call changePlayer in validateGraduationSelection")
    _game.changePlayer()
    goToNextState()
  }

  fun makeP1AIMove() {
//        Log.d(TAG, "Make P1 move")
    val move = aiP1.select_move(_game, null, 1000)
    pieceTypeToPlay = move.piece.getType()
    playAt(move.to)
  }

  fun makeP2AIMove() {
//        Log.d(TAG, "Make P2 move")
    val move = aiP2.select_move(_game, null, 1000)
    pieceTypeToPlay = move.piece.getType()
    playAt(move.to)
  }

  fun twoTypesInPool(): Boolean = _game.board.hasTwoTypesInPool(_game.currentPlayer)

  fun resume() = _gameState.tryEmit(_gameState.value)
}
