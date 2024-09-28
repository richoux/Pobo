package fr.richoux.pobo.engine.ai

import fr.richoux.pobo.engine.Game
import fr.richoux.pobo.engine.Move
import fr.richoux.pobo.engine.Color
import fr.richoux.pobo.engine.Position

abstract class AI(val color: Color, val aiLevel:Int  = 0) {
  abstract fun select_move(
    game: Game,
    lastOpponentMove: Move?,
    timeout_in_ms: Long = 1000
  ): Move

  abstract fun select_promotion(
    game: Game,
    timeout_in_ms: Long = 1000
  ): List<Position>
}
