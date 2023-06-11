package fr.richoux.pobo.engine.ai

import fr.richoux.pobo.engine.Game
import fr.richoux.pobo.engine.Move
import fr.richoux.pobo.engine.PieceColor
import fr.richoux.pobo.engine.Position

class AI(val color: PieceColor) {
    fun calculateNextMove(game: Game, player: PieceColor): Move {
        return randomPlay(game)
    }
}
