package fr.richoux.pobo.engine.ai

import fr.richoux.pobo.engine.*

class PureHeuristics(color: Color) : AI(color) {
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

    external fun compute_graduations_cpp(
      grid: ByteArray, blue_turn: Boolean, blue_pool_size: Int, red_pool_size: Int
    ): DoubleArray
  }

  override fun select_move(game: Game, lastOpponentMove: Move?, timeout_in_ms: Long): Move {
    val solution = ghost_solver_call(
      game.board.grid,
      game.board.bluePool.toByteArray(),
      game.board.redPool.toByteArray(),
      game.board.bluePool.size,
      game.board.redPool.size,
      game.currentPlayer == Color.Blue,
      byteArrayOf(),
      byteArrayOf(),
      byteArrayOf(),
      0
    )

    if(solution[0] == 42) {
      return randomPlay(game)
    } else {
      val code = when(game.currentPlayer) {
        Color.Blue -> -solution[0]
        Color.Red -> solution[0]
      }

      val id = when(code) {
        -2 -> "BB"
        -1 -> "BP"
        1 -> "RP"
        else -> "RB"
      }
      val piece = Piece(id, code.toByte())
      val position = Position(solution[2], solution[1])
      return Move(piece, position)
    }
  }

  override fun select_graduation(game: Game, timeout_in_ms: Long): List<Position> {
    val potentialGraduations = game.getGraduations()
    val graduationScores = MCTS_GHOST.compute_graduations_cpp(
      game.board.grid,
      game.currentPlayer == Color.Blue,
      game.board.bluePool.size,
      game.board.redPool.size
    )
    var best_score = -10000.0
    var best_groups: MutableList<Int> = mutableListOf()

    graduationScores.forEachIndexed { index, score ->
      if(best_score < score) {
        best_score = score
        best_groups.clear()
        best_groups.add(index)
      } else if(best_score == score) {
        best_groups.add(index)
      }
    }

    return potentialGraduations[best_groups.random()]
  }

  override fun toString(): String {
    return "Pure Heuristics"
  }
}