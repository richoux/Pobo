package fr.richoux.pobo.engine.ai

import android.util.Log
import fr.richoux.pobo.engine.*
import java.lang.Math.sqrt
import java.lang.StrictMath.abs
import kotlin.math.ln
import kotlin.math.pow

private const val TAG = "pobotag MCTS"

data class Node(
    val id: Int,
    val game: Game,
    val player: Color,
    val move: Move?,
    var score: Double,
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
        0.0,
        1,
        false,
        0,
        mutableListOf()
    ),
    var currentNode: Node = root,
    val nodes: ArrayList<Node> = arrayListOf(),
    var numberNodes: Int = 1,
    val number_preselected_actions: Int = 0,
    val extensions_and_playouts_with_GHOST: Boolean = false,
    val first_n_strategy: Int = 21,
    val playout_depth: Int = 21,
    val action_masking_time: Int = 6,
    val discount_score: Double = 0.9
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
            blue_turn: Boolean,
            to_remove_x: ByteArray,
            to_remove_y: ByteArray,
            to_remove_piece: ByteArray,
            number_to_remove: Int
        ): IntArray

        external fun ghost_solver_call_full(
            grid: ByteArray,
            blue_pool: ByteArray,
            red_pool: ByteArray,
            blue_pool_size: Int,
            red_pool_size: Int,
            blue_turn: Boolean,
            number_preselected_actions: Int
        ): IntArray

        external fun heuristic_state_cpp(
            grid: ByteArray,
            blue_turn: Boolean,
            blue_pool: ByteArray,
            blue_pool_size: Int,
            red_pool: ByteArray,
            red_pool_size: Int
        ): Double

        external fun compute_graduations_cpp(
            grid: ByteArray,
            blue_turn: Boolean,
            blue_pool_size: Int,
            red_pool_size: Int
        ): DoubleArray
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
                0.0,
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

        //TODO: should not be reinitialized each time
        val actionMasking = mutableListOf<Int>()

        if( number_preselected_actions > 0 )
        {
            /** Debug Action masking **/
//            Log.d(TAG,"\n*** Action masking, current grid ***\nGrid:")
//            var ss = ""
//            for( i in 0..35 ) {
//                var p = currentNode.game.board.grid[i].toInt()
//                if( p < 0 )
//                    p += 10;
//                ss += (p.toString() + " ")
//                if( (i+1) % 6 == 0 )
//                    ss += "\n"
//            }
//            Log.d(TAG,"$ss")

            val possible_moves = ghost_solver_call_full(
                currentNode.game.board.grid,
                currentNode.game.board.bluePool.toByteArray(),
                currentNode.game.board.redPool.toByteArray(),
                currentNode.game.board.bluePool.size,
                currentNode.game.board.redPool.size,
                currentNode.game.currentPlayer == Color.Blue,
                number_preselected_actions
            )

            if(possible_moves[0] != 42) {
                var actionToKeep: Boolean
                for (childID in currentNode.childID) {
                    actionToKeep = false
                    for (i in 0..(possible_moves.size / 3) - 1) {
                        if (nodes[childID].move?.piece?.code == possible_moves[3 * i].toByte()
                            && nodes[childID].move?.to?.x == possible_moves[3 * i + 2]
                            && nodes[childID].move?.to?.y == possible_moves[3 * i + 1]
                        ) {
                            actionToKeep = true
                            break
                        }
                    }
                    if (!actionToKeep) {
                        actionMasking.add(childID)
//                        Log.d(TAG,"Mask move ${nodes[childID].move} from node ${childID}")
                    }
                }
            }
        }
        else {
            for (node in nodes)
                if (node.move?.to?.x == 0
                    || node.move?.to?.x == 5
                    || node.move?.to?.y == 0
                    || node.move?.to?.y == 5
                ) {
                    actionMasking.add(node.id)
                }
        }

        while (System.currentTimeMillis() - start < timeout_in_ms) {

            /** Debug setup before selection **/
//            Log.d(TAG,"\n*** Before selection ***\nGrid:")
//            var ss = ""
//            for( i in 0..35 ) {
//                var p = game.board.grid[i].toInt()
//                if( p < 0 )
//                    p += 10;
//                ss += (p.toString() + " ")
//                if( (i+1) % 6 == 0 )
//                    ss += "\n"
//            }
//            Log.d(TAG,"$ss")
//            ss = ""
//            Log.d(TAG,"Blue player pool:")
//            for( i in 0..game.board.bluePool.size-1 ) {
//                var p = game.board.bluePool[i].toInt()
//                ss += (p.toString() + " ")
//            }
//            Log.d(TAG,"$ss")
//            ss = "Blue player pool size: " + game.board.bluePool.size.toString()
//            Log.d(TAG,"$ss")
//            if( game.board.bluePool.size == 1 )
//                Log.d(TAG,"Blue pool contains one unit only!")
//            ss = ""
//            Log.d(TAG,"Red player pool:")
//            for( i in 0..game.board.redPool.size-1 ) {
//                var p = game.board.redPool[i].toInt()
//                ss += (p.toString() + " ")
//            }
//            Log.d(TAG,"$ss")
//            ss = "Red player pool size: " + game.board.redPool.size.toString()
//            Log.d(TAG,"$ss")
//            if( game.board.redPool.size == 1 )
//                Log.d(TAG,"Red pool contains one unit only!")
//            var blueTurn = game.currentPlayer == Color.Blue
//            Log.d(TAG, "Is Blue turn: $blueTurn")
//            Log.d(TAG, "\n")

            /////////////////
            // Select node //
            /////////////////
            val selectedNode = UCT(actionMasking)
            val movesToRemove: MutableList<Move> = mutableListOf()

            /** Debug heuristics **/
//            val ggrid:ByteArray = byteArrayOf(
//                0,2,0,-2,0,0,
//                0,0,0,-2,0,2,
//                0,0,1,0,-1,0,
//                0,-1,2,0,0,-1,
//                0,0,0,0,0,0,
//                0,0,0,0,0,0 )
//            val bturn = true
//            val bpool:ByteArray = byteArrayOf(2,2,2)
//            val rpool:ByteArray = byteArrayOf(1,2,2,2)
//
//            ss = "Pouet grid:\n"
//            for( i in 0..35 ) {
//                var p = ggrid[i].toInt()
//                if( p < 0 )
//                    p += 10;
//                ss += (p.toString() + " ")
//                if( (i+1) % 6 == 0 )
//                    ss += "\n"
//            }
//            Log.d(TAG,"$ss")
//
//            //compute_graduations_cpp( ggrid, false, 2, 0 )
//
////            val state= heuristic_state_cpp(
////                ggrid,
////                bturn,
////                bpool,
////                1,
////                rpool,
////                5 )
////            ss = state.toString() + "\n"
////            Log.d(TAG,"$ss")
//
//            val possible_moves = ghost_solver_call_full(
//                ggrid,
//                bpool,
//                rpool,
//                3,
//                4,
//                bturn,
//                27
//            )
            /////////// End test heuristics

            //TODO: add actionMasking moves?
            for (child in selectedNode.childID)
                nodes[child].move?.let { movesToRemove.add(it) }

            /** Debug selection **/
//            ss = ""
//            for( i in 0..35 ) {
//                var p = selectedNode.game.board.grid[i].toInt()
//                if( p < 0 )
//                    p += 10;
//                ss += (p.toString() + " ")
//                if( (i+1) % 6 == 0 )
//                    ss += "\n"
//            }
//            Log.d(TAG,"$ss")
//            ss = ""
//            Log.d(TAG,"Blue player pool:")
//            for( i in 0..selectedNode.game.board.bluePool.size-1 ) {
//                var p = selectedNode.game.board.bluePool[i].toInt()
//                ss += (p.toString() + " ")
//            }
//            Log.d(TAG,"$ss")
//            ss = "Blue player pool size: " + selectedNode.game.board.bluePool.size.toString()
//            Log.d(TAG,"$ss")
//            ss = ""
//            Log.d(TAG,"Red player pool:")
//            for( i in 0..selectedNode.game.board.redPool.size-1 ) {
//                var p = selectedNode.game.board.redPool[i].toInt()
//                ss += (p.toString() + " ")
//            }
//            Log.d(TAG,"$ss")
//            ss = "Red player pool size: " + selectedNode.game.board.redPool.size.toString()
//            Log.d(TAG,"$ss")
//            blueTurn = selectedNode.game.currentPlayer == Color.Blue
//            Log.d(TAG, "Is Blue turn: $blueTurn")
//            Log.d(TAG, "\n")

            ////////////
            // Expand //
            ////////////
            //val move = randomPlay( selectNode.game, movesToRemove.toList() )

            var movesToRemoveRow: ByteArray = byteArrayOf()
            var movesToRemoveColumn: ByteArray = byteArrayOf();
            var movesToRemovePiece: ByteArray = byteArrayOf();

//            Log.d(TAG,"Number of moves to remove: ${movesToRemove.size}")
            for( move in movesToRemove ) {
//                Log.d(TAG,"Removed move=${move}")
                movesToRemoveRow += move.to.y.toByte() // y are rows
                movesToRemoveColumn += move.to.x.toByte() // x are columns
                movesToRemovePiece += abs( move.piece.code.toInt() ).toByte()
            }
            var move: Move
            val solution = ghost_solver_call(
                selectedNode.game.board.grid,
                selectedNode.game.board.bluePool.toByteArray(),
                selectedNode.game.board.redPool.toByteArray(),
                selectedNode.game.board.bluePool.size,
                selectedNode.game.board.redPool.size,
                selectedNode.game.currentPlayer == Color.Blue,
                movesToRemoveRow,
                movesToRemoveColumn,
                movesToRemovePiece,
                movesToRemove.size
            )

            numberSolverCalls++
            if (solution[0] == 42) {
                move = randomPlay(selectedNode.game, movesToRemove.toList())
                numberSolverFailures++
                Log.d(TAG, "### Expansion: RANDOM move ${move}")
            }
            else {
                val code = when (selectedNode.game.currentPlayer) {
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
//                Log.d(TAG, "### Expansion: solver move ${move}, cost ${solution[3]}")
            }

            val expandedNode = createNode(selectedNode.game, move, selectedNode.id)

            /** Debug expansion **/
//            ss = "Expansion done, created node ${expandedNode.id}:\n"
//            for( i in 0..35 ) {
//                var p = expandedNode.game.board.grid[i].toInt()
//                if( p < 0 )
//                    p += 10;
//                ss += (p.toString() + " ")
//                if( (i+1) % 6 == 0 )
//                    ss += "\n"
//            }
//            Log.d(TAG,"$ss")
//            Log.d(TAG, "\n")

            /////////////
            // Playout //
            /////////////
            if (!expandedNode.isTerminal) {
                expandedNode.score = -playout(expandedNode, first_n_strategy) // first n moves are GHOST-based
                expandedNode.score /= selectedNode.childID.size // first expansions are the most optimal one according to the heuristics
                // -playout because the score returned is the score for the selected node
                numberPlayouts++
            }

            /////////////////////////
            // Backpropagate score //
            /////////////////////////
//            Log.d( TAG,"\nExpanded node ${expandedNode.id} score = ${expandedNode.score}, visits = ${expandedNode.visits}" )
            backpropagate(selectedNode.id, expandedNode.score)
        }

//        Log.d(TAG,"MCTS timeout")

        val potentialChildrenID = mutableListOf<Int>()

        // Visit
        var mostSelected = 0
        var bestScore = -10000.0
        var bestRatio = -10000.0
//        Log.d( TAG,"Current node ID: ${currentNode.id}" )
        for (childID in currentNode.childID) {
//            Log.d( TAG,"Current node's child ID: ${childID}, ${nodes[childID].move}, visits=${nodes[childID].visits}, score=${nodes[childID].score}" )
            if( nodes[childID].visits == 0 )
                continue

            // Best score
//            if (nodes[childID].score > bestScore) {
//                bestRatio = nodes[childID].score.toDouble() / nodes[childID].visits
//                potentialChildrenID.clear()
//                bestScore = nodes[childID].score
//                potentialChildrenID.add(childID)
//            } else if (nodes[childID].score == bestScore) {
//                val ratio = nodes[childID].score.toDouble() / nodes[childID].visits
//                if (ratio > bestRatio) {
//                    potentialChildrenID.clear()
//                    bestRatio = ratio
//                    potentialChildrenID.add(childID)
//                } else if (ratio == bestRatio) {
//                    potentialChildrenID.add(childID)
//                }
//            }

            //Best ratio
            val currentRatio = nodes[childID].score.toDouble() / nodes[childID].visits
            if (currentRatio > bestRatio) {
                bestRatio = currentRatio
                potentialChildrenID.clear()
                potentialChildrenID.add(childID)
            }
            else
                if (currentRatio == bestRatio) {
                  potentialChildrenID.add(childID)
                }

            // Most visited
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
        bestRatio = nodes[bestChildID].score.toDouble() / nodes[bestChildID].visits
//        Log.d(TAG,"Best child ID (new current node): ${bestChildID} ${nodes[bestChildID].move}, visits=${nodes[bestChildID].visits}, ratio=${bestRatio}, score=${nodes[bestChildID].score}")
//        Log.d(TAG, "Tree size: ${nodes.size} nodes, number of playouts: ${numberPlayouts}, solver calls: ${numberSolverCalls}, solver failures: ${numberSolverFailures}")

        currentNode = nodes[bestChildID]
        return currentNode.move!!
    }

    fun UCT(node: Node, actionMasking: MutableList<Int>): Node {
        var mask_size: Int = 0

        if ( number_preselected_actions == 0 && node.game.moveNumber < action_masking_time)
            mask_size = action_masking_time

        if (node.childID.size < ( node.game.board.emptyPositions.size - mask_size ) ) {
//            Log.d(TAG, "### Selection: selected node ID=${node.id}, node.childID.size=${node.childID.size}, available moves=${node.game.board.emptyPositions.size}")
            return node
        }
        var bestValue = -10000.0
        val potentialNodes = mutableListOf<Node>()

//        Log.d(TAG, "UCT: start scanning children")

        for (nodeID in node.childID) {
            if (!actionMasking.contains(nodeID) || ( number_preselected_actions == 0 && nodes[nodeID].game.moveNumber > action_masking_time ) ) {
//                Log.d( TAG,"Selection: current node's child ID ${nodeID} moveNumber=${nodes[nodeID].game.moveNumber}")
                val newNode = nodes[nodeID]
                val value = UCTValue(newNode, newNode.visits)
                if ( value > bestValue ) {
                    potentialNodes.clear()
                    bestValue = value
//                    Log.d(TAG, "### Selection: better child found. ID=${newNode.id}, value=${value}, moveNumber=${newNode.game.moveNumber}")
                    potentialNodes.add(newNode)
                } else if (value == bestValue ) {
                    potentialNodes.add(newNode)
//                    Log.d(TAG, "### Selection: equivalent child found. ID=${newNode.id}, value=${value}, moveNumber=${newNode.game.moveNumber}")
                }
            }
        }

//        Log.d(TAG, "UCT: finish scanning children")

        // Should never happen
        if( potentialNodes.isEmpty() ) {
//            Log.d(TAG, "### Selection: no best child (?!). Return current node ID=${node.id}")
            return node
        }

        val bestNode = potentialNodes.random()
//        Log.d(TAG, "### Selection: best child ID=${bestNode.id}. Going deeper.")
        return UCT(bestNode, actionMasking)
    }

    fun UCT(actionMasking: MutableList<Int>): Node = UCT(currentNode, actionMasking)

    fun UCTValue(node: Node, parentVisits: Int ): Double {
        val coeff = when( node.player ) {
            Color.Blue -> 1 //-1
            Color.Red -> 1
        }
        if (node.visits == 0) {
//            Log.d(TAG, "UCT, node ${node.id} has ${node.visits} visits")
            return 999999.9
        } else {
//            Log.d(TAG, "UCT, node ${node.id} score=${(node.score / node.visits) + sqrt(2.0) * sqrt(ln(parentVisits.toDouble()) / node.visits)}")
//            ( (coeff * node.score) / node.visits) + (2.0 / sqrt(2.0)) * sqrt(2 * ln(parentVisits.toDouble()) / node.visits)
            return (node.score / node.visits) + sqrt(2.0) * sqrt(ln(parentVisits.toDouble()) / node.visits)
        }
    }

    fun playout(node: Node, first_n: Int = 0): Double {
        var numberMoves = 0

        val game = node.game.copyForPlayout()
        var isBlueVictory = game.checkVictoryFor(game.board, Color.Blue)
        var isRedVictory = game.checkVictoryFor(game.board, Color.Red)
        var score = 0.0
        // if node.player == Color.Red, then the selected node, i.e., node's parent, is blue
        val selectedNodeColorIsBlue = ( node.player == Color.Red );

        /** Debug setup before playouts **/
//        Log.d(TAG,"### Playout: is selected node color blue? ${selectedNodeColorIsBlue}")
//        var ss = ""
//        for (i in 0..35) {
//            var p = game.board.grid[i].toInt()
//            if (p < 0)
//                p += 10;
//            ss += (p.toString() + " ")
//            if ((i + 1) % 6 == 0)
//                ss += "\n"
//        }
//        ss += "\n"
//        Log.d(TAG, "${ss}")

        while (!isBlueVictory && !isRedVictory && ( numberMoves < playout_depth ) ) {
            val move: Move
            if( numberMoves >= first_n ) {
//                Log.d(TAG,"### Playout: ${numberMoves} moves -> random move")
                move = randomPlay(game)
            }
            else {
                val movesToRemoveRow: ByteArray = byteArrayOf()
                val movesToRemoveColumn: ByteArray = byteArrayOf();
                val movesToRemovePiece: ByteArray = byteArrayOf();
                val solution = ghost_solver_call(
                    game.board.grid,
                    game.board.bluePool.toByteArray(),
                    game.board.redPool.toByteArray(),
                    game.board.bluePool.size,
                    game.board.redPool.size,
                    game.currentPlayer == Color.Blue,
                    movesToRemoveRow,
                    movesToRemoveColumn,
                    movesToRemovePiece,
                    0
                )

//                Log.d(TAG,"### Playout: ${numberMoves} moves -> solver called")

                if (solution[0] == 42) {
//                    Log.d(TAG,"### Playout: no solution found -> random move")
                    move = randomPlay(game)
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

//                    Log.d(TAG, "### Playout: solver move ${move}, cost ${solution[3]}")
                }
            }

            val board = game.board.playAt(move)
            game.board = game.doPush(board, move)
            isBlueVictory = game.checkVictoryFor(game.board, Color.Blue)
            isRedVictory = game.checkVictoryFor(game.board, Color.Red)

            // check if we need to graduate a piece
            val potentialGraduations = game.getGraduations()
            if (!isBlueVictory && !isRedVictory && potentialGraduations.isNotEmpty()) {
                val graduationScores = compute_graduations_cpp(
                    game.board.grid,
                    game.currentPlayer == Color.Blue,
                    game.board.bluePool.size,
                    game.board.redPool.size
                )
                var best_score = -10000.0
                var best_groups: MutableList<Int> = mutableListOf()

                graduationScores.forEachIndexed { index, score ->
                    if( best_score < score ) {
                        best_score = score
                        best_groups.clear()
                        best_groups.add( index )
                    }
                    else if( best_score == score ) {
                        best_groups.add( index )
                    }
                }

                //game.promoteOrRemovePieces( randomGraduation( game ) )
                game.promoteOrRemovePieces( potentialGraduations[ best_groups.random() ] )
            }

            game.changePlayer()
            numberMoves++

            if (!isBlueVictory && !isRedVictory) {
                val heuristic_score = heuristic_state_cpp(
                    game.board.grid,
                    selectedNodeColorIsBlue,
                    game.board.bluePool.toByteArray(),
                    game.board.bluePool.size,
                    game.board.redPool.toByteArray(),
                    game.board.redPool.size
                )
                val exponential_discount = discount_score.pow(numberMoves-1) // -1 because we don't want any discount for the first move
                score += (exponential_discount * heuristic_score)

//                Log.d(TAG,"### Playout: discounted cumulative score = ${score}. Discount=${exponential_discount}, original score=${heuristic_score}")
            }

            /** Debug playouts **/
//            var ss = ""
//            for (i in 0..35) {
//                var p = game.board.grid[i].toInt()
//                if (p < 0)
//                    p += 10;
//                ss += (p.toString() + " ")
//                if ((i + 1) % 6 == 0)
//                    ss += "\n"
//            }
//            Log.d(TAG, "${ss}")
//            ss = ""
//            Log.d(TAG, "Blue player pool:")
//            for (i in 0..game.board.bluePool.size - 1) {
//                var p = game.board.bluePool[i].toInt()
//                ss += (p.toString() + " ")
//            }
//            Log.d(TAG, "${ss}")
//            ss = "Blue player pool size: " + game.board.bluePool.size.toString()
//            Log.d(TAG,"$ss")
//            ss = ""
//            Log.d(TAG, "Red player pool:")
//            for (i in 0..game.board.redPool.size - 1) {
//                var p = game.board.redPool[i].toInt()
//                ss += (p.toString() + " ")
//            }
//            Log.d(TAG, "${ss}")
//            ss = "Red player pool size: " + game.board.redPool.size.toString()
//            Log.d(TAG,"$ss")
//            var blueTurn = game.currentPlayer == Color.Blue
//            Log.d(TAG, "Is Blue turn: $blueTurn")
//            Log.d(TAG, "\n")
        }

        /** Debug playouts results **/
//        ss = ""
//        for (i in 0..35) {
//            var p = game.board.grid[i].toInt()
//            if (p < 0)
//                p += 10;
//            ss += (p.toString() + " ")
//            if ((i + 1) % 6 == 0)
//                ss += "\n"
//        }
//        ss += "\n"
//        Log.d(TAG, "${ss}")

        if( isBlueVictory ) {
            score += -discount_score.pow(numberMoves-1) //* -1000.0
//            Log.d(TAG, "### Playout: Blue victory, score = ${score}")
        }
        else {
            if( isRedVictory ) {
                score += discount_score.pow(numberMoves-1) //* 1000.0
//                Log.d(TAG, "### Playout: Red victory, score = ${score}")
            }
//            else {
//                Log.d(TAG, "### Playout: No victory, score = ${score}")
//            }
        }
        return score
    }

    fun backpropagate(parentID: Int, score: Double) {
//        Log.d( TAG,"### Backprop: Node ${parentID} old score = ${nodes[parentID].score}, old visits = ${nodes[parentID].visits}" )
        nodes[parentID].score += -score
        nodes[parentID].visits++
//        Log.d( TAG,"### Backprop: Node ${parentID} new score = ${nodes[parentID].score}, new visits = ${nodes[parentID].visits}" )
        if (parentID != 0) { // if not root
            backpropagate(nodes[parentID].parentID, -score)
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
        val isBlueVictory = newGame.checkVictoryFor(newGame.board, Color.Blue)
        val isRedVictory = newGame.checkVictoryFor(newGame.board, Color.Red)
        val isTerminal = isBlueVictory || isRedVictory

        val potentialGraduations = newGame.getGraduations()
        if (!isTerminal && potentialGraduations.isNotEmpty()) {
            val graduationScores = compute_graduations_cpp(
                newGame.board.grid,
                newGame.currentPlayer == Color.Blue,
                newGame.board.bluePool.size,
                newGame.board.redPool.size
            )
            var best_score = -10000.0
            var best_groups: MutableList<Int> = mutableListOf()

            graduationScores.forEachIndexed { index, score ->
                if( best_score < score ) {
                    best_score = score
                    best_groups.clear()
                    best_groups.add( index )
                }
                else if( best_score == score ) {
                    best_groups.add( index )
                }
            }

            //game.promoteOrRemovePieces( randomGraduation( game ) )
            newGame.promoteOrRemovePieces( potentialGraduations[ best_groups.random() ] )
        }

        val score =
            if (isBlueVictory)
                -1.0 //-1000.0
            else {
                if (isRedVictory)
                    1.0 //1000.0
                else
                    0.0
                    //heuristic_state_cpp( newGame.board.grid, newGame.currentPlayer == Color.Blue ).toInt();
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

        /** Debug node creation **/
//        var ss = "Create node number ${numberNodes} with move ${move}"
//        Log.d(TAG, "${ss}")
//        ss = ""
//        for (i in 0..35) {
//            var p = newGame.board.grid[i].toInt()
//            if (p < 0)
//                p += 10;
//            ss += (p.toString() + " ")
//            if ((i + 1) % 6 == 0)
//                ss += "\n"
//        }
//        Log.d(TAG, "${ss}")
//        ss = ""
//        Log.d(TAG, "Blue player pool:")
//        for (i in 0..newGame.board.bluePool.size - 1) {
//            var p = newGame.board.bluePool[i].toInt()
//            ss += (p.toString() + " ")
//        }
//        Log.d(TAG, "${ss}")
//        ss = "Blue player pool size: " + newGame.board.bluePool.size.toString()
//        Log.d(TAG,"$ss")
//        ss = ""
//        Log.d(TAG, "Red player pool:")
//        for (i in 0..newGame.board.redPool.size - 1) {
//            var p = newGame.board.redPool[i].toInt()
//            ss += (p.toString() + " ")
//        }
//        Log.d(TAG, "${ss}")
//        ss = "Red player pool size: " + newGame.board.redPool.size.toString()
//        Log.d(TAG,"$ss")
//        var blueTurn = newGame.currentPlayer == Color.Blue
//        Log.d(TAG, "Is Blue turn: $blueTurn")
//        Log.d(TAG, "\n")

        nodes[parentID].childID.add(numberNodes)
        nodes.add(child)
        numberNodes++

        return child
    }
}