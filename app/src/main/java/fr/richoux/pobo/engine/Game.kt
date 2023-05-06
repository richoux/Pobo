package fr.richoux.pobo.engine

import java.util.*

private const val TAG = "pobotag Game"

data class Move(val piece: Piece, val to: Position) {
    override fun toString(): String {
        return "$piece at $to"
    }
}
data class History(val board: Board, val player: PieceColor) {}

enum class GameState {
    INIT, PLAY, SELECTPIECE, SELECTPOSITION, CHECKGRADUATION, AUTOGRADUATION, SELECTGRADUATION, END
}

enum class Direction {
    TOPLEFT, TOP, TOPRIGHT, RIGHT, BOTTOMRIGHT, BOTTOM, BOTTOMLEFT, LEFT
}

val scanDirection = EnumSet.range(Direction.RIGHT, Direction.BOTTOMLEFT)

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
    var gameState: GameState = GameState.INIT,
    var currentPlayer: PieceColor = PieceColor.Blue,
    var victory: Boolean = false,
    val isPlayout: Boolean = false, // true if the game is a simulation for decision-making
) {
    val displayGameState: String
        get() {
            return when (gameState) {
                GameState.INIT -> ""
                GameState.PLAY -> ""
                GameState.SELECTPIECE -> "Select a small or a large piece"
                GameState.SELECTPOSITION -> ""
                GameState.CHECKGRADUATION -> ""
                GameState.AUTOGRADUATION -> ""
                GameState.SELECTGRADUATION -> "Select small pieces to graduate or large pieces to remove"
                GameState.END -> ""
            }
        }

    fun canPlayAt(to: Position): Boolean = board.pieceAt(to) == null

    fun canPlay(move: Move): Boolean = board.hasPieceInPool(move.piece) && canPlayAt(move.to)

    fun canBePushed(board: Board, pusher: PieceType, myPosition: Position, direction: Direction): Boolean {
        // if myPosition is empty, there is nothing to push
        val myPiece = board.pieceAt(myPosition) ?: return false

        // if I am a Bo, and pusher is a Po, I don't move
        if(myPiece.type > pusher) return false

        // if the cell in the pushing direction is not empty, I don't move
        if(board.pieceAt( getPositionTowards(myPosition, direction) ) != null) return false

        return true
    }

    fun doPush(board: Board, move: Move): Board {
        var newBoard = board
        enumValues<Direction>().forEach {
            val victim = getPositionTowards(move.to, it)
            if(canBePushed(newBoard, move.piece.type, victim, it)) {
                val target = getPositionTowards(victim, it)
                newBoard = newBoard.slideFromTo(victim, target)
            }
        }
        return newBoard
    }

    fun changePlayer() {
        currentPlayer = currentPlayer.other()
    }

    fun nextGameState(): GameState {
        if(victory) {
            return GameState.END
        }
        return when(gameState) {
            GameState.INIT -> {
                GameState.SELECTPOSITION
            }
            GameState.PLAY -> {
                if(board.hasTwoTypesInPool(currentPlayer)) {
                    GameState.SELECTPIECE
                }
                else {
                    GameState.SELECTPOSITION
                }
            }
            GameState.SELECTPIECE -> {
                GameState.SELECTPOSITION
            }
            GameState.SELECTPOSITION -> {
                checkVictory()
                if(victory)
                    GameState.END
                else
                    GameState.CHECKGRADUATION
            }
            GameState.CHECKGRADUATION -> {
                if(getGraduations(board).isEmpty()) {
                    GameState.PLAY
                }
                else {
                    if(getGraduations(board).size == 1)
                        GameState.AUTOGRADUATION
                    else
                        GameState.SELECTGRADUATION
                }
            }
            GameState.AUTOGRADUATION -> {
                GameState.PLAY
            }
            GameState.SELECTGRADUATION -> {
                GameState.PLAY
            }
            else -> { //GameState.END, but it should be caught before the when
                GameState.END
            }
        }
    }

    fun getAlignedPositionsInDirection(board: Board, player: PieceColor, position: Position, direction: Direction): List<Position> {
        val alignedPieces: MutableList<Position> = mutableListOf()

        val currentPiece = board.pieceAt(position)
        if(currentPiece?.color == player)
            alignedPieces.add(position)

        val nextPosition = getPositionTowards(position, direction)
        val nextPiece = board.pieceAt(nextPosition)
        if(nextPiece?.color == player)
            alignedPieces.add(nextPosition)

        val nextNextPosition = getPositionTowards(nextPosition, direction)
        val nextNextPiece = board.pieceAt(getPositionTowards(nextPosition, direction))
        if(nextNextPiece?.color == player)
            alignedPieces.add(nextNextPosition)

        return alignedPieces.toList()
    }

    fun isValidAlignedPositions(positions: List<Position>): Boolean = positions.size == 3

    fun countBoInAlignment(board: Board, positions: List<Position>): Int {
        var countBo = 0
        positions.forEach {
            if(board.pieceAt(it)?.type == PieceType.Bo)
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
                if(piece?.color == currentPlayer){
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

    fun promoteOrRemovePieces(toPromote: List<Position>) {
        var newBoard: Board = board
        for( position in toPromote) {
            newBoard = newBoard.removePieceAndPromoteIt( position )
        }
        board = newBoard
    }

    fun checkVictory(board: Board): Boolean {
        // check if current player has 8 Bo on the board
        victory = false
        if(board.isPoolEmpty(currentPlayer) && board.getPlayerNumberBo(currentPlayer) == 8) {
            victory =  true
        }
        else { // check if current player has at least 3 Bo in line
            (0 until 6).map { y ->
                (0 until 6).map { x ->
                    val position = Position(x,y)
                    val piece = board.pieceAt(position)
                    if(piece?.color == currentPlayer && piece.type == PieceType.Bo ) {
                        scanDirection.forEach {
                            val alignment = getAlignedPositionsInDirection(
                                board,
                                currentPlayer,
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

    fun checkVictory(): Boolean = checkVictory(board)

    fun changeWithHistory(history: History) {
        board = history.board
        currentPlayer = history.player
        checkVictory()
    }
}
