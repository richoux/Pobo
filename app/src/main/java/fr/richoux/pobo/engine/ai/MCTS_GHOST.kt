package fr.richoux.pobo.engine.ai

import android.util.Log
import fr.richoux.pobo.engine.*
import java.lang.Math.sqrt
import kotlin.math.ln

private const val PLAYOUT_DEPTH = 10
private const val TAG = "pobotag MCTS"

data class Node(
    val id: Int,
    val game: Game,
    val player: Color,
    val move: Move?,
    var score: Int,
    var visits: Int,
    val isTerminal: Boolean,
    val parentID: Int,
    var childID: MutableList<Int>
    ) {}

class MCTS_GHOST (
    color: Color,
    var lastMove: Move? = null,
    var currentGame: Game = Game(),
    var root: Node = Node(
        0,
        currentGame,
        currentGame.currentPlayer.other(), // because we want the player who played the move of the node
        lastMove,
        0,
        1,
        false,
        0,
        mutableListOf()
    ),
    var currentNode: Node = root,
    val nodes: ArrayList<Node> = arrayListOf(),
    var numberNodes: Int = 1
) : AI(color) {
    companion object {
        init {
            System.loadLibrary("pobo")
        }

        external fun ghost_solver_call(
            grid: ByteArray,
            blue_pool: ByteArray,
            red_pool: ByteArray,
            blue_pool_size: Int,
            red_pool_size: Int,
            blue_turn: Boolean
        ): IntArray
    }

    override fun select_move(game: Game,
                             lastOpponentMove: Move,
                             timeout_in_ms: Long): Move {
        val start = System.currentTimeMillis()
        currentGame = game.copyForPlayout()
        lastMove = lastOpponentMove
        var opponentMoveExistsInTree = false
        var numberPlayouts = 0
        var numberSolverCalls = 0
        var numberSolverFailures = 0

        // First move of the game from the opponent (for the moment, the AI in always playing second)
        if (numberNodes == 1) {
            root = Node(
                0,
                currentGame,
                currentGame.currentPlayer.other(), // because we want the player who played the move of the node
                lastMove,
                0,
                1,
                false,
                0,
                mutableListOf() )
            currentNode = root
            nodes.add(root)
        }
        // Otherwise, the opponent played 'lastOpponentMove', we have to look for it in the tree
        else {
            for (child in currentNode.childID)
                if (nodes[child].move?.isSame(lastMove) == true) {
                    currentNode = nodes[child]
                    opponentMoveExistsInTree = true
                    break
                }

            if (!opponentMoveExistsInTree) {
                tryEachPossibleMove()
                for (child in currentNode.childID)
                    if (nodes[child].move?.isSame(lastMove) == true) {
                        currentNode = nodes[child]
                        break
                    }
            }
        }

        // generate our moves
        tryEachPossibleMove()

        val actionMasking = mutableListOf<Int>()
        for (node in nodes)
            if (node.move?.to?.x == 0
                || node.move?.to?.x == 5
                || node.move?.to?.y == 0
                || node.move?.to?.y == 5
            ) {
                actionMasking.add(node.id)
            }

        while (System.currentTimeMillis() - start < timeout_in_ms) {
            // Select node
            val selectNode = UCT(actionMasking)
            val movesToRemove: MutableList<Move> = mutableListOf()

            for (child in selectNode.childID)
                nodes[child].move?.let { movesToRemove.add(it) }

            // Expand
            //val move = randomPlay( selectNode.game, movesToRemove.toList() )

            Log.d(TAG,"GHOST call\nGrid:")
            var ss = ""
            for( i in 0..35 ) {
                var p = game.board.grid[i].toInt()
                if( p < 0 )
                    p += 10;
                ss += (p.toString() + " ")
                if( (i+1) % 6 == 0 )
                    ss += "\n"
            }
            ss += "\n"
            Log.d(TAG,"$ss")
            ss = ""
            Log.d(TAG,"Blue player pool:")
            for( i in 0..game.board.bluePool.size-1 ) {
                var p = game.board.bluePool[i].toInt()
                ss += (p.toString() + " ")
            }
            Log.d(TAG,"$ss")
            ss = ""
            Log.d(TAG,"Red player pool:")
            for( i in 0..game.board.redPool.size-1 ) {
                var p = game.board.redPool[i].toInt()
                ss += (p.toString() + " ")
            }
            Log.d(TAG,"$ss")
            var blueTurn = game.currentPlayer == Color.Blue
            Log.d(TAG,"Is Blue turn: $blueTurn")

            var move: Move
            val solution = ghost_solver_call(
                game.board.grid,
                game.board.bluePool.toByteArray(),
                game.board.redPool.toByteArray(),
                game.board.bluePool.size,
                game.board.redPool.size,
                game.currentPlayer == Color.Blue
            )

            numberSolverCalls++
            if (solution[0] == 42) {
                move = randomPlay(selectNode.game, movesToRemove.toList())
                numberSolverFailures++
                Log.d(TAG, "Selection: RANDOM move ${move}")
            }
            else {
                val code = when (game.currentPlayer) {
                    Color.Blue -> -solution[0]
                    Color.Red -> solution[0]
                }

                val id = when( code ) {
                    -2 -> "BB"
                    -1 -> "BP"
                    1 -> "RP"
                    else -> "RB"
                }
                val piece = Piece(id, code.toByte())
                val position = Position(solution[2], solution[1])
                move = Move(piece, position)
//                Log.d(TAG, "Selection: solver move ${move}, cost ${solution[3]}")
            }
            val child = createNode(selectNode.game, move, selectNode.id)

            // Playout
            if (!child.isTerminal) {
                child.score = playout(child)
                numberPlayouts++
            }

            // Backpropagate score
            backpropagate(selectNode.id, child.score)
        }

        val potentialChildrenID = mutableListOf<Int>()

        // Visit
        var mostSelected = 0
        var bestScore = 0
        var bestRatio = 0.0
        val coeff = when( currentNode.player ) {
            Color.Blue -> 1 // This is reversed,
            Color.Red -> -1 // because we are considering children's score
        }
        for (childID in currentNode.childID) {
            Log.d( TAG,"Root child ID: ${childID}, ${nodes[childID].move}, visits=${nodes[childID].visits}, score=${nodes[childID].score}" )
            if (coeff * nodes[childID].score > bestScore) {
                bestRatio = (coeff.toDouble() * nodes[childID].score.toDouble()) / nodes[childID].visits
                potentialChildrenID.clear()
                bestScore = coeff * nodes[childID].score
                potentialChildrenID.add(childID)
            } else if (coeff * nodes[childID].score == bestScore) {
                val ratio = (coeff.toDouble() * nodes[childID].score.toDouble()) / nodes[childID].visits
                if (ratio > bestRatio) {
                    potentialChildrenID.clear()
                    bestRatio = ratio
                    potentialChildrenID.add(childID)
                } else if (ratio == bestRatio) {
                    potentialChildrenID.add(childID)
                }
            }
//            if (nodes[childID].visits > mostSelected) {
//                bestRatio = nodes[childID].score.toDouble() / nodes[childID].visits
//                potentialChildrenID.clear()
//                mostSelected = nodes[childID].visits
//                potentialChildrenID.add(childID)
//            } else if (nodes[childID].visits == mostSelected) {
//                val ratio = nodes[childID].score.toDouble() / nodes[childID].visits
//                if (ratio > bestRatio) {
//                    potentialChildrenID.clear()
//                    bestRatio = ratio
//                    potentialChildrenID.add(childID)
//                } else if (ratio == bestRatio) {
//                    potentialChildrenID.add(childID)
//                }
//            }
        }

        val bestChildID = potentialChildrenID.random()
        Log.d(TAG,"Best child ID: ${bestChildID} ${nodes[bestChildID].move}, visits=${mostSelected}, ratio=${bestRatio}, score=${nodes[bestChildID].score}")
        Log.d(TAG, "Tree size: ${nodes.size} nodes, number of playouts: ${numberPlayouts}, solver calls: ${numberSolverCalls}, solver failures: ${numberSolverFailures}")

        currentNode = nodes[bestChildID]
        return currentNode.move!!
    }

    fun UCT(node: Node, actionMasking: MutableList<Int>): Node {
        if (node.childID.size != node.game.board.emptyPositions.size) {
//            Log.d(TAG, "Selection: nodeID=${node.id}, node.childID.size=${node.childID.size}, available moves=${node.game.board.emptyPositions.size}")
            return node
        }
        var bestValue = 0.0
        val potentialNodes = mutableListOf<Node>()

//        Log.d(TAG, "UCT: start scanning children")

        for (nodeID in node.childID) {
//            Log.d(TAG, "UCT: nodes[nodeID].game.moveNumber=${nodes[nodeID].game.moveNumber}, actionMasking.contains(nodeID)=${actionMasking.contains(nodeID)}")
            if (nodes[nodeID].game.moveNumber > 6 || !actionMasking.contains(nodeID)) {
                val newNode = nodes[nodeID]
                val value = UCTValue(newNode, newNode.visits)
                if (value > bestValue) {
                    potentialNodes.clear()
                    bestValue = value
//                    Log.d(TAG, "UCT: better child found")
                    potentialNodes.add(newNode)
                } else if (value == bestValue) {
                    potentialNodes.add(newNode)
//                    Log.d(TAG, "UCT: equivalent child found")
                }
            }
        }

//        Log.d(TAG, "UCT: finish scanning children")

        // Should never happen
        if( potentialNodes.isEmpty() )
            return node

        val bestNode = potentialNodes.random()
        return UCT(bestNode, actionMasking)
    }

    fun UCT(actionMasking: MutableList<Int>): Node = UCT(currentNode, actionMasking)
    //UCT(nodes[0], actionMasking)

    fun UCTValue(node: Node, parentVisits: Int ): Double {
        val coeff = when( node.player ) {
            Color.Blue -> -1
            Color.Red -> 1
        }
        return if (node.visits == 0) {
//            Log.d(TAG, "UCT, node ${node.id} has ${node.visits} visits")
            999999.9
        } else {
//            Log.d(TAG, "UCT, node ${node.id} score=${( (coeff * node.score.toDouble()) / node.visits) + (2.0 / sqrt(2.0)) * sqrt(2 * ln(parentVisits.toDouble()) / node.visits)}")
            ( (coeff * node.score.toDouble()) / node.visits) + (2.0 / sqrt(2.0)) * sqrt(2 * ln(parentVisits.toDouble()) / node.visits)
        }
    }

    fun playout(node: Node): Int {
//        val myColor = node.game.currentPlayer
//    var score = 0
//        val start = System.currentTimeMillis()
//        var numberBlueBo = 0
//        var numberRedBo = 0
        var numberMoves = 0

        val game = node.game.copyForPlayout()
        var isBlueVictory = game.checkVictoryFor(game.board, Color.Blue)
        var isRedVictory = game.checkVictoryFor(game.board, Color.Red)

//            var ss = ""
//            for( i in 0..35 ) {
//                var p = game.board.grid[i].toInt()
//                if( p < 0 )
//                    p += 10;
//                ss += (p.toString() + " ")
//                if( (i+1) % 6 == 0 )
//                    ss += "\n"
//            }
//            ss += "\n"
//            Log.d(TAG,"${ss}")

        while (!isBlueVictory && !isRedVictory){ // && numberMoves < PLAYOUT_DEPTH) {
            val move = randomPlay(game)
            // Move selected by the solver
//            var move: Move
//
//            val solution = ghost_solver_call(
//                game.board.grid,
//                game.board.bluePool.toByteArray(),
//                game.board.redPool.toByteArray(),
//                game.board.bluePool.size,
//                game.board.redPool.size,
//                game.currentPlayer == Color.Blue
//            )
//
//            if (solution[0] == 42) {
//                move = randomPlay(game)
//            }
//            else {
//                val code = when (game.currentPlayer) {
//                    Color.Blue -> -solution[0]
//                    Color.Red -> solution[0]
//                }
//
//                val piece = Piece("", code.toByte())
//                val position = Position(solution[2], solution[1])
//                move = Move(piece, position)
//            }

            val board = game.board.playAt(move)
            game.board = game.doPush(board, move)
            // check if we need to graduate a piece
            if (game.getGraduations().isNotEmpty())
                game.promoteOrRemovePieces( randomGraduation(game) )

//            numberBlueBo = game.board.numberBlueBo
//            numberRedBo = game.board.numberRedBo
            numberMoves++
            game.changePlayer()
            isBlueVictory = game.checkVictoryFor(game.board, Color.Blue)
            isRedVictory = game.checkVictoryFor(game.board, Color.Red)

//                var ss = ""
//                for( i in 0..35 ) {
//                    var p = game.board.grid[i].toInt()
//                    if( p < 0 )
//                        p += 10;
//                    ss += (p.toString() + " ")
//                    if( (i+1) % 6 == 0 )
//                        ss += "\n"
//                }
//                ss += "\n"
//                Log.d(TAG,"${ss}")
        }

        if( isBlueVictory )
            return -1
        else {
            if( isRedVictory )
                return 1
            else
                return 0
        }

//        if (numberBlueBo == numberRedBo)
//            return 0
//        else {
//            if( numberBlueBo > numberRedBo )
//                return -1
//            else
//                return 1
//        }
    }

    fun backpropagate(parentID: Int, score: Int) {
        nodes[parentID].score += score
        nodes[parentID].visits++
//        Log.d( TAG,"Parent ${parentID}: score=${nodes[parentID].score}, visits=${nodes[parentID].visits}" )
        if (parentID != 0) { // if not root
            backpropagate(nodes[parentID].parentID, score)
        }
    }

    fun tryEachPossibleMove() {
        val game = currentNode.game
        val player = game.currentPlayer
        val positions = game.board.emptyPositions
        val pieces: List<Piece>

        if (game.board.hasTwoTypesInPool(player))
            pieces = listOf(getPoInstanceOfColor(player), getBoInstanceOfColor(player))
        else
            pieces = listOf(getPieceInstance(player, game.board.getPlayerPool(player)[0]))

        var childExists: Boolean

        for (piece in pieces)
            for (position in positions) {
                val move = Move(piece, position)
                childExists = false
                for (child in currentNode.childID)
                    if (nodes[child].move?.isSame(move) == true) {
                        childExists = true
                        break
                    }

                if (!childExists)
                    createNode(game, move, currentNode.id)
            }
    }

    fun createNode(game: Game, move: Move, parentID: Int): Node {
        val newGame = game.copyForPlayout()
        val newBoard = newGame.board.playAt(move)
        newGame.board = newGame.doPush(newBoard, move)
        // check if we need to graduate a piece
        if (newGame.getGraduations().isNotEmpty())
            newGame.promoteOrRemovePieces( randomGraduation(newGame) )

        val isBlueVictory = newGame.checkVictoryFor(newGame.board, Color.Blue)
        val isRedVictory = newGame.checkVictoryFor(newGame.board, Color.Red)
        val isTerminal = isBlueVictory || isRedVictory

        val score =
            if (isBlueVictory)
                -1
            else {
                if (isRedVictory)
                    1
                else
                    0
            }

        newGame.changePlayer()

        val child = Node(
            numberNodes,
            newGame,
            newGame.currentPlayer.other(), // because we want the player who played the move of the node
            move,
            score,
            0,
            isTerminal,
            parentID,
            mutableListOf()
        )

        nodes[parentID].childID.add(numberNodes)
        nodes.add(child)
        numberNodes++

        return child
    }
}