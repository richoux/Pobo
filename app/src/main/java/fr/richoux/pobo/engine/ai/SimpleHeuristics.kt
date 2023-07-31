package fr.richoux.pobo.engine.ai

import android.util.Log
import fr.richoux.pobo.engine.*
import java.lang.Integer.MIN_VALUE
import java.lang.StrictMath.abs

private const val TAG = "pobotag SimpleHeuristics"

private enum class PieceType {
    BO, PO, WHATEVER
}

private enum class Direction {
    TOPRIGHT, RIGHT, BOTTOMRIGHT, BOTTOM
}

class SimpleHeuristics(color: Color) : AI(color) {

    lateinit var _game: Game //= Game()
    lateinit var _simulation_grid: ByteArray // = ByteArray(36) { 0 }
    var _blue_turn = false

    override fun select_move(game: Game,
                             lastOpponentMove: Move,
                             timeout_in_ms: Long): Move {
        Log.d(TAG, "*************")
        Log.d(TAG, "New call to SimpleHeuristics::select_move")
        _game = game.copyForPlayout()
        _blue_turn = _game.currentPlayer == Color.Blue

//        var grid_string = ""
//        for(row in 0..5 ) {
//            for (col in 0..5)
//                grid_string += ( "" + _game.board.grid[row*6 + col] + " " )
//            grid_string += "\n"
//        }
//        Log.d(TAG, grid_string)

        var max_score = MIN_VALUE
        var best_moves = mutableListOf<Move>() //= Move( getPoInstanceOfColor(color), Position(-1, -1) )
        var score: Int
        for( free_cell in _game.board.emptyPositions )
        {
            if( _game.board.hasTwoTypesInPool( color ) ) {
                score = compute_score(free_cell.y, free_cell.x, PieceType.PO )
//                Log.d(TAG, "move ${Move( getPoInstanceOfColor(color), free_cell )} has score $score")
                if( max_score < score )
                {
                    max_score = score
                    val best_move = Move( getPoInstanceOfColor(color), free_cell )
                    best_moves.clear()
                    best_moves.add( best_move )
                }
                else if( max_score == score )
                {
                    val best_move = Move( getPoInstanceOfColor(color), free_cell )
                    best_moves.add( best_move )
                }

                score = compute_score(free_cell.y, free_cell.x, PieceType.BO )
//                Log.d(TAG, "Move ${Move( getBoInstanceOfColor(color), free_cell )} has score $score")
                if( max_score < score )
                {
                    max_score = score
                    val best_move = Move( getBoInstanceOfColor(color), free_cell )
                    best_moves.clear()
                    best_moves.add( best_move )
                }
                else if( max_score == score )
                {
                    val best_move = Move( getBoInstanceOfColor(color), free_cell )
                    best_moves.add( best_move )
                }
            } else {
                val type: PieceType
                val piece: Piece

                if( _game.board.getPlayerPool(color)[0] == fr.richoux.pobo.engine.PieceType.Po.value ) {
                    type = PieceType.PO
                    piece = getPoInstanceOfColor(color)
                }
                else {
                    type = PieceType.BO
                    piece = getBoInstanceOfColor(color)
                }

                score = compute_score(free_cell.y, free_cell.x, type )
//                Log.d(TAG, "Move ${Move( piece, free_cell )} has score $score")
                if( max_score < score )
                {
                    max_score = score
                    val best_move = Move( piece, free_cell )
                    best_moves.clear()
                    best_moves.add( best_move )
                }
                else if( max_score == score )
                {
                    val best_move = Move( piece, free_cell )
                    best_moves.add( best_move )
                }
            }
        }

        val best_move = best_moves.random()
        Log.d(TAG, "Best move found: ${best_move}, score $max_score")

        Thread.sleep( timeout_in_ms / 2 )
        return best_move
    }

    private fun check_three_in_a_row(from_row: Int,
                                     from_col: Int,
                                     direction: Direction,
                                     type: PieceType
    ) : Boolean
    {
        if( from_col > 5 || from_row < 0 || from_row > 5 )
            return false

        val next_row = get_next_row( from_row, direction )
        val next_col = get_next_col( from_col, direction )

        if( _simulation_grid[from_row*6 + from_col] == 0.toByte() || next_col > 5 || next_row < 0 || next_row > 5 )
            return false

        return check_two_in_a_row( from_row, from_col, direction, type )
                && check_two_in_a_row( next_row, next_col, direction, type )
    }

    private fun check_two_in_a_row( from_row: Int,
                                    from_col: Int,
                                    direction: Direction,
                                    type: PieceType
    ) : Boolean
    {
        if( from_col > 5 || from_row < 0 || from_row > 5 )
            return false

        val next_row = get_next_row( from_row, direction )
        val next_col = get_next_col( from_col, direction )

        val index = from_row*6 + from_col
        val type_value = when( type ) {
            PieceType.PO -> 1
            PieceType.BO -> 2
            else -> 3 // WHATEVER
        }

        if( ( type_value != 3 && abs( _simulation_grid[index].toInt() ) != type_value ) ||
            next_col > 5 || next_row < 0 || next_row > 5 )
            return false

        val next_index = next_row*6 + next_col

        return when( type )
        {
            PieceType.WHATEVER -> _simulation_grid[index] * _simulation_grid[next_index] > 0
            else -> _simulation_grid[index] == _simulation_grid[next_index]
        }
    }

    private fun count_Po_in_a_row( from_row: Int,
                                   from_col: Int,
                                   direction: Direction ) : Int
    {
        if( from_col > 5 || from_row < 0 || from_row > 5 )
            return 0

        val next_row = get_next_row( from_row, direction )
        val next_col = get_next_col( from_col, direction )
        val index = from_row*6 + from_col

        if( _simulation_grid[index] == 0.toByte() || next_col > 5 || next_row < 0 || next_row > 5 )
            return 0;

        val next_next_row = get_next_row( next_row, direction )
        val next_next_col = get_next_col( next_col, direction )
        val next_index = next_row*6 + next_col

        if( _simulation_grid[next_index] == 0.toByte() || next_next_col > 5 || next_next_row < 0 || next_next_row > 5 )
            return 0

        val next_next_index = next_next_row * 6 + next_next_col

        var count = 0
        // no need to check the color of these Po pieces: count_Po_in_a_row is only called after
        // checking if pieces are of the same color, with check_three_in_a_row
        if( abs( _simulation_grid[index].toInt() ) == 1 )
            ++count
        if( abs( _simulation_grid[next_index].toInt() ) == 1 )
            ++count
        if( abs( _simulation_grid[next_next_index].toInt() ) == 1 )
            ++count

        return count
    }

    private fun get_next_row( from_row: Int,
                              direction: Direction ): Int
    {
        return when( direction )
        {
            Direction.TOPRIGHT -> from_row - 1
            Direction.RIGHT -> from_row
            Direction.BOTTOMRIGHT -> from_row + 1
            else -> from_row + 1 // Direction.BOTTOM
        }
    }

    private fun get_next_col( from_col: Int,
                              direction: Direction ): Int
    {
        return when( direction )
        {
            Direction.TOPRIGHT -> from_col + 1
            Direction.RIGHT -> from_col + 1
            Direction.BOTTOMRIGHT -> from_col + 1
            else -> from_col // Direction.BOTTOM
        }
    }

    private fun compute_partial_score( from_row: Int,
                                       from_col: Int,
                                       direction: Direction ): Pair<Int, Int>
    {
        var score = 0
        var jump_forward = 0
        val is_player_piece: Boolean = ( ( _simulation_grid[ from_row * 6 + from_col ] < 0 && _blue_turn ) ||
                ( _simulation_grid[ from_row * 6 + from_col ] > 0 && !_blue_turn ) )

        if( check_three_in_a_row( from_row, from_col, direction, PieceType.BO ))
        {
            score += if( is_player_piece ) 200 else -200
            jump_forward = 2
        }
        else
        {
            if( check_three_in_a_row( from_row, from_col, direction, PieceType.PO ))
            {
                score += if( is_player_piece ) 30 else -30
                jump_forward = 2
            }
            else
            {
                if( check_three_in_a_row( from_row, from_col, direction, PieceType.WHATEVER ))
                {
                    score += count_Po_in_a_row( from_row, from_col, direction ) * ( if ( is_player_piece ) 10 else -10 )
                    jump_forward = 1
                }
                else
                {
                    if( check_two_in_a_row( from_row, from_col, direction, PieceType.BO ))
                    {
                        score += if( is_player_piece) 20 else -100
                        jump_forward = 1
                    }
                    else
                    {
                        if( check_two_in_a_row( from_row, from_col, direction, PieceType.PO ))
                        {
                            score += if( is_player_piece ) 5 else -10
                            jump_forward = 1
                        }
                    }
                }
            }
        }

        return Pair( score, jump_forward )
    }

    private fun simulate_move( row: Int,
                               col: Int,
                               type: PieceType )
    {
        val p = ( ( if( type == PieceType.PO ) 1 else 2 ) * if( _blue_turn ) -1 else 1 ).toByte()
        val index = row*6 + col

        _simulation_grid[index] = p

        if( col-1 >= 0 )
        {
            // Top Left
            if( row-1 >= 0 )
            {
                if( _simulation_grid[ (row-1)*6 + col-1 ] != 0.toByte() &&
                    abs( _simulation_grid[ (row-1)*6 + col-1 ].toInt() ) <= abs( _simulation_grid[ index ].toInt() ) &&
                    ( col-2 < 0 || row-2 < 0 || _simulation_grid[ (row-2)*6 + col-2] == 0.toByte() ))
                {
                    if( col-2 >= 0 && row-2 >= 0 && _simulation_grid[ (row-2)*6 + col-2] == 0.toByte() )
                        _simulation_grid[ (row-2)*6 + col-2] = _simulation_grid[ (row-1)*6 + col-1]
                    _simulation_grid[ (row-1)*6 + col-1] = 0
                }
            }

            // Bottom Left
            if( row+1 <= 5 )
            {
                if( _simulation_grid[ (row+1)*6 + col-1] != 0.toByte() &&
                    abs( _simulation_grid[ (row+1)*6 + col-1 ].toInt() ) <= abs( _simulation_grid[ index ].toInt() ) &&
                    ( col-2 < 0 || row+2 > 5 || _simulation_grid[ (row+2)*6 + col-2] == 0.toByte() ))
                {
                    if( col-2 >= 0 && row+2 <= 5 && _simulation_grid[ (row+2)*6 + col-2] == 0.toByte() )
                        _simulation_grid[ (row+2)*6 + col-2] = _simulation_grid[ (row+1)*6 + col-1]
                    _simulation_grid[ (row+1)*6 + col-1] = 0
                }
            }

            // Left
            if( _simulation_grid[ row*6 + col-1] != 0.toByte() &&
                abs( _simulation_grid[ row*6 + col-1 ].toInt() ) <= abs( _simulation_grid[ index ].toInt() ) &&
                ( col-2 < 0 || _simulation_grid[ row*6 + col-2] == 0.toByte() ))
            {
                if( col-2 >= 0 && _simulation_grid[ row*6 + col-2 ] == 0.toByte() )
                    _simulation_grid[ row*6 + col-2 ] = _simulation_grid[ row*6 + col-1 ]
                _simulation_grid[ row*6 + col-1 ] = 0
            }
        }

        if( col+1 <= 5 )
        {
            // Top Right
            if( row-1 >= 0 )
            {
                if( _simulation_grid[ (row-1)*6 + col+1 ] != 0.toByte() &&
                    abs( _simulation_grid[ (row-1)*6 + col+1 ].toInt() ) <= abs( _simulation_grid[ index ].toInt() ) &&
                    ( col+2 > 5 || row-2 < 0 || _simulation_grid[ (row-2)*6 + col+2 ] == 0.toByte() ))
                {
                    if( col+2 <= 5 && row-2 >= 0 && _simulation_grid[ (row-2)*6 + col+2 ] == 0.toByte() )
                        _simulation_grid[ (row-2)*6 + col+2 ] = _simulation_grid[ (row-1)*6 + col+1 ]
                    _simulation_grid[ (row-1)*6 + col+1 ] = 0
                }
            }

            // Bottom Right
            if( row+1 <= 5 )
            {
                if( _simulation_grid[ (row+1)*6 + col+1] != 0.toByte() &&
                    abs( _simulation_grid[ (row+1)*6 + col+1 ].toInt() ) <= abs( _simulation_grid[ index ].toInt() ) &&
                    ( col+2 > 5 || row+2 > 5 || _simulation_grid[ (row+2)*6 + col+2 ] == 0.toByte() ))
                {
                    if( col+2 <= 5 && row+2 <= 5 && _simulation_grid[ (row+2)*6 + col+2 ] == 0.toByte() )
                        _simulation_grid[ (row+2)*6 + col+2 ] = _simulation_grid[ (row+1)*6 + col+1 ]
                    _simulation_grid[ (row+1)*6 + col+1 ] = 0
                }
            }

            // Right
            if( _simulation_grid[ row*6 + col+1 ] != 0.toByte() &&
                abs( _simulation_grid[ row*6 + col+1 ].toInt() ) <= abs( _simulation_grid[ index ].toInt() ) &&
                ( col+2 > 5 || _simulation_grid[ row*6 + col+2 ] == 0.toByte() ))
            {
                if( col+2 <= 5 && _simulation_grid[ row*6 + col+2 ] == 0.toByte() )
                    _simulation_grid[ row*6 + col+2 ] = _simulation_grid[ row*6 + col+1 ]
                _simulation_grid[ row*6 + col+1 ] = 0
            }
        }

        // Top
        if( row-1 >= 0 )
        {
            if( _simulation_grid[ (row-1)*6 + col ] != 0.toByte() &&
                abs( _simulation_grid[ (row-1)*6 + col ].toInt() ) <= abs( _simulation_grid[ index ].toInt() ) &&
                ( row-2 < 0 || _simulation_grid[ (row-2)*6 + col ] == 0.toByte() ))
            {
                if( row-2 >= 0 && _simulation_grid[ (row-2)*6 + col ] == 0.toByte() )
                    _simulation_grid[ (row-2)*6 + col ] = _simulation_grid[ (row-1)*6 + col ]
                _simulation_grid[ (row-1)*6 + col ] = 0
            }
        }

        // Bottom
        if( row+1 <= 5 )
        {
            if( _simulation_grid[ (row+1)*6 + col ] != 0.toByte() &&
                abs( _simulation_grid[ (row+1)*6 + col ].toInt() ) <= abs( _simulation_grid[ index ].toInt() ) &&
                ( row+2 > 5 || _simulation_grid[ (row+2)*6 + col ] == 0.toByte() ))
            {
                if( row+2 <= 5 && _simulation_grid[ (row+2)*6 + col ] == 0.toByte() )
                    _simulation_grid[ (row+2)*6 + col ] = _simulation_grid[ (row+1)*6 + col ]
                _simulation_grid[ (row+1)*6 + col ] = 0
            }
        }
    }

    private fun compute_score( row: Int,
                               col: Int,
                               type: PieceType ) : Int
    {
        var score = 0

        _simulation_grid = _game.board.grid.copyOf()

        simulate_move( row, col, type )

//        var grid_string = ""
//        for( row in 0..5 ) {
//            for ( col in 0..5 )
//                grid_string += ( "" + _simulation_grid[ row*6 + col ] + " " )
//            grid_string += "\n"
//        }
//        Log.d(TAG, grid_string)

        var count_blue_pieces = 0
        var count_red_pieces = 0

        var count_blue_central_pieces = 0
        var count_red_central_pieces = 0

        var count_blue_border_pieces = 0
        var count_red_border_pieces = 0

        for( row in 0..5 )
            for( col in 0..5 )
            {
                val index = row*6 + col

                if( _simulation_grid[ index ] < 0 )
                    ++count_blue_pieces
                if( _simulation_grid[ index ] > 0 )
                    ++count_red_pieces

                if( row == 0 || row == 5 ) {
                    if( _simulation_grid[ index ] < 0 )
                        ++count_blue_border_pieces
                    if( _simulation_grid[ index ] > 0 )
                        ++count_red_border_pieces
                }
                else {
                    if( col == 0 || col == 5 ) {
                        if( _simulation_grid[ index ] < 0 )
                            ++count_blue_border_pieces
                        if( _simulation_grid[ index ] > 0 )
                            ++count_red_border_pieces
                    }
                    else {
                        if( index == 14 || index == 15 || index == 20 || index == 21 ) {
                            if( _simulation_grid[ index ] < 0 )
                                ++count_blue_central_pieces
                            if( _simulation_grid[ index ] > 0 )
                                ++count_red_central_pieces
                        }
                    }
                }
            }

        // horizontal scans
        for( row in 0..5 )
        {
            var col = 0
            while( col < 5 )
            {
                if( _simulation_grid[ row*6 + col ] != 0.toByte() )
                {
                    val partial_score = compute_partial_score( row, col, Direction.RIGHT )
                    score += partial_score.first
                    col += partial_score.second
                }
                col++
            }
        }

        // vertical scans
        for( col in 0..5 )
        {
            var row = 0
            while( row < 5 )
            {
                if( _simulation_grid[ row*6 + col ] != 0.toByte() )
                {
                    val partial_score = compute_partial_score( row, col, Direction.BOTTOM );
                    score += partial_score.first
                    row += partial_score.second
                }
                row++
            }
        }

        // ascendant diagonal scans
        val ascendant = intArrayOf( 6,
            12, 7,
            18, 13, 8,
            24, 19, 14, 9,
            30, 25, 20, 15, 10,
            31, 26, 21, 16,
            32, 27, 22,
            33, 28,
            34 )

        for( index in ascendant )
            if( _simulation_grid[index] != 0.toByte() )
                score += compute_partial_score( index / 6, index % 6, Direction.TOPRIGHT ).first

        // descendant diagonal scans
        val descendant = intArrayOf( 24,
            18, 25,
            12, 19, 26,
            6, 13, 20, 27,
            0, 7, 14, 21, 28,
            1, 8, 15, 22,
            2, 9, 16,
            3, 10,
            4 )

        for( index in descendant )
            if( _simulation_grid[index] != 0.toByte() )
                score += compute_partial_score( index / 6, index % 6, Direction.BOTTOMRIGHT ).first

        val diff_pieces: Int
        val diff_pieces_central: Int
        val diff_pieces_border: Int

        if( _blue_turn ) {
            diff_pieces = count_blue_pieces - count_red_pieces
            diff_pieces_central = count_blue_central_pieces - count_red_central_pieces
            diff_pieces_border = count_red_border_pieces - count_blue_border_pieces
        }
        else {
            diff_pieces = count_red_pieces - count_blue_pieces
            diff_pieces_central = count_red_central_pieces - count_blue_central_pieces
            diff_pieces_border = count_blue_border_pieces - count_red_border_pieces
        }

        score += 3 * diff_pieces + diff_pieces_central + diff_pieces_border

        return score
    }
}