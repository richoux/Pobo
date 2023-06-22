package fr.richoux.pobo.engine.ai

import android.util.Log
import fr.richoux.pobo.engine.*
import java.lang.Math.sqrt
import kotlin.math.ln

private const val PLAYOUTS = 1
private const val TIMEOUT_PLAYOUT = 30 //ms
private const val PLAYOUT_DEPTH = 30
private const val TAG = "pobotag MCTS"

data class Node(
    val id: Int,
    val game: Game,
    val move: Move?,
    var score: Int,
    var visits: Int,
    val isTerminal: Boolean,
    val parentID: Int,
    var childID: MutableList<Int>
    ) {}

data class MCTS(
    var lastMove: Move? = null,
    var currentGame: Game = Game(),
    var root: Node = Node(0, currentGame, lastMove, 0, 1, false, 0, mutableListOf()),
    var currentNode: Node = root,
    val nodes: ArrayList<Node> = arrayListOf(),
    var numberNodes: Int = 1
) {
    companion object {
        init {
            System.loadLibrary("pobo")
        }

        external fun ghost_solver_call( grid: ByteArray,
                                        blue_pool: ByteArray,
                                        red_pool: ByteArray,
                                        blue_pool_size: Int,
                                        red_pool_size: Int,
                                        blue_turn: Boolean ): IntArray
    }

    fun run( game: Game, lastOpponentMove: Move, timeout_in_ms: Long ): Move {
        val start = System.currentTimeMillis()
        currentGame = game.copyForPlayout()
        lastMove = lastOpponentMove
        var opponentMoveExistsInTree = false

        // First move of the game from the opponent (for the moment, the AI in always playing second)
        if( numberNodes == 1 ) {
            root = Node(0, currentGame, lastMove, 0, 1, false, 0, mutableListOf())
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

        while( System.currentTimeMillis() - start < timeout_in_ms ) {
            // Select node
            val selectNode = UCT(actionMasking)
            val movesToRemove: MutableList<Move> = mutableListOf()
            for (child in selectNode.childID)
                nodes[child].move?.let { movesToRemove.add(it) }
//        Log.d(TAG,"Selected node: ${currentNode.id} with move ${currentNode.move}")

            // Expand
            val move = randomPlay( selectNode.game, movesToRemove.toList() )
            val child = createNode( selectNode.game, move, selectNode.id )

//        Log.d(TAG,"Expansion: node ${numberNodes} with move ${move}")

            // Playout
            if (!child.isTerminal) {
                child.score = 1 - playout(child)
//            Log.d(TAG,"Score of the expanded node: ${child.score}")
            }
//        else {
//            Log.d(TAG,"Expansion node ${numberNodes} is a terminal state")
//        }

            // Backpropagate score
            backpropagate( selectNode.id, 1 - child.score )
        }

        val potentialChildrenID = mutableListOf<Int>()

        // Visit
        var mostSelected = 0
        var bestRatio = 0.0
        for (childID in currentNode.childID) {
//            Log.d(TAG,"Root child ID: ${childID}, ${nodes[childID].move}, visits=${nodes[childID].visits}, score=${nodes[childID].score}")
            if (nodes[childID].visits > mostSelected) {
                bestRatio = nodes[childID].score.toDouble() / nodes[childID].visits
                potentialChildrenID.clear()
                mostSelected = nodes[childID].visits
                potentialChildrenID.add(childID)
            } else if (nodes[childID].visits == mostSelected) {
                val ratio = nodes[childID].score.toDouble() / nodes[childID].visits
                if (ratio > bestRatio) {
                    potentialChildrenID.clear()
                    bestRatio = ratio
                    potentialChildrenID.add(childID)
                } else if (ratio == bestRatio) {
                    potentialChildrenID.add(childID)
                }
            }
        }

        val bestChildID = potentialChildrenID.random()
        Log.d(TAG,"Best child ID: ${bestChildID}, visits=${mostSelected}, ratio=${bestRatio}, score=${nodes[bestChildID].score}")
        Log.d(TAG,"Tree size: ${nodes.size} nodes")

        currentNode = nodes[bestChildID]
        return currentNode.move!!
    }

    fun UCT(node: Node, actionMasking: MutableList<Int>): Node {
        if (node.childID.isEmpty())
            return node

        var bestValue = 0.0
//    var bestNode = currentNode
        val potentialNodes = mutableListOf<Node>()

        for (nodeID in node.childID) {
            if (nodes[nodeID].game.moveNumber > 6 || !actionMasking.contains(nodeID)) {
                val newNode = nodes[nodeID]
                val value = UCTValue(newNode, newNode.visits)
                if (value > bestValue) {
                    potentialNodes.clear()
                    bestValue = value
//            bestNode = newNode
                    potentialNodes.add(newNode)
                } else if (value == bestValue) {
                    potentialNodes.add(newNode)
                }
            }
        }

        val bestNode = potentialNodes.random()
        return UCT(bestNode, actionMasking)
    }

    fun UCT(actionMasking: MutableList<Int>): Node =
        UCT(currentNode, actionMasking)
        //UCT(nodes[0], actionMasking)

    fun UCTValue(node: Node, parentVisits: Int): Double {
        return if (node.visits == 0) {
//        Log.d(TAG,"UCT, node ${node.id} has ${node.visits} visits")
            999999.9
        } else {
            (node.score.toDouble() / node.visits) + 2 * sqrt(2.0) * sqrt(2 * ln(parentVisits.toDouble()) / node.visits)
        }
    }

    fun playout(node: Node): Int {
        val myColor = node.game.currentPlayer
//    var score = 0
        val start = System.currentTimeMillis()
        var numberBlueBo = 0
        var numberRedBo = 0
        var numberMoves = 0

//    while( System.currentTimeMillis() - start < TIMEOUT_PLAYOUT ) {
        for (i in 1..PLAYOUTS) {
            val game = node.game.copyForPlayout()
            var isBlueVictory = game.checkVictoryFor(game.board, Color.Blue)
            var isRedVictory = game.checkVictoryFor(game.board, Color.Red)
            while (!isBlueVictory && !isRedVictory && numberMoves < PLAYOUT_DEPTH) {
                //val move = randomPlay(game)
//                val bluePoolArray = ByteArray(8){ 0 }
//                val redPoolArray = ByteArray(8){ 0 }
//
//                for( i in 0..game.board.bluePool.size - 1 )
//                    bluePoolArray[i] = game.board.bluePool.get(i)
//
//                for( i in 0..game.board.redPool.size - 1 )
//                    redPoolArray[i] = game.board.redPool.get(i)

                val solution = ghost_solver_call(
                    game.board.grid,
//                    bluePoolArray,
//                    redPoolArray,
                    game.board.bluePool.toByteArray(),
                    game.board.redPool.toByteArray(),
                    game.board.bluePool.size,
                    game.board.redPool.size,
                    myColor == Color.Blue
                )
                val code = when (myColor) {
                    Color.Blue -> -solution[0]
                    Color.Red -> solution[0]
                }

                val piece = Piece("", code.toByte())
                val position = Position(solution[1], solution[2])
                val move = Move(piece, position)
                val board = game.board.playAt(move)
                game.board = game.doPush(board, move)
                // check if we need to graduate a piece
                if (game.getGraduations().isNotEmpty())
                    game.promoteOrRemovePieces(randomGraduation(game))

                numberBlueBo = game.board.numberBlueBo
                numberRedBo = game.board.numberRedBo
                numberMoves++
                game.changePlayer()
                isBlueVictory = game.checkVictoryFor(game.board, Color.Blue)
                isRedVictory = game.checkVictoryFor(game.board, Color.Red)
            }

            if ((isBlueVictory && myColor == Color.Blue) || (isRedVictory && myColor == Color.Red)) {
//                score += 1
                return 1 // incompatible with PLAYOUTS > 1
//            Log.d(TAG,"My color ${myColor} wins!")
            }
//        if( (isBlueVictory && myColor == PieceColor.Red) || (isRedVictory && myColor == PieceColor.Blue))
//            score -= 1
        }
//    }

        if ((myColor == Color.Blue && numberBlueBo > numberRedBo)
            || (myColor == Color.Red && numberBlueBo < numberRedBo)
        )
            return 1
        else
            return 0
//    return score
    }

    fun backpropagate(parentID: Int, score: Int) {
        nodes[parentID].score += score
        nodes[parentID].visits++
//    Log.d(TAG,"Parent ${parentID}: score=${nodes[parentID].score}, visits=${nodes[parentID].visits}")
        if (parentID != 0) { // if not root
            backpropagate( nodes[parentID].parentID, 1 - score )
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
            pieces = listOf( getPieceInstance(player, game.board.getPlayerPool(player)[0]) )

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

                if( !childExists ) {
                    createNode(game, move, currentNode.id)
                }
            }
    }

    fun createNode(game: Game, move: Move, parentID: Int ): Node {
        val newGame = game.copyForPlayout()
        val newBoard = newGame.board.playAt(move)
        newGame.board = newGame.doPush(newBoard, move)
        // check if we need to graduate a piece
        if (newGame.getGraduations().isNotEmpty())
            newGame.promoteOrRemovePieces(randomGraduation(newGame))

        val isBlueVictory = newGame.checkVictoryFor(newBoard, Color.Blue)
        val isRedVictory = newGame.checkVictoryFor(newBoard, Color.Red)
        val isTerminal = isBlueVictory || isRedVictory

        val score =
            if (isBlueVictory) {
                when (newGame.currentPlayer) {
                    Color.Blue -> 1
                    Color.Red -> 0 //-1
                }
            } else if (isRedVictory) {
                when (newGame.currentPlayer) {
                    Color.Red -> 1
                    Color.Blue -> 0 //-1
                }
            } else {
                0
            }

        newGame.changePlayer()

        val child = Node(
            numberNodes,
            newGame,
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