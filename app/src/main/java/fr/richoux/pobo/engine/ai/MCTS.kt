package fr.richoux.pobo.engine.ai

import android.util.Log
import fr.richoux.pobo.engine.*
import java.lang.Math.sqrt
import kotlin.math.ln

private const val PLAYOUTS = 1
private const val TIMEOUT_PLAYOUT = 30 //ms
private const val TAG = "pobotag MCTS"

data class Node(
    val id: Int,
    val game: Game,
    val move: Move,
    var score: Int,
    var visits: Int,
    val isTerminal: Boolean,
    val parentID: Int,
    var childID: MutableList<Int>
    ) {}

fun MCTS( game: Game, lastMove: Move, timeout_in_ms: Long ): Move {
    val start = System.currentTimeMillis()
    val copyGame = game.copyForPlayout()

    val root = Node( 0, copyGame, lastMove,0, 1, false, 0, mutableListOf() )
    val nodes: ArrayList<Node> = arrayListOf( root )

    var currentNode: Node
    var numberNodes = tryEachPossibleMove( nodes )
    val actionMasking = mutableListOf<Int>()
    for (node in nodes)
        if (node.move.to.x == 0
            || node.move.to.x == 5
            || node.move.to.y == 0
            || node.move.to.y == 5
        ) {
            actionMasking.add(node.id)
        }

    while( System.currentTimeMillis() - start < timeout_in_ms )
    {
        // Select node
        currentNode = UCT( nodes, actionMasking )
        val movesToRemove: MutableList<Move> = mutableListOf()
        for( child in currentNode.childID )
            movesToRemove.add( nodes[child].move )
//        Log.d(TAG,"Selected node: ${currentNode.id} with move ${currentNode.move}")

        // Expand
        val newGame = currentNode.game
        val move = randomPlay( newGame, movesToRemove.toList() )
        val newBoard = newGame.board.playAt(move)
        newGame.board = newGame.doPush(newBoard, move)
        // check if we need to graduate a piece
        if( newGame.getGraduations().isNotEmpty() )
            newGame.promoteOrRemovePieces( randomGraduation(newGame) )

        val isBlueVictory = newGame.checkVictoryFor(newBoard, PieceColor.Blue)
        val isRedVictory = newGame.checkVictoryFor(newBoard, PieceColor.Red)
        val isTerminal = isBlueVictory || isRedVictory

        val score =
            if( isBlueVictory ) {
                when( newGame.currentPlayer ) {
                    PieceColor.Blue -> 1
                    PieceColor.Red -> 0 //-1
                }
            } else if( isRedVictory ) {
                when( newGame.currentPlayer ) {
                    PieceColor.Red -> 1
                    PieceColor.Blue -> 0 //-1
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
            currentNode.id,
            mutableListOf()
        )

//        Log.d(TAG,"Expansion: node ${numberNodes} with move ${move}")
        currentNode.childID.add(numberNodes)
        nodes.add( child )
        numberNodes++

        // Playout
        if( !isTerminal ) {
            child.score = 1 - playout( child )
//            Log.d(TAG,"Score of the expanded node: ${child.score}")
        }
//        else {
//            Log.d(TAG,"Expansion node ${numberNodes} is a terminal state")
//        }

        // Backpropagate score
        backpropagate( nodes, child.parentID, 1 - child.score )
    }

    val potentialChildrenID = mutableListOf<Int>()

    // Visit
    var mostSelected = 0
    var bestRatio = 0.0
    for( childID in root.childID ) {
        Log.d(TAG,"Root child ID: ${childID}, ${nodes[childID].move}, visits=${nodes[childID].visits}, score=${nodes[childID].score}")
        if( nodes[childID].visits > mostSelected ) {
            bestRatio = nodes[childID].score.toDouble() / nodes[childID].visits
            potentialChildrenID.clear()
            mostSelected = nodes[childID].visits
            potentialChildrenID.add(childID)
        } else if (nodes[childID].visits == mostSelected) {
            val ratio = nodes[childID].score.toDouble() / nodes[childID].visits
            if( ratio > bestRatio ) {
                potentialChildrenID.clear()
                bestRatio = ratio
                potentialChildrenID.add(childID)
            }
            else if(ratio == bestRatio) {
                potentialChildrenID.add(childID)
            }
        }
    }

    val bestChildID = potentialChildrenID.random()
    Log.d(TAG,"Best child ID: ${bestChildID}, visits=${mostSelected}, ratio=${bestRatio}, score=${nodes[bestChildID].score}")
    return nodes[bestChildID].move
}

fun UCT( currentNode: Node, nodes: ArrayList<Node>, actionMasking: MutableList<Int> ): Node {
    if( currentNode.childID.isEmpty() )
        return currentNode

    var bestValue = 0.0
//    var bestNode = currentNode
    val potentialNodes = mutableListOf<Node>()

    for( nodeID in currentNode.childID ) {
        if( nodes[nodeID].game.moveNumber > 6 || !actionMasking.contains( nodeID ) ) {
            val node = nodes[nodeID]
            val value = UCTValue( node, currentNode.visits )
            if( value > bestValue ) {
                potentialNodes.clear()
                bestValue = value
//            bestNode = node
                potentialNodes.add( node )
            } else if( value == bestValue ) {
                potentialNodes.add( node )
            }
        }
    }

    val bestNode = potentialNodes.random()
    return UCT( bestNode, nodes, actionMasking )
}

fun UCT( nodes: ArrayList<Node>, actionMasking: MutableList<Int> ): Node = UCT( nodes[0], nodes, actionMasking )

fun UCTValue( node: Node, parentVisits: Int ): Double {
    if( node.visits == 0 ) {
//        Log.d(TAG,"UCT, node ${node.id} has ${node.visits} visits")
        return 999999.9
    }
    else {
        val pouet = (node.score.toDouble() / node.visits) + 2 * sqrt(2.0) * sqrt(2 * ln(parentVisits.toDouble()) / node.visits)
//        Log.d(TAG,"UCT value for node ${node.id}: ${pouet}")
        return (node.score.toDouble() / node.visits) + 2 * sqrt(2.0) * sqrt(2 * ln(parentVisits.toDouble()) / node.visits)
    }
}

fun playout( node: Node ): Int {
    val myColor = node.game.currentPlayer
//    var score = 0
    val start = System.currentTimeMillis()
    var numberBlueBo = 0
    var numberRedBo = 0

    while( System.currentTimeMillis() - start < TIMEOUT_PLAYOUT ) {
        for (i in 1..PLAYOUTS) {
            val game = node.game.copyForPlayout()
            var isBlueVictory = game.checkVictoryFor(game.board, PieceColor.Blue)
            var isRedVictory = game.checkVictoryFor(game.board, PieceColor.Red)
            while (!isBlueVictory && !isRedVictory) {
                val move = randomPlay(game)
                val board = game.board.playAt(move)
                game.board = game.doPush(board, move)
                // check if we need to graduate a piece
                if (game.getGraduations().isNotEmpty())
                    game.promoteOrRemovePieces(randomGraduation(game))

                numberBlueBo = game.board.numberBlueBo
                numberRedBo = game.board.numberRedBo
                game.changePlayer()
                isBlueVictory = game.checkVictoryFor(game.board, PieceColor.Blue)
                isRedVictory = game.checkVictoryFor(game.board, PieceColor.Red)
            }
//        if(isBlueVictory) {
//            Log.d(TAG,"Blue wins in playout")
//        }
//        if(isRedVictory) {
//            Log.d(TAG,"Red wins in playout")
//        }
            if ((isBlueVictory && myColor == PieceColor.Blue) || (isRedVictory && myColor == PieceColor.Red)) {
//                score += 1
                return 1 // incompatible with PLAYOUTS > 1
//            Log.d(TAG,"My color ${myColor} wins!")
            }
//        if( (isBlueVictory && myColor == PieceColor.Red) || (isRedVictory && myColor == PieceColor.Blue))
//            score -= 1
        }
    }

    if( (myColor == PieceColor.Blue && numberBlueBo > numberRedBo )
        || (myColor == PieceColor.Red && numberBlueBo < numberRedBo) )
        return 1
    else
         return 0
//    return score
}

fun backpropagate( nodes: ArrayList<Node>, parentID: Int, score: Int ) {
    nodes[parentID].score += score
    nodes[parentID].visits++
//    Log.d(TAG,"Parent ${parentID}: score=${nodes[parentID].score}, visits=${nodes[parentID].visits}")
    if( parentID != 0 ) { // if not root
        backpropagate( nodes, nodes[parentID].parentID, 1 - score )
    }
}

fun tryEachPossibleMove( nodes: ArrayList<Node> ): Int {
    val root = nodes[0]
    val game = root.game
    val player = game.currentPlayer
    val positions = game.board.getAllEmptyPositions()
    val pieces: List<Piece>
    var numberNodes = 1

    if( game.board.hasTwoTypesInPool( player ) )
        pieces = listOf( Piece.createPo(player), Piece.createBo(player) )
    else
        pieces = listOf( game.board.getPlayerPool(player)[0] )

    for( piece in pieces )
        for( position in positions ) {
            val move = Move(piece, position)

            val newGame = game.copyForPlayout()
            val newBoard = newGame.board.playAt(move)
            newGame.board = newGame.doPush(newBoard, move)
            // check if we need to graduate a piece
            if( newGame.getGraduations().isNotEmpty() )
                newGame.promoteOrRemovePieces( randomGraduation(newGame) )

            val isBlueVictory = newGame.checkVictoryFor(newBoard, PieceColor.Blue)
            val isRedVictory = newGame.checkVictoryFor(newBoard, PieceColor.Red)
            val isTerminal = isBlueVictory || isRedVictory

            val score =
                if( isBlueVictory ) {
                    when( newGame.currentPlayer ) {
                        PieceColor.Blue -> 1
                        PieceColor.Red -> 0 //-1
                    }
                } else if( isRedVictory ) {
                    when( newGame.currentPlayer ) {
                        PieceColor.Red -> 1
                        PieceColor.Blue -> 0 //-1
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
                0,
                mutableListOf()
            )

            root.childID.add(numberNodes)
            nodes.add( child )
            numberNodes++
        }

    return numberNodes
}