package fr.richoux.pobo.gamescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.richoux.pobo.engine.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class Action {
    IDLE, SELECT1, SELECT3, CANCEL, OK
}

class GameViewModel : ViewModel() {
    private var _actions = MutableStateFlow<Action>(Action.IDLE)
    val actions: Flow<Action> get() = _actions
    val action = Action.IDLE

    private val _history: MutableList<Board> = mutableListOf<Board>()
    private val _forwardHistory: MutableList<Board> = mutableListOf<Board>()

    private val _ai = AI(PieceColor.Red)
    private var _aiEnabled = true

    var selectedPieces: List<Position> = listOf()
        private set

    private var _game: Game = Game()

    val canGoBack = if (_aiEnabled) _history.size > 1 else _history.isNotEmpty()
//    val canGoBack = actions.map {
//        if (_aiEnabled)
//            return@map _history.size > 1
//        else
//            return@map _history.isNotEmpty()
//    }

    val canGoForward = _forwardHistory.isNotEmpty()
//    val mustSelectPiece = actions.map {
//        return@map _actions.value is GameLoopStep.SelectPiece
//    }

    fun
    fun getGameState(): GameState {
        return _game.gameState
    }

    fun updateLoop(aiCanPlay: Boolean = true) {
        if (_aiEnabled
            && aiCanPlay
            && _game.turn == PieceColor.Red
            && _game.gameState == GameState.PLAY
        ) {
            viewModelScope.launch {
                val nextMove = _ai.calculateNextMove(_game, PieceColor.Red)
                if (nextMove != null) {
                    val aiResult = _game.nextGameLoopStep(nextMove)
//                    val finalAiResult = when (aiResult) {
//                        is MoveResult.Played -> aiResult
//                        is MoveResult.Promotion -> aiResult.onPieceSelection(PieceType.Queen)
//                    }
                    //updateLoop(aiResult)
                }
            }
        }
    }

//    fun newGame(aiEnabled: Boolean) {
//        this._aiEnabled = aiEnabled
//
//        updateLoop()
//        _forwardHistory.tryEmit(listOf())
//    }

    fun clearForwardHistory() {
        _forwardHistory.clear()
    }

    fun selectPo() {
        val game = (_actions.value as? GameLoopStep.SelectPiece)?.game ?: return
    }

    fun selectBo() {
        val game = (_actions.value as? GameLoopStep.SelectPiece)?.game ?: return
    }

    fun goBackMove() {
        val game = (_actions.value as? GameLoopStep.Play)?.game ?: return

        val lastMove = game.history.last()
        val newHistory =
            if (_aiEnabled)
                game.history.subList(0, game.history.size - 2)
            else
                game.history.subList(0, game.history.size - 1)
        val newBoard = Board.fromHistory(newHistory)
        updateLoop(GameLoopStep.Play(Game(newBoard, newHistory)))
        if (_aiEnabled) {
            val previousLastMove = game.history[game.history.size - 2]
            _forwardHistory.tryEmit(_forwardHistory.value + listOf(lastMove, previousLastMove))
        } else
            _forwardHistory.tryEmit(_forwardHistory.value + listOf(lastMove))
    }

    fun goForwardMove() {
        var game = (_actions.value as? GameLoopStep.Play)?.game ?: return

        val move = _forwardHistory.value.last()
        updateLoop(game.nextGameLoopStep(move), false)

        if (_aiEnabled) {
            game = (_actions.value as? GameLoopStep.Play)?.game ?: return
            val previousMove = _forwardHistory.value[_forwardHistory.value.size - 2]
            updateLoop(game.nextGameLoopStep(previousMove))
        }

        if (_aiEnabled)
            _forwardHistory.tryEmit(_forwardHistory.value.subList(0, _forwardHistory.value.size - 2))
        else
            _forwardHistory.tryEmit(_forwardHistory.value.subList(0, _forwardHistory.value.size - 1))
    }

    fun validate() {
        if (_game.gameState == GameState.SELECTPIECE) {

        }
    }

    fun cancelPieceSelection() {
        if (_game.gameState == GameState.SELECTPOSITION) {
            _game.gameState = GameState.SELECTPIECE
        }
    }
}
