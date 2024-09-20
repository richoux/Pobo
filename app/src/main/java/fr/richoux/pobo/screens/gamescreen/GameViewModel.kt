package fr.richoux.pobo.screens.gamescreen

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import fr.richoux.pobo.R
import fr.richoux.pobo.engine.*
import fr.richoux.pobo.engine.ai.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "pobotag GameViewModel"

enum class PromotionType {
  NONE, SELECT1, SELECT3, SELECT1OR3
}

class GameViewModel : ViewModel() {
  /*** AI ***/
  var p1IsAI = true
    private set
  var p2IsAI = true
    private set
  private var aiP1: AI = RandomPlay(Color.Blue)
  private var aiP2: AI = RandomPlay(Color.Red)
  var xp = false
    private set
  var countNumberGames = 0
    private set

  /*** States ***/
  private var _game: Game = Game()

  data class BoardViewState(
    val board: Board,
    val lastMovePosition: Position?,
    val promotables: List<List<Position>>,
    val promotionList: List<Position>,
    val tapAction: (Position) -> Unit,
    val stringForDebug: String = ""
  )

  data class PoolViewState(
    val messageID: Int,
    val currentPlayer: Color,
    val selectedPieceType: PieceType?,
    val canValidatePromotion: Boolean,
    val numberBluePo: Int,
    val numberBlueBo: Int,
    val numberRedPo: Int,
    val numberRedBo: Int
  )

//  private val _boardViewState = mutableStateOf(
//    BoardViewState(
//      _game.board,
//      null,
//      listOf(),
//      listOf(),
//      { _ -> }
//    )
//  )
//  val boardViewState: State<BoardViewState> get() = _boardViewState

  private val _boardViewState = MutableStateFlow(
    BoardViewState(
      _game.board,
      null,
      listOf(),
      listOf(),
      { _ -> }
    )
  )
  val boardViewState: StateFlow<BoardViewState> = _boardViewState.asStateFlow()

//  private val _poolViewState = mutableStateOf(
//    PoolViewState(
//      -1,
//      Color.Blue,
//      null,
//      false,
//      8,
//      0,
//      8,
//      0
//    )
//  )
//  val poolViewState: State<PoolViewState> get() = _poolViewState

  private val _poolViewState = MutableStateFlow(
    PoolViewState(
      -1,
      Color.Blue,
      null,
      false,
      8,
      0,
      8,
      0
    )
  )
  val poolViewState: StateFlow<PoolViewState> = _poolViewState.asStateFlow()

  /*** Game History ***/
  private val _history: MutableList<History> = mutableListOf()
  private val _forwardHistory: MutableList<History> = mutableListOf()
  private val _moveHistory: MutableList<Move> = mutableListOf()
  private val _forwardMoveHistory: MutableList<Move> = mutableListOf()
  var moveNumber: Int = 0
    private set
  var canGoBack = MutableStateFlow<Boolean>(if(p2IsAI) _history.size > 1 else _history.isNotEmpty())
  var canGoForward = MutableStateFlow<Boolean>(_forwardHistory.isNotEmpty())

  /*** Piece Promotions ***/
  var promotionType = PromotionType.NONE
    private set
  private var _promotionListIndex: MutableList<Int> = mutableListOf()
  private var _promotionListMask: MutableList<Boolean> = mutableListOf()
  private var _piecesToPromoteIndex: HashMap<Position, MutableList<Int>> = hashMapOf()

  /*** Other Variables ***/
  var animations: MutableMap<Position, Pair<Position, Piece>> = mutableMapOf()
    private set

  var hasStarted: Boolean = false

  lateinit var navController: NavController
    private set

  /*** Member functions ***/
  fun aiP1(): String = aiP1.toString()
  fun aiP2(): String = aiP2.toString()

  val tapToPlay: (Position) -> Unit = {
    if(
      canPlayAt(it)
      && !IsAIToPLay()
      && (_poolViewState.value.selectedPieceType != null || !twoTypesInPool())
    ) {
      playAt(it)
    }
  }

  val tapToPromote: (Position) -> Unit = {
    var promotableAtPosition = false
    for(group in _boardViewState.value.promotables)
      if(group.contains(it)) {
        promotableAtPosition = true
        break
      }

    if(
      !IsAIToPLay()
      && promotableAtPosition
    ) {
      selectForPromotionOrCancel(it)
    }
  }

  fun reset() {
    _promotionListIndex = mutableListOf()
    _promotionListMask = mutableListOf()
    _piecesToPromoteIndex = hashMapOf()
    if(IsAIToPLay()) {
      canGoBack.tryEmit(false)
      canGoForward.tryEmit(false)
    } else {
      canGoBack.tryEmit(if(p2IsAI) _history.size > 1 else _history.isNotEmpty())
      canGoForward.tryEmit(_forwardHistory.isNotEmpty())
    }
  }

  fun newGame(navController: NavController, p1IsAI: Boolean, p2IsAI: Boolean, xp: Boolean) {
    this.navController = navController
    this.p1IsAI = p1IsAI
    this.p2IsAI = p2IsAI
    this.xp = xp
    countNumberGames++

    if(p1IsAI) {
//      aiP1 = MCTS_GHOST(Color.Blue, number_preselected_actions = 0, expansions_with_GHOST = false, first_n_strategy = 0, playout_depth = 0) // Vanilla-MCTS
//      aiP1 = MCTS_GHOST(Color.Blue, expansions_with_GHOST = false, first_n_strategy = 0, playout_depth = 0) // MCTS + Selection
//      aiP1 = MCTS_GHOST(Color.Blue, number_preselected_actions = 0, first_n_strategy = 0, playout_depth = 0) // MCTS + Expansion
//      aiP1 = MCTS_GHOST(Color.Blue, number_preselected_actions = 0, expansions_with_GHOST = false) // MCTS + Playout
//      aiP1 = MCTS_GHOST(Color.Blue, first_n_strategy = 0, playout_depth = 0) // MCTS + Selection + Expansion
//      aiP1 = MCTS_GHOST(Color.Blue, expansions_with_GHOST = false) // MCTS + Selection + Playout
//      aiP1 = MCTS_GHOST(Color.Blue, number_preselected_actions = 0) // MCTS + Expansion + Playout
      aiP1 = MCTS_GHOST(Color.Blue) // full-GHOSTed MCTS
//      aiP1 = PureHeuristics(Color.Blue)
      if(!xp || countNumberGames == 1)
        Log.d(TAG, "Blue: ${aiP1.toString()}")
    }
    if(p2IsAI) {
//      aiP2 = MCTS_GHOST(Color.Red, number_preselected_actions = 0, expansions_with_GHOST = false, first_n_strategy = 0, playout_depth = 0) // Vanilla-MCTS
//      aiP2 = MCTS_GHOST(Color.Red, expansions_with_GHOST = false, first_n_strategy = 0, playout_depth = 0) // MCTS + Selection
//      aiP2 = MCTS_GHOST(Color.Red, number_preselected_actions = 0, first_n_strategy = 0, playout_depth = 0) // MCTS + Expansion
//      aiP2 = MCTS_GHOST(Color.Red, number_preselected_actions = 0, expansions_with_GHOST = false) // MCTS + Playout
//      aiP2 = MCTS_GHOST(Color.Red, first_n_strategy = 0, playout_depth = 0) // MCTS + Selection + Expansion
//      aiP2 = MCTS_GHOST(Color.Red, expansions_with_GHOST = false) // MCTS + Selection + Playout
//      aiP2 = MCTS_GHOST(Color.Red, number_preselected_actions = 0) // MCTS + Expansion + Playout
      aiP2 = MCTS_GHOST(Color.Red) // full-GHOSTed MCTS
//      aiP2 = PureHeuristics(Color.Red)
      if(!xp || countNumberGames == 1)
        Log.d(TAG, "Red: ${aiP2.toString()}")
    }

    _history.clear()
    _forwardHistory.clear()
    _moveHistory.clear()
    _forwardMoveHistory.clear()
    animations.clear()
    moveNumber = 0
    _game = Game()
    reset()
    hasStarted = false
    _boardViewState.value = BoardViewState(
      _game.board,
      null,
      listOf(),
      listOf(),
      if(!p1IsAI) tapToPlay else { _ -> }
    )
    _poolViewState.value = PoolViewState(
      -1,
      Color.Blue,
      null,
      false,
      8,
      0,
      8,
      0
    )

    if(p1IsAI)
      makeP1AIMove()
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
    Log.d(TAG, "Cancel move ${lastMove}")

    if(p1IsAI || p2IsAI) {
      _forwardHistory.add(last)
      last = _history.removeLast()
      lastMove = _moveHistory.removeLast()
      _forwardMoveHistory.add(lastMove)
      Log.d(TAG, "Cancel move ${lastMove}")
    }

    _game.changeWithHistory(last)
    _game.board = last.board
    _game.currentPlayer = last.player
    animations.clear()

    val promotables = _game.getPossiblePromotions(_game.board)

    _boardViewState.update { currentState ->
      currentState.copy(
        board = _game.board,
        lastMovePosition = if(_moveHistory.isEmpty()) null else _moveHistory.last().to,
        promotables = promotables,
        promotionList = listOf(),
        tapAction = tapToPlay
      )
    }

    _poolViewState.update { currentState ->
      currentState.copy(
        currentPlayer = _game.currentPlayer,
        canValidatePromotion = false,
        numberBluePo = _game.board.getNumberOfPoInPool(Color.Blue),
        numberBlueBo = _game.board.getNumberOfBoInPool(Color.Blue),
        numberRedPo = _game.board.getNumberOfPoInPool(Color.Red),
        numberRedBo = _game.board.getNumberOfBoInPool(Color.Red),
      )
    }

    if(twoTypesInPool()) {
      mustSelectPiece()
    } else {
      if(_game.board.getPlayerPool(_game.currentPlayer)
          .first() == 1.toByte()
      ) // only Po in the pool
        selectPo()
      else
        selectBo()
    }

    moveNumber = last.moveNumber
    reset()
  }

  fun goForwardMove() {
    _history.add(History(_game.board, _game.currentPlayer, moveNumber))
    var last = _forwardHistory.removeLast()
    var lastMove = _forwardMoveHistory.removeLast()
    _moveHistory.add(lastMove)
    Log.d(TAG, "Redo move ${lastMove}")

    if(p1IsAI || p2IsAI) {
      _history.add(last)
      last = _forwardHistory.removeLast()
      lastMove = _forwardMoveHistory.removeLast()
      _moveHistory.add(lastMove)
      Log.d(TAG, "Redo move ${lastMove}")
    }

    _game.changeWithHistory(last)
    _game.board = last.board
    _game.currentPlayer = last.player
    animations.clear()

    val promotables = _game.getPossiblePromotions(_game.board)

    _boardViewState.update { currentState ->
      currentState.copy(
        board = _game.board,
        lastMovePosition = lastMove.to,
        promotables = promotables,
        promotionList = listOf(),
        tapAction = tapToPlay
      )
    }

    _poolViewState.update { currentState ->
      currentState.copy(
        currentPlayer = _game.currentPlayer,
        canValidatePromotion = false,
        numberBluePo = _game.board.getNumberOfPoInPool(Color.Blue),
        numberBlueBo = _game.board.getNumberOfBoInPool(Color.Blue),
        numberRedPo = _game.board.getNumberOfPoInPool(Color.Red),
        numberRedBo = _game.board.getNumberOfBoInPool(Color.Red),
      )
    }

    if(twoTypesInPool()) {
      mustSelectPiece()
    } else {
      if(_game.board.getPlayerPool(_game.currentPlayer)
          .first() == 1.toByte()
      ) // only Po in the pool
        selectPo()
      else
        selectBo()
    }

    moveNumber = last.moveNumber
    reset()
  }

  fun selectPo() {
    _poolViewState.update { currentState ->
      currentState.copy(
        messageID = -1,
        selectedPieceType = PieceType.Po
      )
    }
  }

  fun selectBo() {
    _poolViewState.update { currentState ->
      currentState.copy(
        messageID = -1,
        selectedPieceType = PieceType.Bo
      )
    }
  }

  fun mustSelectPiece() {
    _poolViewState.update { currentState ->
      currentState.copy(
        messageID = R.string.select_piece,
        selectedPieceType = null
      )
    }
  }

  fun canPlayAt(it: Position): Boolean = _game.canPlayAt(it)

  fun computePieceTypeToPlay(): PieceType {
    return when(_poolViewState.value.selectedPieceType) {
      PieceType.Po -> PieceType.Po
      PieceType.Bo -> PieceType.Bo
      null -> when(_game.board.getPlayerPool(_game.currentPlayer).first()) {
        1.toByte() -> PieceType.Po
        else -> PieceType.Bo
      }
    }
  }

  fun playAt(movePosition: Position) {
    _forwardHistory.clear()
    _forwardMoveHistory.clear()
    _history.add(History(_game.board, _game.currentPlayer, moveNumber))

    computeAnimation(movePosition)

    val piece = when(_poolViewState.value.selectedPieceType) {
      PieceType.Po -> Piece.createPo(_game.currentPlayer)
      PieceType.Bo -> Piece.createBo(_game.currentPlayer)
      null -> Piece.createFromByte(
        _game.currentPlayer,
        _game.board.getPlayerPool(_game.currentPlayer).first()
      )
    }
    _poolViewState.update { currentState ->
      currentState.copy(
        selectedPieceType = null
      )
    }
    val move = Move(piece, movePosition)
    if(!_game.canPlay(move)) return

    // Print move
    if(!xp)
      Log.d(TAG, "${move}")

    _moveHistory.add(move)
    moveNumber++
    var newBoard = _game.board.playAt(move)
    newBoard = _game.doPush(newBoard, move)

    _game.checkVictoryFor(newBoard, _game.currentPlayer)
    _game.board = newBoard

    _boardViewState.update { currentState ->
      currentState.copy(
        board = _game.board,
        lastMovePosition = movePosition,
        tapAction = { _ -> },
        stringForDebug = "playAt_before_check"
      )
    }

    _poolViewState.update { currentState ->
      currentState.copy(
        numberBluePo = _game.board.getNumberOfPoInPool(Color.Blue),
        numberBlueBo = _game.board.getNumberOfBoInPool(Color.Blue),
        numberRedPo = _game.board.getNumberOfPoInPool(Color.Red),
        numberRedBo = _game.board.getNumberOfBoInPool(Color.Red),
      )
    }

    checkPromotions()
    nextStep()
  }

  fun nextStep() {
    when(_boardViewState.value.promotables.size) {
      0 -> {
        _game.changePlayer()
        _poolViewState.update { currentState ->
          currentState.copy(
            currentPlayer = _game.currentPlayer,
          )
        }

        if(IsAIToPLay()) {
          canGoBack.tryEmit(false)
          canGoForward.tryEmit(false)

//          GlobalScope.launch(Dispatchers.Main){
//            delay(200)
//          }
          makeAIMove()
        } else {
          canGoBack.tryEmit(if(p1IsAI || p2IsAI) _history.size > 1 else _history.isNotEmpty())
          canGoForward.tryEmit(_forwardHistory.isNotEmpty())

          _boardViewState.update { currentState ->
            currentState.copy(
              tapAction = tapToPlay,
              stringForDebug = "playAt_after_check_if_no_promo"
            )
          }
          if(twoTypesInPool()) {
            mustSelectPiece()
          } else {
            if(!_game.board.getPlayerPool(_game.currentPlayer).isEmpty()) { // should always be true
              if(_game.board.getPlayerPool(_game.currentPlayer)
                  .first() == 1.toByte()
              ) // only Po in the pool
                selectPo()
              else
                selectBo()
            } else { // should never happen
              _poolViewState.update { currentState ->
                currentState.copy(
                  messageID = -1,
                  selectedPieceType = null
                )
              }
            }
          }
        }
      }
//      1 -> {
//        autopromotions()
//        if( IsAIToPLay() ) {
//          makeAIMove()
//        }
//      }
      else -> {
        canGoBack.tryEmit(false)
        canGoForward.tryEmit(false)
        if(IsAIToPLay()) {
          if(p1IsAI && _poolViewState.value.currentPlayer == Color.Blue) {
            _boardViewState.update { currentState ->
              currentState.copy(
                promotionList = aiP1.select_promotion(_game),
                stringForDebug = "playAt_after_check_if_multi_promo_and_AI_Blue"
              )
            }
          } else {
            _boardViewState.update { currentState ->
              currentState.copy(
                promotionList = aiP2.select_promotion(_game),
                stringForDebug = "playAt_after_check_if_multi_promo_and_AI_Red"
              )
            }
          }
          validatePromotionsSelection()
        } else {
          _boardViewState.update { currentState ->
            currentState.copy(
              tapAction = tapToPromote,
              stringForDebug = "playAt_after_check_if_multi_promo"
            )
          }
          _poolViewState.update { currentState ->
            currentState.copy(
              messageID = R.string.select_promotion,
              selectedPieceType = null
            )
          }
        }
      }
    }
  }

  fun checkPromotions() {
    val groups = _game.getPossiblePromotions(_game.board)
    var groupOfAtLeast3 = false
    if(groups.isEmpty()) {
      promotionType = PromotionType.NONE
    } else {
      if(groups.size >= 8) {
        for(group in groups) {
          if(group.size >= 3)
            groupOfAtLeast3 = true
        }
        if(groupOfAtLeast3)
          promotionType = PromotionType.SELECT1OR3
        else
          promotionType = PromotionType.SELECT1
      } else {
        promotionType = PromotionType.SELECT3
      }
    }

    _boardViewState.update { currentState ->
      currentState.copy(
        promotables = groups,
        stringForDebug = "check"
      )
    }
  }

  fun autopromotions() {
    val promotable = _boardViewState.value.promotables //_game.getPossiblePromotions(_game.board)
    if(promotable.size == 1 && promotionType != PromotionType.NONE) {
      promotable[0].forEach {
        _game.board = _game.board.removePieceAndPromoteIt(it)
      }
    }
    promotionType = PromotionType.NONE
    _game.changePlayer()

    _boardViewState.update { currentState ->
      currentState.copy(
        board = _game.board,
        promotables = listOf(),
        tapAction = tapToPlay,
        stringForDebug = "auto"
      )
    }
    _poolViewState.update { currentState ->
      currentState.copy(
        currentPlayer = _game.currentPlayer,
        numberBluePo = _game.board.getNumberOfPoInPool(Color.Blue),
        numberBlueBo = _game.board.getNumberOfBoInPool(Color.Blue),
        numberRedPo = _game.board.getNumberOfPoInPool(Color.Red),
        numberRedBo = _game.board.getNumberOfBoInPool(Color.Red),
      )
    }

    if( IsAIToPLay() ) {
      makeAIMove()
    } else {
      _boardViewState.update { currentState ->
        currentState.copy(
          tapAction = tapToPlay,
          stringForDebug = "auto_2"
        )
      }
    }
  }

  fun selectForPromotionOrCancel(position: Position) {
    val promotable = _boardViewState.value.promotables
    val piecesToPromote = _boardViewState.value.promotionList.toMutableList()
//    Log.d(TAG, "===> CALL selectForPromotionOrCancel")
//    Log.d(TAG, "piecesToPromote: ")
//    for(it in piecesToPromote) { Log.d(TAG, "${it} ") }
//    Log.d(TAG, "_promotionListIndex: ")
//    for(it in _promotionListIndex) { Log.d(TAG, "${it} ") }
//    Log.d(TAG, "_promotionListMask: ")
//    for(it in _promotionListMask) { Log.d(TAG, "${it} ") }
//    Log.d(TAG, "_piecesToPromoteIndex: ")
//    for(it in _piecesToPromoteIndex) { Log.d(TAG, "${it} ") }

    if(promotionType != PromotionType.NONE) {
      // if the player taps a selected piece, unselect it
//      Log.d(TAG, "promotionType != PromotionType.NONE")
      if(piecesToPromote.contains(position)) {
//        Log.d(TAG, "Removing stuff")
//        Log.d(TAG, "About to remove ${position} from promotion")
        piecesToPromote.remove(position)
//        Log.d(TAG, "Remove done")
        _piecesToPromoteIndex[position] = mutableListOf()
        _promotionListIndex.clear()
        if(piecesToPromote.isEmpty()) {
//          Log.d(TAG, "Promotion list empty")
          _promotionListMask.clear()
        } else {
//          Log.d(TAG, "Update mark")
          // Rescan all group indexes in which a piece selected for promotion belongs to
          _promotionListMask = MutableList(promotable.size) { false }
          for(piece in piecesToPromote) {
//            Log.d(TAG, "For piece ${piece}")
            _piecesToPromoteIndex[piece] = mutableListOf()
            for((index, list) in promotable.withIndex())
              if(list.contains(piece)) {
//                Log.d(TAG, "Add promotable list ${index} since it contains ${piece}")
                _promotionListMask[index] = true
                if(!_promotionListIndex.contains(index))
                  _promotionListIndex.add(index)
                _piecesToPromoteIndex[piece]?.add(index)
              }
          }
          // Remove group indexes that are not shared by all pieces selected for promotion
          val toRemove: MutableList<Int> = mutableListOf()
          for(indexValue in _promotionListIndex) {
//            Log.d(TAG, "Looking at list ${indexValue}")
            for(piece in piecesToPromote) {
//              Log.d(TAG, "Looking at piece ${piece}")
              if(_piecesToPromoteIndex.containsKey(piece) && _piecesToPromoteIndex[piece]?.contains(indexValue) != true) {
//                Log.d(
//                  TAG,
//                  "_piecesToPromoteIndex[${piece}]=${_piecesToPromoteIndex[piece]} does not contain ${indexValue}"
//                )
//                Log.d(TAG, "About to remove ${indexValue} from promotionList")
                toRemove.add(indexValue)
                _promotionListMask[indexValue] = false
//                Log.d(TAG, "Remove done")
              }
            }
          }
          for(removeIndex in toRemove) {
            _promotionListIndex.remove(removeIndex)
          }
        }
        val toRemove: MutableList<Position> = mutableListOf()
        for((key, list) in _piecesToPromoteIndex)
          if(list.isEmpty())
            toRemove.add(key)
        for(removePiece in toRemove) {
//          Log.d(TAG, "About to remove ${removePiece} from promoteIndex")
          _piecesToPromoteIndex.remove(removePiece)
//          Log.d(TAG, "Remove done")
        }
      } else {
        // _promotionListIndexes is a tedious way to check if a selected piece (in particular the 1st one)
        // belongs to different promotable groups, to make sure we don't select pieces from different groups
        if(_promotionListIndex.isEmpty()) {
//          Log.d(TAG, "Still nothing in the promotion list")
          _promotionListMask = MutableList(promotable.size) { false }
          for((index, list) in promotable.withIndex()) {
            if(list.contains(position)) {
//              Log.d(TAG, "Mask and add promotable list ${index}")
              _promotionListMask[index] = true
              _promotionListIndex.add(index)
              if(!_piecesToPromoteIndex.containsKey(position))
                _piecesToPromoteIndex[position] = mutableListOf()
              _piecesToPromoteIndex[position]?.add(index)
            }
          }
          if(!_promotionListIndex.isEmpty())
            piecesToPromote.add(position)
        } else {
//          Log.d(TAG, "There is something in the promotion list already.")
          for((index, list) in promotable.withIndex()) {
            if(list.contains(position)) {
//              Log.d(TAG, "Mark ${index} for promotion")
              if(_promotionListMask[index]) {
                if(!piecesToPromote.contains(position))
                  piecesToPromote.add(position)
                if(!_piecesToPromoteIndex.containsKey(position))
                  _piecesToPromoteIndex[position] = mutableListOf()
                _piecesToPromoteIndex[position]?.add(index)
              }
            }
          }
          // Filter the group index list
          val toRemove: MutableList<Int> = mutableListOf()
          for(indexValue in _promotionListIndex)
            for(piece in piecesToPromote)
              if(_piecesToPromoteIndex[piece]?.contains(indexValue) != true) {
//                Log.d(TAG, "_piecesToPromoteIndex[${piece}]=${_piecesToPromoteIndex[piece]} does not contain ${indexValue}")
                toRemove.add(indexValue)
                _promotionListMask[indexValue] = false
                for((key, list) in _piecesToPromoteIndex) {
                  if(list.isNotEmpty()) {
//                    Log.d(TAG, "Shrinking the promotion list by removing ${indexValue} from _piecesToPromoteIndex[${key}]=${list}")
                    list.remove(indexValue)
//                    Log.d(TAG, "Remove done")
                  }
                }
              }
          for(indexValue in toRemove) {
//            Log.d(TAG, "Shrinking the index list by removing ${indexValue} from _promotionListIndex")
            _promotionListIndex.remove(indexValue)
//            Log.d(TAG, "Remove done")
          }
        }
      }
    }

//    Log.d(TAG, "piecesToPromote: ")
//    for(it in piecesToPromote) { Log.d(TAG, "${it} ") }
//    Log.d(TAG, "_promotionListIndex: ")
//    for(it in _promotionListIndex) { Log.d(TAG, "${it} ") }
//    Log.d(TAG, "_promotionListMask: ")
//    for(it in _promotionListMask) { Log.d(TAG, "${it} ") }
//    Log.d(TAG, "_piecesToPromoteIndex: ")
//    for(it in _piecesToPromoteIndex) { Log.d(TAG, "${it} ") }

    val completeSelectionForRemoval =
      ((promotionType == PromotionType.SELECT3 || promotionType == PromotionType.SELECT1OR3) && piecesToPromote.size == 3)
      || ((promotionType == PromotionType.SELECT1 || promotionType == PromotionType.SELECT1OR3) && piecesToPromote.size == 1)

    _boardViewState.update { currentState ->
      currentState.copy(
        promotionList = piecesToPromote.toList(),
        stringForDebug = "select"
      )
    }

    _poolViewState.update { currentState ->
      currentState.copy(
        canValidatePromotion = completeSelectionForRemoval
      )
    }

    // no more than 1 or 3 selections, regarding the situation
//    if(piecesToPromote.size == 3 || (promotionType == PromotionType.SELECT1 && piecesToPromote.size == 1)) {
//      validatePromotionsSelection()
//    }
  }

  fun validatePromotionsSelection() {
    _promotionListIndex = mutableListOf()
    _promotionListMask = mutableListOf()
    _piecesToPromoteIndex = hashMapOf()
    animations.clear()
    _game.promoteOrRemovePieces(_boardViewState.value.promotionList)
    _game.checkVictory() //ToDo when do we check victory now?
//    _promotionListIndex.clear()
//    if(!xp) {
//      piecesToPromote.forEach {
//        Log.d(TAG, "[${it}]")
//      }
//    }
//    piecesToPromote.clear()
    promotionType = PromotionType.NONE
    _game.changePlayer()

    _boardViewState.update { currentState ->
      currentState.copy(
        board = _game.board,
        promotables = listOf(),
        promotionList = listOf(),
        stringForDebug = "validate"
      )
    }
      _poolViewState.update { currentState ->
      currentState.copy(
        currentPlayer = _game.currentPlayer,
        numberBluePo = _game.board.getNumberOfPoInPool(Color.Blue),
        numberBlueBo = _game.board.getNumberOfBoInPool(Color.Blue),
        numberRedPo = _game.board.getNumberOfPoInPool(Color.Red),
        numberRedBo = _game.board.getNumberOfBoInPool(Color.Red),
      )
    }

    if( IsAIToPLay() ) {
      makeAIMove()
    } else {
      _boardViewState.update { currentState ->
        currentState.copy(
          tapAction = tapToPlay,
          stringForDebug = "validate_2"
        )
      }

      canGoBack.tryEmit(if(p1IsAI || p2IsAI) _history.size > 1 else _history.isNotEmpty())
      canGoForward.tryEmit(_forwardHistory.isNotEmpty())
    }
  }

  fun makeAIMove() {
    if( _poolViewState.value.currentPlayer == Color.Blue)
      makeP1AIMove()
    else
      makeP2AIMove()
  }

  fun makeP1AIMove() {
    val move = aiP1.select_move(_game, null, 1000)
    _poolViewState.update { currentState ->
      currentState.copy(
        selectedPieceType = move.piece.getType()
      )
    }
    playAt(move.to)
  }

  fun makeP2AIMove() {
    val move = aiP2.select_move(_game, null, 1000)
    _poolViewState.update { currentState ->
      currentState.copy(
        selectedPieceType = move.piece.getType()
      )
    }
    playAt(move.to)
  }

  fun twoTypesInPool(): Boolean = _game.board.hasTwoTypesInPool(_game.currentPlayer)
  fun hasPromotables(): Boolean = !_boardViewState.value.promotables.isEmpty()

  fun canBePushed(victim: Position, it: Direction): Boolean {
    return _game.canBePushed(_game.board, computePieceTypeToPlay(), victim, it)
  }

  fun computeAnimation(movePosition: Position) {
    animations.clear()
    enumValues<Direction>().forEach {
      val moveFrom = getPositionTowards(movePosition, it)
      if(canBePushed(moveFrom, it)) {
        val moveTo = getPositionTowards(moveFrom, it)
        val piece = _game.board.pieceAt(moveFrom)
        animations[moveFrom] = Pair(moveTo, piece!!)
      }
    }
  }
}

