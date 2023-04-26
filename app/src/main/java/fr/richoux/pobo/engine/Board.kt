package fr.richoux.pobo.engine

import androidx.annotation.DrawableRes
import fr.richoux.pobo.R

sealed class PieceType(val value: Int) {
    object Po : PieceType(1) // small ones
    object Bo : PieceType(2) // big ones

    operator fun compareTo( other: PieceType ): Int {
        return this.value - other.value
    }
}

sealed class PieceColor {
    object Blue : PieceColor()
    object Red : PieceColor()

    fun other(): PieceColor {
        return if (this == Blue) Red else Blue
    }
}

private fun pieceTypeFromId(id: String): Pair<PieceType, PieceColor> {
    val chars = id.toCharArray()
    if (chars.size != 2) throw IllegalStateException("Piece id should be 2 characters")
    val pieceColor = when (chars[0]) {
        'B' -> PieceColor.Blue
        'R' -> PieceColor.Red
        else -> throw IllegalStateException("First character should be B or R")
    }
    val pieceType = when (chars[1]) {
        'P' -> PieceType.Po
        'B' -> PieceType.Bo
        else -> throw IllegalStateException("Second character should be a piece type")
    }
    return pieceType to pieceColor
}

data class Piece(val id: String, val type: PieceType, val color: PieceColor) {
    companion object {
        fun pieceOrNullFromString(id: String?): Piece? {
            val id = id ?: return null
            val types = pieceTypeFromId(id)
            return Piece(id, types.first, types.second)
        }

        fun pieceFromString(id: String): Piece {
            val types = pieceTypeFromId(id)
            return Piece(id, types.first, types.second)
        }
    }

    @DrawableRes
    fun imageResource(): Int {
        return when (type) {
            PieceType.Po -> if (color is PieceColor.Blue) R.drawable.small_circle else R.drawable.small_cross
            PieceType.Bo -> if (color is PieceColor.Blue) R.drawable.big_circle else R.drawable.big_cross
        }
    }
}

data class Delta(val x: Int, val y: Int)
data class Position(val x: Int, val y: Int) {
    operator fun plus(other: Position): Delta {
        return Delta(this.x + other.x, this.y + other.y)
    }

    operator fun minus(other: Position): Delta {
        return Delta(this.x - other.x, this.y - other.y)
    }

    operator fun plus(other: Delta): Position {
        return Position(this.x + other.x, this.y + other.y)
    }
    fun isSame(other: Position?): Boolean {
        return this === other || ( this.x == other?.x && this.y == other?.y )
    }

//    override fun equals(other: Any?): Boolean {
//        return this === other || ( this.x == other?.x && this.y == other?.y )
//    }
}

private val INITIAL_BOARD = listOf(
    listOf(null, null, null, null, null, null).map { Piece.pieceOrNullFromString(it) },
    listOf(null, null, null, null, null, null).map { Piece.pieceOrNullFromString(it) },
    listOf(null, null, null, null, null, null).map { Piece.pieceOrNullFromString(it) },
    listOf(null, null, null, null, null, null).map { Piece.pieceOrNullFromString(it) },
    listOf(null, null, null, null, null, null).map { Piece.pieceOrNullFromString(it) },
    listOf(null, null, null, null, null, null).map { Piece.pieceOrNullFromString(it) },
)

val bluePool = MutableList(8){ Piece.pieceFromString("BP") }
val blueReserve = MutableList(8){ Piece.pieceFromString("BB") }

val redPool = MutableList(8){ Piece.pieceFromString("RP") }
val redReserve = MutableList(8){ Piece.pieceFromString("RB") }

fun getPlayerPool(color: PieceColor): MutableList<Piece> {
    return when(color) {
        PieceColor.Blue -> bluePool
        else -> redPool
    }
}

fun getPlayerReserve(color: PieceColor): MutableList<Piece> {
    return when(color) {
        PieceColor.Blue -> blueReserve
        else -> redReserve
    }
}

fun hasTwoTypesInPool(color: PieceColor): Boolean {
    val pool = getPlayerPool(color)
    var hasPo = false
    var hasBo = false
    for (piece in pool) {
        if(piece.type == PieceType.Po)
            hasPo = true
        else
            hasBo = true
    }
    return hasPo && hasBo
}

//private val INITIAL_PIECES = listOf(
//    listOf("BP", "BP", "BP", "BP", "BP", "BP", "BP", "BP", ).map { Piece.pieceOrNullFromString(it) },
//    listOf("RP", "RP", "RP", "RP", "RP", "RP", "RP", "RP", ).map { Piece.pieceOrNullFromString(it) }
//)

private val INITIAL_PIECES = listOf(
    bluePool,
    redPool
)
val STARTING_PIECES = INITIAL_PIECES.flatten()

data class Board(val pieces: List<List<Piece?>> = INITIAL_BOARD) {
    companion object {
        private val ALL_POSITIONS = (0 until 6).flatMap { y ->
            (0 until 6).map { x -> Position(x, y) }
        }

        fun fromHistory(history: List<Move>): Board {
            var board = Board()
            history.forEach {
                board = board.playAt(it)
            }

            return board
        }
    }

    val allPositions = ALL_POSITIONS
    val allPieces: List<Pair<Position, Piece>> =
        allPositions.mapNotNull { position -> pieces[position.y][position.x]?.let { position to it } }

    fun getAllEmptyPositions(): List<Position> {
        val allEmptyPositions = mutableListOf<Position>()
        for (position in allPositions) {
            if(pieces[position.y][position.x] == null)
                allEmptyPositions.add(position)
        }
        return allEmptyPositions.toList()
    }

    fun pieceAt(position: Position): Piece? {
        if(!isPositionOnTheBoard(position)) return null
        return pieces.getOrNull(position.y)?.getOrNull(position.x)
    }

    // remove a piece from its position, but not necessary from the board
    fun removePieceFrom(from: Position): Board {
        var newPieces = pieces.map { it.toMutableList() }.toMutableList()
        newPieces[from.y][from.x] = null
        return Board(newPieces.map { it.toList() }.toList())
    }

    // place a piece at a position, but not necessary a new piece from pool
    fun placePieceAt(piece: Piece, at: Position): Board {
        if(!isPositionOnTheBoard(at)) return this
        var newPieces = pieces.map { it.toMutableList() }.toMutableList()
        newPieces[at.y][at.x] = piece
        return Board(newPieces.map { it.toList() }.toList())
    }

    // place a piece at a position, but not necessary a new piece from pool
    fun placePieceAt(move: Move): Board {
        return placePieceAt(move.piece, move.to)
    }

    // push a piece on the board (or outside the board)
    // do nothing if 'from' is invalid
    fun moveFromTo(from: Position, to: Position): Board {
        if(!isPositionOnTheBoard(to)) {
            return removePieceFromBoard(from)
        }
        else {
            val piece = pieceAt(from) ?: return this
            var newPieces = pieces.map { it.toMutableList() }.toMutableList()
            newPieces[from.y][from.x] = null
            newPieces[to.y][to.x] = piece
            return Board(newPieces.map { it.toList() }.toList())
        }
    }

    fun removePieceFromBoard(position: Position): Board {
        val piece = (pieceAt(position) ?: return this).also {
            putInPool(it)
        }
        return removePieceFrom(position)
    }

    fun playAt(piece: Piece, at: Position): Board {
        takeFromPool(piece)
        return placePieceAt(piece, at)
    }

    fun playAt(move: Move): Board = playAt(move.piece, move.to)

    fun takeFromPool(piece: Piece) {
        val pool = getPlayerPool(piece.color)
        if (piece.type == PieceType.Po)
            pool.removeLast()
        else
            pool.removeFirst()
    }

    fun putInPool(piece: Piece) {
        val pool = getPlayerPool(piece.color)
        if (piece.type == PieceType.Po)
            pool.add(piece)
        else
            pool.add(0, piece)
    }

    fun isPoolEmpty(player: PieceColor): Boolean = getPlayerPool(player).isEmpty()

    fun hasPieceInPool(color: PieceColor, type: PieceType): Boolean
            = getPlayerPool(color).find { it -> it.type == type } != null

    fun hasPieceInPool(piece: Piece): Boolean = hasPieceInPool(piece.color, piece.type)

    fun hasPieceInReserve(color: PieceColor, type: PieceType): Boolean
            = getPlayerReserve(color).find { it -> it.type == type } != null

    fun hasPieceInReserve(piece: Piece): Boolean = hasPieceInReserve(piece.color, piece.type)

//    fun removeLine(position: Position, direction: Direction, player: PieceColor): Board {
//        var countPo = 0
//        val currentPiece = pieceAt(position)
//        val nextPosition = getPositionTowards(position, direction)
//        val nextPiece = pieceAt(nextPosition)
//        val nextNextPosition = getPositionTowards(nextPosition, direction)
//        val nextNextPiece = pieceAt(nextNextPosition)
//
//        //if(currentPiece != null && currentPiece.type == PieceType.Po)
//        if(currentPiece?.type == PieceType.Po) countPo++
//        if(nextPiece?.type == PieceType.Po) countPo++
//        if(nextNextPiece?.type == PieceType.Po) countPo++
//
//        var newBoard = removePieceFromBoard(position)
//        newBoard = newBoard.removePieceFromBoard(nextPosition)
//        newBoard = newBoard.removePieceFromBoard(nextNextPosition)
//        graduatePieces(countPo, player)
//        return newBoard
//    }

    fun graduatePieces(number: Int, color: PieceColor) {
        (0 until number).map { i ->
            if (color == PieceColor.Blue) {
                bluePool.add(0, Piece.pieceFromString("BB") )
                bluePool.removeLast()

                blueReserve.add(Piece.pieceFromString("BP"))
                blueReserve.removeFirst()
            } else {
                redPool.add(0, Piece.pieceFromString("RB") )
                redPool.removeLast()

                redReserve.add(Piece.pieceFromString("RP"))
                redReserve.removeFirst()
            }
        }
    }
}
