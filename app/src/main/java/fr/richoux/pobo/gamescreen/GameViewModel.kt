package fr.richoux.pobo.gamescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.richoux.pobo.engine.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private var _gameLoopStep = MutableStateFlow<GameLoopStep>(GameLoopStep.Play(Game()))
    val gameLoopStep: Flow<GameLoopStep> get() = _gameLoopStep

    private var forwardHistory = MutableStateFlow<List<Move>>(listOf())

    private val ai = AI(PieceColor.Red)
    private var aiEnabled = true

    private var selectedPiece: PieceType = PieceType.Po

    val canGoBack = gameLoopStep.map {
        val game = (_gameLoopStep.value as? GameLoopStep.Play)?.game ?: return@map false
        if (aiEnabled)
            return@map game.history.size > 1
        else
            return@map game.history.isNotEmpty()
    }
    val canGoForward = forwardHistory.map { it.isNotEmpty() }
    val mustSelectPiece = gameLoopStep.map {
        return@map _gameLoopStep.value is GameLoopStep.SelectPiece
    }

    fun updateLoop(loopStep: GameLoopStep, game: Game, aiCanPlay: Boolean = true) {
        _gameLoopStep.tryEmit(loopStep)
        if (aiEnabled
            && aiCanPlay
            && game.turn == PieceColor.Red
            && game.gameState == GameState.CONTINUE
        ) {
            viewModelScope.launch {
                val nextMove = ai.calculateNextMove(game, PieceColor.Red)
                if (nextMove != null) {
                    val aiResult = game.nextGameLoopStep(nextMove)
//                    val finalAiResult = when (aiResult) {
//                        is MoveResult.Played -> aiResult
//                        is MoveResult.Promotion -> aiResult.onPieceSelection(PieceType.Queen)
//                    }
                    updateLoop(aiResult)
                }
            }
        }
    }

    fun newGame(aiEnabled: Boolean) {
        this.aiEnabled = aiEnabled

        updateLoop(GameLoopStep.Play(Game()))
        forwardHistory.tryEmit(listOf())
    }

    fun clearForwardHistory() {
        forwardHistory.tryEmit(listOf())
    }

    fun selectPo() {
        val game = (_gameLoopStep.value as? GameLoopStep.SelectPiece)?.game ?: return
        selectedPiece = PieceType.Po
    }

    fun selectBo() {
        val game = (_gameLoopStep.value as? GameLoopStep.SelectPiece)?.game ?: return
        selectedPiece = PieceType.Bo
    }

    fun goBackMove() {
        val game = (_gameLoopStep.value as? GameLoopStep.Play)?.game ?: return

        val lastMove = game.history.last()
        val newHistory =
            if (aiEnabled)
                game.history.subList(0, game.history.size - 2)
            else
                game.history.subList(0, game.history.size - 1)
        val newBoard = Board.fromHistory(newHistory)
        updateLoop(GameLoopStep.Play(Game(newBoard, newHistory)))
        if (aiEnabled) {
            val previousLastMove = game.history[game.history.size - 2]
            forwardHistory.tryEmit(forwardHistory.value + listOf(lastMove, previousLastMove))
        } else
            forwardHistory.tryEmit(forwardHistory.value + listOf(lastMove))
    }

    fun goForwardMove() {
        var game = (_gameLoopStep.value as? GameLoopStep.Play)?.game ?: return

        val move = forwardHistory.value.last()
        updateLoop(game.nextGameLoopStep(move), false)

        if (aiEnabled) {
            game = (_gameLoopStep.value as? GameLoopStep.Play)?.game ?: return
            val previousMove = forwardHistory.value[forwardHistory.value.size - 2]
            updateLoop(game.nextGameLoopStep(previousMove))
        }

        if (aiEnabled)
            forwardHistory.tryEmit(forwardHistory.value.subList(0, forwardHistory.value.size - 2))
        else
            forwardHistory.tryEmit(forwardHistory.value.subList(0, forwardHistory.value.size - 1))
    }
}
}
