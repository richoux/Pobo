package fr.richoux.pobo.engine.ai

import fr.richoux.pobo.engine.Game
import fr.richoux.pobo.engine.Move
import fr.richoux.pobo.engine.Color

abstract class AI(val color: Color) {
    abstract fun select_move( game: Game,
                              lastOpponentMove: Move,
                              timeout_in_ms: Long = 1000 ): Move
}
