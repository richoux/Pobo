package fr.richoux.pobo.engine.ai

import fr.richoux.pobo.engine.Game
import fr.richoux.pobo.engine.Move
import fr.richoux.pobo.engine.Color
import fr.richoux.pobo.engine.Position

abstract class AI(val color: Color) {
    open fun select_move( game: Game,
                          lastOpponentMove: Move?,
                          timeout_in_ms: Long = 1000 ): Move {
        return randomPlay( game )
    }

    open fun select_graduation( game: Game,
                                timeout_in_ms: Long = 1000 ): List<Position> {
        return randomGraduation( game )
    }
}
