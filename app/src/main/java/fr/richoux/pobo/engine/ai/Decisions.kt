package fr.richoux.pobo.engine.ai

import fr.richoux.pobo.engine.*

// return null if no immediate winning move
fun searchForWinningMove( game: Game, movesToRemove: List<Move> ): Move? {
    val board = game.board
    val player = game.currentPlayer

    val pool = when(player) {
        PieceColor.Blue -> board.bluePool
        PieceColor.Red -> board.redPool
    }

    var hasBo = false
    for( p in pool )
        if( p.type == PieceType.Bo )
        {
            hasBo = true
            break
        }

    if( hasBo )
    {
        val emptyPositions = board.getAllEmptyPositions().toMutableList()
        for( m in movesToRemove )
            if( m.piece.type == PieceType.Bo )
                emptyPositions.remove( m.to )

        val newGame = game//.copyForPlayout()

        for( position in emptyPositions ) {
            val move = Move(Piece.createBo(player), position)
            if (newGame.canPlay(move) ) {
                var newBoard = board.playAt(move)
                newBoard = newGame.doPush(newBoard, move)
                if( newGame.checkVictoryFor(newBoard, player) )
                    return move
            }
        }
    }

    return null
}

fun randomPlay( game: Game, movesToRemove: List<Move> ): Move {
    // check first if we have a winning move
    // Commented because VERY time-consuming
//    val winningMove = searchForWinningMove( game, movesToRemove )
//    if( winningMove != null )
//        return winningMove

    // if not, make a random move
    // draw uniformly a piece to play
    val board = game.board
    val player = game.currentPlayer
    val pool = when(player) {
        PieceColor.Blue -> board.bluePool
        PieceColor.Red -> board.redPool
    }

    val piece = pool.random()
    val positions = board.getAllEmptyPositions().toMutableList()
    for( m in movesToRemove )
        if( piece.type == m.piece.type )
            positions.remove( m.to )

    val position = positions.random()

    return Move(piece, position)
}

fun randomPlay( game: Game ): Move = randomPlay( game, listOf<Move>() )
fun randomGraduation( game: Game ): List<Position> = game.getGraduations().random()