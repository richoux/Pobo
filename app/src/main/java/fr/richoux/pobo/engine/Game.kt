package fr.richoux.pobo.engine

import java.util.*

private const val TAG = "pobotag Game"

data class Move(val piece: Piece, val to: Position) {
    override fun toString(): String {
        //return "$piece at $to"
        return when( piece.getType() ) {
            PieceType.Po -> to.poPosition()
            PieceType.Bo -> to.boPosition()
        }
    }
    fun isSame(other: Move?): Boolean {
        return this === other || ( this.piece.isEquivalent( other?.piece ) && this.to.isSame(other?.to) )
    }
}
data class History(val board: Board, val player: Color, val moveNumber: Int) {}

enum class Direction {
    TOPLEFT, TOP, TOPRIGHT, RIGHT, BOTTOMRIGHT, BOTTOM, BOTTOMLEFT, LEFT
}

val scanDirection = EnumSet.range(Direction.TOPRIGHT, Direction.BOTTOM)

fun getPositionTowards(at: Position, direction: Direction) : Position {
    return at + when(direction) {
        Direction.TOPLEFT -> Delta(-1, -1)
        Direction.TOP -> Delta(0, -1)
        Direction.TOPRIGHT -> Delta(1, -1)
        Direction.RIGHT -> Delta(1, 0)
        Direction.BOTTOMRIGHT -> Delta(1, 1)
        Direction.BOTTOM -> Delta(0, 1)
        Direction.BOTTOMLEFT -> Delta(-1, 1)
        else -> Delta(-1, 0) // Direction.LEFT
    }
}

fun isPositionOnTheBoard(at: Position): Boolean {
    return at.x in 0..5 && at.y in 0..5
}

data class Game(
    var board: Board = Board(),
    var currentPlayer: Color = Color.Blue,
    var victory: Boolean = false,
    val isPlayout: Boolean = false, // true if the game is a simulation for decision-making
    var moveNumber: Int = 0
) {
    fun signature(): String {
        var signature = ""
        for (i in 0..35) {
            var p = board.grid[i].toInt()
            if (p < 0)
                p += 10;
            signature += p.toString()
        }
        signature += when( currentPlayer ) {
            Color.Blue -> "B"
            Color.Red -> "R"
        }
        signature += board.getNumberOfBoInPool(Color.Blue).toString()
        signature += board.getNumberOfPoInPool(Color.Blue).toString()
        signature += board.getNumberOfBoInPool(Color.Red).toString()
        signature += board.getNumberOfPoInPool(Color.Red).toString()

        return signature
    }

    fun canPlayAt(to: Position): Boolean = board.pieceAt(to) == null

    fun canPlay(move: Move): Boolean = board.hasPieceInPool(move.piece) && canPlayAt(move.to)

    fun canBePushed(board: Board, pusher: PieceType, myPosition: Position, direction: Direction): Boolean {
        // if myPosition is empty, there is nothing to push
        val myPiece = board.pieceAt(myPosition) ?: return false

        // if I am a Bo, and pusher is a Po, I don't move
        if(myPiece.getType() > pusher) return false

        // if the cell in the pushing direction is not empty, I don't move
        if(board.pieceAt( getPositionTowards(myPosition, direction) ) != null) return false

        return true
    }

    fun copyForPlayout(): Game {
        return Game(
            this.board.copy(),
            this.currentPlayer,
            this.victory,
            isPlayout = true,
            this.moveNumber
        )
    }

    fun doPush(board: Board, move: Move): Board {
        var newBoard = board
        enumValues<Direction>().forEach {
            val victim = getPositionTowards(move.to, it)
            if(canBePushed(newBoard, move.piece.getType(), victim, it)) {
                val target = getPositionTowards(victim, it)
                newBoard = newBoard.slideFromTo(victim, target)
            }
        }
        moveNumber++
        return newBoard
    }

    fun changePlayer() {
        currentPlayer = currentPlayer.other()
    }

    fun getAlignedPositionsInDirection(board: Board, player: Color, position: Position, direction: Direction): List<Position> {
        val alignedPieces: MutableList<Position> = mutableListOf()

        val currentPiece = board.pieceAt(position)
        if(currentPiece?.getColor() == player)
            alignedPieces.add(position)

        val nextPosition = getPositionTowards(position, direction)
        val nextPiece = board.pieceAt(nextPosition)
        if(nextPiece?.getColor() == player)
            alignedPieces.add(nextPosition)

        val nextNextPosition = getPositionTowards(nextPosition, direction)
        val nextNextPiece = board.pieceAt(getPositionTowards(nextPosition, direction))
        if(nextNextPiece?.getColor() == player)
            alignedPieces.add(nextNextPosition)

        return alignedPieces.toList()
    }

    fun isValidAlignedPositions(positions: List<Position>): Boolean = positions.size == 3

    fun countBoInAlignment(board: Board, positions: List<Position>): Int {
        var countBo = 0
        positions.forEach {
            if(board.pieceAt(it)?.getType() == PieceType.Bo)
                countBo++
        }
        return countBo
    }

    fun containsBoOnly(board: Board, positions: List<Position>): Boolean
            = countBoInAlignment(board, positions) == 3

    fun getGraduations(board: Board): List<List<Position>> {
        val graduable: MutableList<List<Position>> = mutableListOf()
        val hasAllPiecesOnTheBoard = board.isPoolEmpty(currentPlayer)

        (0 until 6).map { y ->
            (0 until 6).map { x ->
                val position = Position(x,y)
                val piece = board.pieceAt(position)
                if(piece?.getColor() == currentPlayer){
                    // check if we have 8 pieces on the board
                    if(hasAllPiecesOnTheBoard)
                        graduable.add(listOf(position))

                    // check 3-in-a-row
                    scanDirection.forEach {
                        val alignment = getAlignedPositionsInDirection(
                            board,
                            currentPlayer,
                            position,
                            it
                        )
                        if(isValidAlignedPositions(alignment))
                           graduable.add(alignment)
                    }
                }
            }
        }
        return graduable.toList()
    }

    fun getGraduations(): List<List<Position>> {
        return getGraduations( board )
    }

    fun promoteOrRemovePieces(toPromote: List<Position>) {
        var newBoard: Board = board
        for( position in toPromote) {
            newBoard = newBoard.removePieceAndPromoteIt( position )
        }
        board = newBoard
    }

    fun checkVictoryFor(board: Board, player: Color): Boolean {
        victory = false
        if(board.isPoolEmpty(player) && board.getPlayerNumberBo(player) == 8) {
            victory =  true
        }
        else { // check if current player has at least 3 Bo in line
            (0 until 6).map { y ->
                (0 until 6).map { x ->
                    val position = Position(x,y)
                    val piece = board.pieceAt(position)
                    if(piece?.getColor() == player && piece.getType() == PieceType.Bo ) {
                        scanDirection.forEach {
                            val alignment = getAlignedPositionsInDirection(
                                board,
                                player,
                                position,
                                it
                            )
                            if(isValidAlignedPositions(alignment) && containsBoOnly(board, alignment)) {
                                victory = true
                                return true
                            }
                        }
                    }
                }
            }
        }
        return victory
    }

//    fun checkVictory(board: Board): Boolean {
//        // check if current player has 8 Bo on the board
//        victory = false
//        if(board.isPoolEmpty(currentPlayer) && board.getPlayerNumberBo(currentPlayer) == 8) {
//            victory =  true
//        }
//        else { // check if current player has at least 3 Bo in line
//            (0 until 6).map { y ->
//                (0 until 6).map { x ->
//                    val position = Position(x,y)
//                    val piece = board.pieceAt(position)
//                    if(piece?.color == currentPlayer && piece.type == PieceType.Bo ) {
//                        scanDirection.forEach {
//                            val alignment = getAlignedPositionsInDirection(
//                                board,
//                                currentPlayer,
//                                position,
//                                it
//                            )
//                            if(isValidAlignedPositions(alignment) && containsBoOnly(board, alignment)) {
//                                victory = true
//                                return true
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return victory
//    }

    fun checkVictory(): Boolean = checkVictoryFor(board, currentPlayer)

    fun changeWithHistory(history: History) {
        board = history.board
        moveNumber = history.moveNumber
        currentPlayer = history.player
        //gameState = GameState.PLAY
        checkVictory()
    }
}
