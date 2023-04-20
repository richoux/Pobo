package fr.richoux.pobo.gamescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.richoux.pobo.engine.AI
import fr.richoux.pobo.engine.Board
import fr.richoux.pobo.engine.Game
import fr.richoux.pobo.engine.GameState
import fr.richoux.pobo.engine.Move
import fr.richoux.pobo.engine.MoveResult
import fr.richoux.pobo.engine.PieceColor
import fr.richoux.pobo.engine.PieceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private var _moveResult = MutableStateFlow<MoveResult>(MoveResult.Success(Game()))
    val moveResult: Flow<MoveResult> get() = _moveResult

    private var forwardHistory = MutableStateFlow<List<Move>>(listOf())

    private val ai = AI(PieceColor.Black)
    private var aiEnabled = true

    val canGoBack = moveResult.map {
        val game = (_moveResult.value as? MoveResult.Success)?.game ?: return@map false
        if(aiEnabled)
            return@map game.history.size > 1
        else
            return@map game.history.isNotEmpty()
    }
    val canGoForward = forwardHistory.map { it.isNotEmpty() }

    fun updateResult(result: MoveResult, aiCanPlay: Boolean = true) {
        _moveResult.tryEmit(result)

        val game = (result as? MoveResult.Success)?.game ?: return
        if (aiEnabled
            && aiCanPlay
            && game.turn == PieceColor.Black
            && listOf(GameState.CHECK, GameState.IDLE).contains(game.gameState)
        ) {
            viewModelScope.launch {
                val nextMove = ai.calculateNextMove(game, PieceColor.Black)
                if (nextMove != null) {
                    val aiResult = game.doMove(nextMove.from, nextMove.to)
                    val finalAiResult = when (aiResult) {
                        is MoveResult.Success -> aiResult
                        is MoveResult.Promotion -> aiResult.onPieceSelection(PieceType.Queen)
                    }
                    updateResult(finalAiResult)
                }
            }
        }
    }

    fun newGame(aiEnabled: Boolean) {
        this.aiEnabled = aiEnabled

        updateResult(MoveResult.Success(Game()))
        forwardHistory.tryEmit(listOf())
    }

    fun clearForwardHistory() {
        forwardHistory.tryEmit(listOf())
    }

    fun goBackMove() {
        val game = (_moveResult.value as? MoveResult.Success)?.game ?: return

        val lastMove = game.history.last()
        val newHistory =
            if(aiEnabled)
                game.history.subList(0, game.history.size - 2)
            else
                game.history.subList(0, game.history.size - 1)
        val newBoard = Board.fromHistory(newHistory)
        updateResult(MoveResult.Success(Game(newBoard, newHistory)))
        if(aiEnabled) {
            val previousLastMove = game.history[game.history.size - 2]
            forwardHistory.tryEmit(forwardHistory.value + listOf(lastMove, previousLastMove))
        }
        else
            forwardHistory.tryEmit(forwardHistory.value + listOf(lastMove))
    }

    fun goForwardMove() {
        var game = (_moveResult.value as? MoveResult.Success)?.game ?: return

        val move = forwardHistory.value.last()
        updateResult(game.doMove(move.from, move.to), false)

        if(aiEnabled) {
            game = (_moveResult.value as? MoveResult.Success)?.game ?: return
            val previousMove = forwardHistory.value[forwardHistory.value.size - 2]
            updateResult(game.doMove(previousMove.from, previousMove.to))
        }

        if(aiEnabled)
            forwardHistory.tryEmit(forwardHistory.value.subList(0, forwardHistory.value.size - 2))
        else
            forwardHistory.tryEmit(forwardHistory.value.subList(0, forwardHistory.value.size - 1))
    }
}
