package fr.richoux.pobo.engine

import android.util.Log
import java.util.*

private const val TAG = "pobotag Game"

data class Move(val piece: Piece, val to: Position) {
    override fun toString(): String {
        return "$piece at $to"
    }
}
data class History(val board: Board, val gameState: GameState, val player: PieceColor) {}

enum class GameState {
    INIT, PLAY, SELECTPIECE, SELECTPOSITION, CHECKGRADUATION, SELECTREMOVAL, END
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
            val turnString = if (this.currentPlayer == PieceColor.Blue) "Blue Player's Turn" else "Red Player's Turn"
            return when (gameState) {
                GameState.INIT -> "Initialization, $turnString"
                GameState.PLAY -> "Play, $turnString"
                GameState.SELECTPIECE -> "Select piece, $turnString"
                GameState.SELECTPOSITION -> "Select position, $turnString"
                GameState.CHECKGRADUATION -> "Check graduation, $turnString"
                GameState.SELECTREMOVAL -> "Select pieces to remove, $turnString"
                GameState.END -> "End - " + if (this.currentPlayer == PieceColor.Blue) "Red Player Wins" else "Blue Player Wins"
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
        Log.d(TAG, "Game.doPush call, player ${currentPlayer}")
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

    fun play(move: Move) {
        Log.d(TAG, "Game.play call, player ${currentPlayer}, move is ${move}")
        if(!canPlay(move)) {
            Log.d(TAG, "Game.play: player ${currentPlayer}, can't play ${move}")
            return
        }

        Log.d(TAG, "Game.play, player ${currentPlayer}, play move on board")
        var newBoard = board.playAt(move)
        Log.d(TAG, "Game.play, player ${currentPlayer}, compute push")
        newBoard = doPush(newBoard, move)

        checkVictory(newBoard)
        if(!victory) {
            Log.d(TAG, "Game.play, player ${currentPlayer}, not a victory yet")
            val graduable = getGraduations(newBoard)
            if( !graduable.isEmpty() ) {
                Log.d(TAG, "Game.play, player ${currentPlayer}, there is promotion in the air!")
                Log.d(TAG, "Game.play, player ${currentPlayer}, graduable.size=${graduable.size}")
                if(graduable.size == 1 ) {
                    graduable[0].forEach {
                        newBoard = newBoard.removePieceAndPromoteIt(it)
                    }
                }
                else {
                    // ???
                    // need to ask the user to select one of these groups
                }
            }
        }

        board = newBoard
        gameState = nextGameState()
    }

    fun changePlayer() {
        Log.d(TAG, "Game.changePlayer call, player ${currentPlayer}")
        currentPlayer = currentPlayer.other()
    }

    fun nextGameState(): GameState {
        Log.d(TAG, "Game.nextGameState call, player ${currentPlayer}")
        if(victory) {
            Log.d(TAG, "Game.nextGameState: player ${currentPlayer}, victory -> GameState.END")
            return GameState.END
        }
        return when(gameState) {
            GameState.INIT -> {
                Log.d(TAG, "Game.nextGameState: player ${currentPlayer}, GameState.INIT -> GameState.SELECTPOSITION")
                GameState.SELECTPOSITION
            }
            GameState.PLAY -> {
                if(board.hasTwoTypesInPool(this.currentPlayer)) {
                    Log.d(TAG, "Game.nextGameState: player ${currentPlayer}, GameState.PLAY -> GameState.SELECTPIECE")
                    GameState.SELECTPIECE
                }
                else {
                    Log.d(TAG, "Game.nextGameState: player ${currentPlayer}, GameState.PLAY -> GameState.SELECTPOSITION")
                    GameState.SELECTPOSITION
                }
            }
            GameState.SELECTPIECE -> {
                Log.d(TAG, "Game.nextGameState: player ${currentPlayer}, GameState.SELECTPIECE -> GameState.SELECTPOSITION")
                GameState.SELECTPOSITION
            }
            GameState.SELECTPOSITION -> {
                Log.d(TAG, "Game.nextGameState: player ${currentPlayer}, GameState.SELECTPOSITION -> GameState.CHECKGRADUATION")
                GameState.CHECKGRADUATION
            }
            GameState.CHECKGRADUATION -> {
                if(getGraduations(board).size <= 1) {
                    Log.d(TAG, "Game.nextGameState: player ${currentPlayer}, GameState.CHECKGRADUATION -> GameState.PLAY")
                    GameState.PLAY
                }
                else {
                    Log.d(TAG, "Game.nextGameState: player ${currentPlayer}, GameState.CHECKGRADUATION -> GameState.SELECTREMOVAL")
                    GameState.SELECTREMOVAL
                }
            }
            GameState.SELECTREMOVAL -> {
                Log.d(TAG, "Game.nextGameState: player ${currentPlayer}, GameState.SELECTREMOVAL -> GameState.PLAY")
                GameState.PLAY
            }
            else -> { //GameState.END, but it should be caught before the when
                Log.d(TAG, "Game.nextGameState: player ${currentPlayer}, GameState.END -> GameState.END")
                GameState.END
            }
        }
    }

    fun getAlignedPositionsInDirection(board: Board, player: PieceColor, position: Position, direction: Direction): List<Position> {
        Log.d(TAG, "Game.getAlignedPositionsInDirection call, player ${currentPlayer}")
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
        Log.d(TAG, "Game.countBoInAlignment call, player ${currentPlayer}")
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
        Log.d(TAG, "Game.getGraduations call, player ${currentPlayer}")
        val graduable: MutableList<List<Position>> = mutableListOf()
        val hasAllPiecesOnTheBoard = board.isPoolEmpty(this.currentPlayer)

        (0 until 6).map { y ->
            (0 until 6).map { x ->
                val position = Position(x,y)
                val piece = board.pieceAt(position)
                if(piece?.color == this.currentPlayer){
                    // check if we have 8 pieces on the board
                    if(hasAllPiecesOnTheBoard)
                        graduable.add(listOf(position))

                    // check 3-in-a-row
                    scanDirection.forEach {
                        val alignment = getAlignedPositionsInDirection(board,
                            this.currentPlayer, position, it)
                        if(isValidAlignedPositions(alignment))
                           graduable.add(alignment)
                    }
                }
            }
        }
        return graduable.toList()
    }

    fun promoteOrRemovePieces(toPromote: List<Position>) {
        Log.d(TAG, "Game.promoteOrRemovePieces call, player ${currentPlayer}")
        var newBoard: Board = board
        for( position in toPromote) {
            newBoard = newBoard.removePieceAndPromoteIt( position )
        }
        board = newBoard
    }

    fun checkVictory(board: Board): Boolean {
        Log.d(TAG, "Game.checkVictory call, player ${currentPlayer}")
        // check if current player has 8 Bo on the board
        victory = false
        if(board.isPoolEmpty(this.currentPlayer) && board.getPlayerNumberBo(this.currentPlayer) == 8) {
            victory =  true
        }
        else { // check if current player has at least 3 Bo in line
            (0 until 6).map { y ->
                (0 until 6).map { x ->
                    val position = Position(x,y)
                    val piece = board.pieceAt(position)
                    if(piece?.color == this.currentPlayer && piece.type == PieceType.Bo ) {
                        scanDirection.forEach {
                            val alignment = getAlignedPositionsInDirection(board,
                                this.currentPlayer, position, it)
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
        Log.d(TAG, "Game.changeWithHistory call, player ${currentPlayer}")
        board = history.board
        gameState = history.gameState
        currentPlayer = history.player
        checkVictory()
    }
}
