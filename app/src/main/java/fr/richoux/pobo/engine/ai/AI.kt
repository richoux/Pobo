package fr.richoux.pobo.engine.ai

import fr.richoux.pobo.engine.Game
import fr.richoux.pobo.engine.Move
import fr.richoux.pobo.engine.Color

class AI(val color: Color) {
    fun calculateNextMove(game: Game, player: Color): Move {
        return randomPlay(game)
    }
}
