//
// Created by flo on 29/09/2023.
//

#include "helpers.hpp"

#include <android/log.h>
//*
#define ALOG(...)
/*/
#define ALOG( ... ) __android_log_print(ANDROID_LOG_INFO, "pobotag C++", __VA_ARGS__)
//*/

bool check_three_in_a_row( int from_row,
													 int from_col,
													 Direction direction,
													 PieceType type,
													 jbyte * const simulation_grid )
{
	if( from_col > 5 || from_row < 0 || from_row > 5 )
		return false;

	int next_row = get_next_row( from_row, direction );
	int next_col = get_next_col( from_col, direction );

	if( simulation_grid[ from_row*6 + from_col ] == 0 || next_col > 5 || next_row < 0 || next_row > 5 )
		return false;

	return check_two_in_a_row( from_row, from_col, direction, type, simulation_grid )
	       && check_two_in_a_row( next_row, next_col, direction, type, simulation_grid );
}

bool check_two_in_a_row( int from_row,
												 int from_col,
												 Direction direction,
												 PieceType type,
												 jbyte * const simulation_grid )
{
	if( from_col > 5 || from_row < 0 || from_row > 5 )
		return false;

	int next_row = get_next_row( from_row, direction );
	int next_col = get_next_col( from_col, direction );

	int index = from_row*6 + from_col;

	if( simulation_grid[index] == 0 || next_col > 5 || next_row < 0 || next_row > 5 )
		return false;

	int next_index = next_row*6 + next_col;

	switch( type )
	{
		case WHATEVER:
			return simulation_grid[index] * simulation_grid[next_index] > 0;
		case PO:
			return simulation_grid[index] == simulation_grid[next_index] && std::abs( simulation_grid[index] ) == 1;
		default: // BO
			return simulation_grid[index] == simulation_grid[next_index] && std::abs( simulation_grid[index] ) == 2;
	}
}

bool is_two_in_a_row_in_corner( int from_row,
																int from_col,
																Direction direction )
{
	return ( direction == TOPRIGHT && ( (from_row == 1 && from_col == 0) || (from_row == 5 && from_col == 4) ) )
			|| ( direction == BOTTOMRIGHT && ( (from_row == 4 && from_col == 0) || (from_row == 0 && from_col == 4) ) );
}

bool is_two_in_a_row_blocked( int from_row,
															int from_col,
															Direction direction,
															jbyte * const simulation_grid )
{
	bool is_blocked = false;
	int piece = simulation_grid[ from_row*6 + from_col ];

	switch( direction )
	{
		case TOPRIGHT:
			if( ( from_row + 1 > 5 || from_col - 1 < 0 || simulation_grid[ (from_row+1)*6 + (from_col-1) ]*piece < 0)
			&& ( from_row - 2 < 0 || from_col + 2 > 5 || simulation_grid[ (from_row-2)*6 + (from_col+2) ]*piece < 0) )
				is_blocked = true;
			break;
		case RIGHT:
			if( ( from_col - 1 < 0 || simulation_grid[ from_row*6 + (from_col-1) ]*piece < 0)
			    && ( from_col + 2 > 5 || simulation_grid[ from_row*6 + (from_col+2) ]*piece < 0) )
				is_blocked = true;
			break;
		case BOTTOMRIGHT:
			if( ( from_row - 1 < 0 || from_col - 1 < 0 || simulation_grid[ (from_row-1)*6 + (from_col-1) ]*piece < 0)
			    && ( from_row + 2 > 5 || from_col + 2 > 5 || simulation_grid[ (from_row+2)*6 + (from_col+2) ]*piece < 0) )
				is_blocked = true;
			break;
		default: // BOTTOM
			if( ( from_row - 1 < 0 || simulation_grid[ (from_row-1)*6 + from_col ]*piece < 0)
			    && ( from_row + 2 > 5 || simulation_grid[ (from_row+2)*6 + from_col ]*piece < 0) )
				is_blocked = true;
	}

	return is_blocked;
}

int count_Po_in_a_row( int from_row,
											 int from_col,
											 Direction direction,
											 jbyte * const simulation_grid )
{
	if( from_col > 5 || from_row < 0 || from_row > 5 )
		return 0;

	int next_row = get_next_row( from_row, direction );
	int next_col = get_next_col( from_col, direction );
	int index = from_row*6 + from_col;

	if( simulation_grid[index] == 0 || next_col > 5 || next_row < 0 || next_row > 5 )
		return 0;

	int next_next_row = get_next_row( next_row, direction );
	int next_next_col = get_next_col( next_col, direction );
	int next_index = next_row*6 + next_col;

	if( simulation_grid[next_index] == 0 || next_next_col > 5 || next_next_row < 0 || next_next_row > 5 )
		return 0;

	int next_next_index = next_next_row*6 + next_next_col;

	int count = 0;
	// no need to check the color of these Po pieces: count_Po_in_a_row is only called after
	// checking if pieces are of the same color, with check_three_in_a_row
	if( std::abs( simulation_grid[index] ) == 1 )
		++count;
	if( std::abs( simulation_grid[next_index] ) == 1 )
		++count;
	if( std::abs( simulation_grid[next_next_index] ) == 1 )
		++count;

	return count;
}

int get_next_row( int from_row, Direction direction )
{
	switch( direction )
	{
		case TOPRIGHT:
			return from_row - 1;
		case RIGHT :
			return from_row;
		case BOTTOMRIGHT :
			return from_row + 1;
		case BOTTOM :
			return from_row + 1;
	}
}

int get_next_col( int from_col, Direction direction )
{
	switch( direction )
	{
		case TOPRIGHT:
			return from_col + 1;
		case RIGHT :
			return from_col + 1;
		case BOTTOMRIGHT :
			return from_col + 1;
		case BOTTOM :
			return from_col;
	}
}

Position get_position_toward( const Position &position, Direction direction )
{
	switch( direction )
	{
		case TOPRIGHT:
			return Position( position.row - 1, position.column + 1 );
		case RIGHT:
			return Position( position.row, position.column + 1 );
		case BOTTOMRIGHT:
			return Position( position.row + 1, position.column + 1 );
		default: //BOTTOM
			return Position( position.row + 1, position.column );
	}
}

Position get_position_toward( const Position &position, int direction )
{
	return get_position_toward( position, static_cast<Direction>( direction ) );
}

bool is_valid_position( const Position &position )
{
	return position.row >= 0 && position.row <= 5 && position.column >= 0 && position.column <= 5;
}

bool is_valid_position( int row, int col )
{
	return row >= 0 && row <= 5 && col >= 0 && col <= 5;
}

bool is_empty_position( jbyte * const simulation_grid, int row, int col )
{
	return simulation_grid[ 6*row + col ] == 0;
}

bool is_empty_position( jbyte * const simulation_grid, const Position &position )
{
	return is_empty_position( simulation_grid, position.row, position.column );
}

bool is_blue_piece_on( jbyte * const simulation_grid, int row, int col )
{
	return simulation_grid[ 6*row + col ] < 0;
}

bool is_blue_piece_on( jbyte * const simulation_grid, const Position &position )
{
	return is_blue_piece_on( simulation_grid, position.row, position.column );
}

bool is_fully_on_border( const std::vector<Position> &group )
{
	bool border = false;

	for( auto pos: group )
	{
		if( pos.row == 0 || pos.row == 5 || pos.column == 0 || pos.column == 5 )
			border = true;
		else
			return false;
	}

	return border;
}

bool is_partially_on_border( const std::vector<Position> &group )
{
	for( auto pos: group )
		if( pos.row == 0 || pos.row == 5 || pos.column == 0 || pos.column == 5 )
			return true;

	return false;
}

bool is_on_border( int from_row,
                   int from_col,
                   Direction direction,
                   int length,
                   bool fully )
{
	if( length < 0 || length > 3 )
		return false;

	std::vector<Position> group;
	group.emplace_back( from_col, from_row );

	int next_row;
	int next_col;
	if( length >= 2 )
	{
		next_row = get_next_row( from_row, direction );
		next_col = get_next_col( from_col, direction );
		group.emplace_back( next_row, next_col );
	}

	if( length == 3 )
	{
		int next_next_row = get_next_row( next_row, direction );
		int next_next_col = get_next_col( next_col, direction );
		group.emplace_back( next_next_row, next_next_col );
	}

	return fully ? is_fully_on_border( group ) : is_partially_on_border( group );
}

bool is_in_center( const Position &position )
{
	return position.row >= 2 && position.row <= 3 && position.column >= 2 && position.column <= 3;
}

bool next_to_other_own_pieces( jbyte * const simulation_grid, const Position &position )
{
	auto piece = simulation_grid[ 6*position.row + position.column ];
	for( int row = position.row - 1 ; row <= position.row + 1 ; ++row )
		for( int col = position.column - 1 ; col <= position.column + 1 ; ++col )
		{
			if( row == position.row && col == position.column )
				continue;
			if( is_valid_position( Position( row, col ) ) )
			{
				ALOG("Position (%d,%d) is valid\n", row, col);
				if( simulation_grid[ 6 * row + col ] * piece > 0 ) // if we have 2 consecutive pieces of our player
				{
					ALOG("Piece at (%d,%d) is next to a friend at (%d,%d)\n", position.row, position.column, row, col);
					return true;
				}
			}
		}

	ALOG("Piece at (%d,%d) is alone in the dark\n", position.row, position.column);
	return false;
}

bool is_blocking( jbyte * const simulation_grid, int row, int col )
{
	// Top
	if( is_valid_position( row-1, col ) && is_valid_position( row-2, col ) )
	{
		if ( simulation_grid[ row*6 + col ] * simulation_grid[ (row-1)*6 + col ] < 0
		     && simulation_grid[ row*6 + col ] * simulation_grid[ (row-2)*6 + col ] < 0 )
			return true;
	}

	// Top Right
	if( is_valid_position( row-1, col+1 ) && is_valid_position( row-2, col+2 ) )
	{
		if( simulation_grid[ row*6 + col ] * simulation_grid[ (row-1)*6 + col+1 ] < 0
		    && simulation_grid[ row*6 + col ] * simulation_grid[ (row-2)*6 + col+2 ] < 0 )
			return true;
	}

	// Right
	if( is_valid_position( row, col+1 ) && is_valid_position( row, col+2 ) )
	{
		if( simulation_grid[ row*6 + col ] * simulation_grid[ row*6 + col+1 ] < 0
		    && simulation_grid[ row*6 + col ] * simulation_grid[ row*6 + col+2 ] < 0 )
			return true;
	}

	// Bottom Right
	if( is_valid_position( row+1, col+1 ) && is_valid_position( row+2, col+2 ) )
	{
		if( simulation_grid[ row*6 + col ] * simulation_grid[ (row+1)*6 + col+1 ] < 0
		    && simulation_grid[ row*6 + col ] * simulation_grid[ (row+2)*6 + col+2 ] < 0 )
			return true;
	}

	// Bottom
	if( is_valid_position( row+1, col ) && is_valid_position( row+2, col ) )
	{
		if( simulation_grid[ row*6 + col ] * simulation_grid[ (row+1)*6 + col ] < 0
		    && simulation_grid[ row*6 + col ] * simulation_grid[ (row+2)*6 + col ] < 0 )
			return true;
	}

	// Bottom Left
	if( is_valid_position( row+1, col-1 ) && is_valid_position( row+2, col-2 ) )
	{
		if( simulation_grid[ row*6 + col ] * simulation_grid[ (row+1)*6 + col-1 ] < 0
		    && simulation_grid[ row*6 + col ] * simulation_grid[ (row+2)*6 + col-2 ] < 0 )
			return true;
	}

	// Left
	if( is_valid_position( row, col-1 ) && is_valid_position( row, col-2 ) )
	{
		if( simulation_grid[ row*6 + col ] * simulation_grid[ row*6 + col-1 ] < 0
		    && simulation_grid[ row*6 + col ] * simulation_grid[ row*6 + col-2 ] < 0 )
			return true;
	}

	// Top Left
	if( is_valid_position( row-1, col-1 ) && is_valid_position( row-2, col-2 ) )
	{
		if( simulation_grid[ row*6 + col ] * simulation_grid[ (row-1)*6 + col-1 ] < 0
		    && simulation_grid[ row*6 + col ] * simulation_grid[ (row-2)*6 + col-2 ] < 0 )
			return true;
	}

	return false;
}

bool is_blocking( jbyte * const simulation_grid, const Position &position )
{
	return is_blocking( simulation_grid, position.row, position.column );
}

std::vector< std::vector<Position> > get_graduations( jbyte * const simulation_grid,
                                                      jboolean blue_turn,
                                                      jint blue_pool_size,
                                                      jint red_pool_size )
{
	std::vector< std::vector<Position> > grads;
	for( int row = 0 ; row < 6 ; ++row )
		for( int col = 0 ; col < 6 ; ++col )
		{
			int piece = simulation_grid[ 6 * row + col ];
			if((blue_turn && piece < 0) || (!blue_turn && piece > 0))
			{
				if((blue_turn && blue_pool_size == 0) || (!blue_turn && red_pool_size == 0))
				{
					grads.emplace_back( std::vector<Position>{Position( row, col )} );
				}
				for( int dir = Direction::TOPRIGHT; dir <= Direction::BOTTOM; ++dir )
				{
					Position next = get_position_toward( Position( row, col ), dir );
					Position next_next = get_position_toward( next, dir );

					if( is_valid_position( next )
					    && !is_empty_position( simulation_grid, next ))
					{
						if((blue_turn && is_blue_piece_on( simulation_grid, next ))
						   || (!blue_turn && !is_blue_piece_on( simulation_grid, next )))
						{
							if( is_valid_position( next_next )
							    && !is_empty_position( simulation_grid, next_next ))
							{
								if((blue_turn && is_blue_piece_on( simulation_grid, next_next ))
								   || (!blue_turn && !is_blue_piece_on( simulation_grid, next_next )))
								{
									grads.emplace_back(
													std::vector<Position>{Position( row, col ), next, next_next} );
								}
							}
						}
					}
				}
			}
		}

	return grads;
}


