package fr.richoux.pobo.engine

import java.util.*

data class Move(val piece: Piece, val to: Position) {
}

val fakeMove: Move = Move( Piece.pieceFromString("BP"), Position(-1,-1))

enum class GameState {
    INIT, PLAY, SELECTPIECE, SELECTPOSITION, CHECKGRADUATION, SELECTREMOVAL, END
}

sealed class GameLoopStep {
    data class Init(val game: Game) : GameLoopStep()
    data class Play(val game: Game) : GameLoopStep()
    data class SelectPiece(val game: Game, val onSelectPiece: (PieceType) -> GameLoopStep
                = { _ -> GameLoopStep.SelectPosition(Game()) }) : GameLoopStep()
    data class SelectPosition(val game: Game, val onSelectPosition: (Position) -> GameLoopStep
                = { _ -> GameLoopStep.CheckGraduationCriteria(Game(), listOf()) }) : GameLoopStep()
    data class CheckGraduationCriteria(val game: Game, val positions: List<List<Position>>): GameLoopStep()
    data class SelectPiecesToRemove(val game: Game, val onSelectPiece: (PieceType) -> GameLoopStep
                = { _ -> GameLoopStep.Play(Game()) }) : GameLoopStep()
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
    val board: Board = Board(),
    //val history: List<Move> = listOf(),
    val gameState: GameState = GameState.INIT,
    val isPlayout: Boolean = false, // true if the game is a simulation for decision-making
    val victory: Boolean = false
) {
    val displayGameState: String
        get() {
            val turnString = if (turn == PieceColor.Blue) "Blue Player's Turn" else "Red Player's Turn"
            return when (gameState) {
                GameState.INIT -> "Initialization, $turnString"
                GameState.PLAY -> "Play, $turnString"
                GameState.SELECTPIECE -> "Select piece, $turnString"
                GameState.SELECTPOSITION -> "Select position, $turnString"
                GameState.CHECKGRADUATION -> "Check graduation, $turnString"
                GameState.SELECTREMOVAL -> "Select pieces to remove, $turnString"
                GameState.END -> "End - " + if (turn == PieceColor.Blue) "Red Player Wins" else "Blue Player Wins"
            }
        }

    val turn: PieceColor
        get() = history.lastOrNull()?.let { board.pieceAt(it.to)?.color?.other() } ?: PieceColor.Blue

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

    fun play(move: Move): Game {
        if(!canPlay(move)) return this

        var newBoard = board.playAt(move)
        newBoard = doPush(newBoard.playAt(move), move)

        val victory = checkVictory(newBoard)
        if(!victory) {
            val graduable = getGraduations(newBoard)
            if( !graduable.isEmpty() ) {
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
        return Game(newBoard, history + listOf(move), nextGameState(newBoard), victory)
    }

    fun nextGameState(board: Board): GameState {
        if(victory)
            return GameState.END

        return when(gameState) {
            GameState.INIT -> GameState.PLAY
            GameState.PLAY -> {
                if(board.hasTwoTypesInPool(turn))
                    GameState.SELECTPIECE
                else
                    GameState.SELECTPOSITION
            }
            GameState.SELECTPIECE -> GameState.SELECTPOSITION
            GameState.SELECTPOSITION -> GameState.CHECKGRADUATION
            GameState.CHECKGRADUATION -> {
                if(getGraduations(board).size < 2)
                    GameState.PLAY
                else
                    GameState.SELECTREMOVAL
            }
            GameState.SELECTREMOVAL -> GameState.PLAY
            else -> { //GameState.END, but it should be caught before the when
                GameState.END
            }
        }
    }

    fun nextGameLoopStep(move: Move): GameLoopStep {
//        when(gameState) {
//            GameState.INIT ->
//            GameState.PLAY ->
//            GameState.SELECTPIECE ->
//            GameState.SELECTPOSITION ->
//            GameState.CHECKGRADUATION ->
//            GameState.SELECTREMOVAL ->
//            else -> { //GameState.END
//
//            }
//        }
//
//        val nextStep = when(loopStep) {
//            GameLoopStep.Init() -> GameLoopStep.Play(this)
//                else -> GameLoopStep.Play(this)
//        }

        val oldGame = this.copy()
        val newGame = play(move)



        if(!canPlay(move))
            return when {
                board.hasTwoTypesInPool(turn) -> GameLoopStep.SelectPiece(this) {
                    GameLoopStep.SelectPosition(
                        this
                    )
                }
                else -> GameLoopStep.SelectPosition(this) {
                    GameLoopStep.Play(
                        this
                    )
                }
            }

//        if(!newGame.getGraduations(board).isEmpty()) {
//            return MoveResult.Promotion { promoteTo ->
//                MoveResult.Success(newGame.promotePieceAt(to, promoteTo))
//            }
//        }

        return GameLoopStep.Play(newGame)
    }

    fun nextGameLoopStep(piece: Piece, position: Position): GameLoopStep = nextGameLoopStep(Move(piece, position))

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
        val hasAllPiecesOnTheBoard = board.isPoolEmpty(turn)

        (0 until 6).map { y ->
            (0 until 6).map { x ->
                val position = Position(x,y)
                val piece = board.pieceAt(position)
                if(piece?.color == turn){
                    // check for Po if we have 8 pieces on the board
                    if(hasAllPiecesOnTheBoard && piece.type == PieceType.Po)
                        graduable.add(listOf(position))

                    // check 3-in-a-row
                    scanDirection.forEach {
                        val alignment = getAlignedPositionsInDirection(board, turn, position, it)
                        if(isValidAlignedPositions(alignment))
                           graduable.add(alignment)
                    }
                }
            }
        }
        return graduable.toList()
    }

    fun checkVictory(board: Board): Boolean {
        // check if current player has 8 Bo on the board
        if(board.isPoolEmpty(turn) && board.getPlayerNumberBo(turn) == 8)
            return true
        else { // check if current player has at least 3 Bo in line
            (0 until 6).map { y ->
                (0 until 6).map { x ->
                    val position = Position(x,y)
                    val piece = board.pieceAt(position)
                    if(piece?.color == turn && piece.type == PieceType.Bo ) {
                        scanDirection.forEach {
                            val alignment = getAlignedPositionsInDirection(board, turn, position, it)
                            if(isValidAlignedPositions(alignment) && containsBoOnly(board, alignment))
                                return true
                        }
                    }
                }
            }
        }
        return false
    }
}
