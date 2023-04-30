package fr.richoux.pobo.engine

class AI(val color: PieceColor) {
    // This AI doesn't work at all currently
    fun calculateNextMove(game: Game, player: PieceColor): Move {
        return Move(game.board.getPlayerPool(color)[0], search(game)[0])
    }

    fun search(game: Game): List<Position> {
        return game.board.getAllEmptyPositions().shuffled()
    }
}
