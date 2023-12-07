package fr.richoux.pobo.engine.ai

import fr.richoux.pobo.engine.*

private const val TAG = "pobotag Decision"


// return null if no immediate winning move
fun searchForWinningMove(game: Game, movesToRemove: List<Move>): Move? {
  val board = game.board
  val player = game.currentPlayer

  val pool = when(player) {
    Color.Blue -> board.bluePool
    Color.Red -> board.redPool
  }

  var hasBo = false
  for(p in pool) if(p == 1.toByte()) {
    hasBo = true
    break
  }

  if(hasBo) {
    val emptyPositions = board.emptyPositions.toMutableList()
    for(m in movesToRemove) if(m.piece.getType() == PieceType.Bo) emptyPositions.remove(m.to)

    val newGame = game//.copyForPlayout()

    for(position in emptyPositions) {
      val move = Move(getBoInstanceOfColor(player), position)
      if(newGame.canPlay(move)) {
        var newBoard = board.playAt(move)
        newBoard = newGame.doPush(newBoard, move)
        if(newGame.checkVictoryFor(newBoard, player)) return move
      }
    }
  }
  return null
}

fun randomPlay(game: Game, movesToRemove: List<Move>): Move {
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
    Color.Blue -> board.bluePool
    Color.Red -> board.redPool
  }

  val type = pool.random()
  val positions = board.emptyPositions.toMutableList()
  for(m in movesToRemove) if(type == m.piece.getType().value) {
    positions.remove(m.to)
  }

  val position = positions.random()
  return Move(getPieceInstance(player, type), position)
}

fun randomPlay(game: Game): Move = randomPlay(game, listOf<Move>())
fun randomGraduation(game: Game): List<Position> = game.getGraduations().random()

class RandomPlay(color: Color) : AI(color) {
  override fun select_move(game: Game, lastOpponentMove: Move?, timeout_in_ms: Long): Move {
    return randomPlay(game)
  }

  override fun select_graduation(game: Game, timeout_in_ms: Long): List<Position> {
    return randomGraduation(game)
  }

  override fun toString(): String {
    return "RandomPlay"
  }
}