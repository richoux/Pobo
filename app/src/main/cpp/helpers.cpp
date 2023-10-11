//
// Created by flo on 29/09/2023.
//

#include "helpers.hpp"

//#include <android/log.h>
//#define ALOG(...) __android_log_print(ANDROID_LOG_INFO, "pobotag C++", __VA_ARGS__)

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

double compute_partial_score( int from_row,
															int from_col,
															Direction direction,
															int& jump_forward,
															jbyte * const simulation_grid,
															jboolean blue_turn,
															jbyte * const blue_pool,
															jint& blue_pool_size,
															jbyte * const red_pool,
															jint& red_pool_size )
{
	double score = 0.;
	bool is_player_piece = ( ( simulation_grid[ from_row*6 + from_col ] < 0 && blue_turn ) ||
	                         ( simulation_grid[ from_row*6 + from_col ] > 0 && !blue_turn ) );

	bool do_current_player_has_bo_in_pool = false;
	bool do_opponent_has_bo_in_pool = false;

	for( int i = 0 ; i < blue_pool_size ; ++i )
		if( blue_pool[i] == 2 )
		{
			if( blue_turn )
				do_current_player_has_bo_in_pool = true;
			else
				do_opponent_has_bo_in_pool = true;
		}

	for( int i = 0 ; i < red_pool_size ; ++i )
		if( red_pool[i] == 2 )
		{
			if( blue_turn )
				do_opponent_has_bo_in_pool = true;
			else
				do_current_player_has_bo_in_pool = true;
		}

	if( check_three_in_a_row( from_row, from_col, direction, BO, simulation_grid ))
	{
		score += is_player_piece ? 1000 : -1000;
		jump_forward = 2;
	}
	else
	{
		if( check_three_in_a_row( from_row, from_col, direction, PO, simulation_grid ))
		{
			score += is_player_piece ? 30 : -30;
//			score += is_player_piece ? 3 : -3;
			jump_forward = 2;
		}
		else
		{
			if( check_three_in_a_row( from_row, from_col, direction, WHATEVER, simulation_grid ))
			{
				score += count_Po_in_a_row( from_row, from_col, direction, simulation_grid ) * ( is_player_piece ? 10 : -10 );
//				score += count_Po_in_a_row( from_row, from_col, direction, simulation_grid );
				jump_forward = 1;
			}
			else
			{
				if( check_two_in_a_row( from_row, from_col, direction, BO, simulation_grid ))
				{
					if( is_two_in_a_row_in_corner(from_row, from_col, direction) )
					{
//						score += is_player_piece ? -20 : 20;
						score += is_player_piece ? -30 : 30;
					}
					else
					{
						if( is_two_in_a_row_blocked(from_row, from_col, direction, simulation_grid) )
						{
//							score += is_player_piece ? -2 : 2;
							score += is_player_piece ? -15 : 15;
						}
						else
						{
//							score += is_player_piece ? 100 : -100;
							if( is_player_piece )
							{
								if( do_current_player_has_bo_in_pool )
									score += 60;
								else
									score += 20;
							}
							else
							{
								if( do_opponent_has_bo_in_pool )
									score -= 300; // because there is a severe risk to loose the game
								else
									score -= 20;
							}
						}
					}
					jump_forward = 1;
				}
				else
				{
					if( check_two_in_a_row( from_row, from_col, direction, PO, simulation_grid ))
					{
						if( is_two_in_a_row_in_corner( from_row, from_col, direction ))
						{
//							score += is_player_piece ? -5 : 5;
							score += is_player_piece ? -10 : 10;
						} else
						{
							if( is_two_in_a_row_blocked( from_row, from_col, direction, simulation_grid ))
							{
//								score += is_player_piece ? -1 : 1;
								score += is_player_piece ? -5 : 5;
							} else
							{
//								score += is_player_piece ? 10 : -10;
								score += is_player_piece ? 20 : -20;
							}
						}
					}
					jump_forward = 1;
				}
			}
		}
	}

	return score;
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

bool is_on_border( const std::vector<Position> &group )
{
	bool border = true;

	for( auto pos: group )
	{
		if( pos.row == 0 || pos.row == 5 || pos.column == 0 || pos.column == 5 )
			border = false;
		else
			return true;
	}

	return border;
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
				for( int dir = Direction::TOPRIGHT; dir < Direction::BOTTOM; ++dir )
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


void simulate_move( const std::vector<ghost::Variable *> &variables,
										jbyte * const simulation_grid,
										jboolean blue_turn,
										jbyte * const blue_pool,
										jint & blue_pool_size,
										jbyte * const red_pool,
										jint & red_pool_size )
{
	int p = variables[0]->get_value() * (blue_turn ? -1 : 1);
	int row = variables[1]->get_value();
	int col = variables[2]->get_value();
	int index = row*6 + col;

	simulation_grid[index] = p;

	if( col-1 >= 0 )
	{
		// Top Left
		if( row-1 >= 0 )
		{
			if( simulation_grid[ ( row-1 )*6 + col-1 ] != 0 &&
			    std::abs( simulation_grid[ ( row-1 )*6 + col-1 ] ) <= std::abs( simulation_grid[ index ] ) &&
			    ( col-2 < 0 || row-2 < 0 || simulation_grid[ ( row-2 )*6 + col-2 ] == 0 ))
			{
				if( col-2 >= 0 && row-2 >= 0 && simulation_grid[ ( row-2 )*6 + col-2 ] == 0 )
					simulation_grid[ ( row-2 )*6 + col-2 ] = simulation_grid[ ( row-1 )*6 + col-1 ];
				simulation_grid[ ( row-1 )*6 + col-1 ] = 0;
			}
		}

		// Bottom Left
		if( row+1 <= 5 )
		{
			if( simulation_grid[ ( row+1 )*6 + col-1 ] != 0 &&
			    std::abs( simulation_grid[ ( row+1 )*6 + col-1 ] ) <= std::abs( simulation_grid[ index ] ) &&
			    ( col-2 < 0 || row+2 > 5 || simulation_grid[ ( row+2 )*6 + col-2 ] == 0 ))
			{
				if( col-2 >= 0 && row+2 <= 5 && simulation_grid[ ( row+2 )*6 + col-2 ] == 0 )
					simulation_grid[ ( row+2 )*6 + col-2 ] = simulation_grid[ ( row+1 )*6 + col-1 ];
				simulation_grid[ ( row+1 )*6 + col-1 ] = 0;
			}
		}

		// Left
		if( simulation_grid[ row*6 + col-1 ] != 0 &&
		    std::abs( simulation_grid[ row*6 + col-1 ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( col-2 < 0 || simulation_grid[ row*6 + col-2 ] == 0 ))
		{
			if( col-2 >= 0 && simulation_grid[ row*6 + col-2 ] == 0 )
				simulation_grid[ row*6 + col-2 ] = simulation_grid[ row*6 + col-1 ];
			simulation_grid[ row*6 + col-1 ] = 0;
		}
	}

	if( col+1 <= 5 )
	{
		// Top Right
		if( row-1 >= 0 )
		{
			if( simulation_grid[ ( row-1 )*6 + col+1 ] != 0 &&
			    std::abs( simulation_grid[ ( row-1 )*6 + col+1 ] ) <= std::abs( simulation_grid[ index ] ) &&
			    ( col+2 > 5 || row-2 < 0 || simulation_grid[ ( row-2 )*6 + col+2 ] == 0 ))
			{
				if( col+2 <= 5 && row-2 >= 0 && simulation_grid[ ( row-2 )*6 + col+2 ] == 0 )
					simulation_grid[ ( row-2 )*6 + col+2 ] = simulation_grid[ ( row-1 )*6 + col+1 ];
				simulation_grid[ ( row-1 )*6 + col+1 ] = 0;
			}
		}

		// Bottom Right
		if( row+1 <= 5 )
		{
			if( simulation_grid[ ( row+1 )*6 + col+1 ] != 0 &&
			    std::abs( simulation_grid[ ( row+1 )*6 + col+1 ] ) <= std::abs( simulation_grid[ index ] ) &&
			    ( col+2 > 5 || row+2 > 5 || simulation_grid[ ( row+2 )*6 + col+2 ] == 0 ) )
			{
				if( col+2 <= 5 && row+2 <= 5 && simulation_grid[ ( row+2 )*6 + col+2 ] == 0 )
					simulation_grid[ ( row+2 )*6 + col+2 ] = simulation_grid[ ( row+1 )*6 + col+1 ];
				simulation_grid[ ( row+1 )*6 + col+1 ] = 0;
			}
		}

		// Right
		if( simulation_grid[ row*6 + col+1 ] != 0 &&
		    std::abs( simulation_grid[ row*6 + col+1 ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( col+2 > 5 || simulation_grid[ row*6 + col+2 ] == 0 ))
		{
			if( col+2 <= 5 && simulation_grid[ row*6 + col+2 ] == 0 )
				simulation_grid[ row*6 + col+2 ] = simulation_grid[ row*6 + col+1 ];
			simulation_grid[ row*6 + col+1 ] = 0;
		}
	}

	// Top
	if( row-1 >= 0 )
	{
		if( simulation_grid[ ( row-1 )*6 + col ] != 0 &&
		    std::abs( simulation_grid[ ( row-1 )*6 + col ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( row-2 < 0 || simulation_grid[ ( row-2 )*6 + col ] == 0 ))
		{
			if( row-2 >= 0 && simulation_grid[ ( row-2 )*6 + col ] == 0 )
				simulation_grid[ ( row-2 )*6 + col ] = simulation_grid[ ( row-1 )*6 + col ];
			simulation_grid[ ( row-1 )*6 + col ] = 0;
		}
	}

	// Bottom
	if( row+1 <= 5 )
	{
		if( simulation_grid[ ( row+1 )*6 + col ] != 0 &&
		    std::abs( simulation_grid[ ( row+1 )*6 + col ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( row+2 > 5 || simulation_grid[ ( row+2 )*6 + col ] == 0 ))
		{
			if( row+2 <= 5 && simulation_grid[ ( row+2 )*6 + col ] == 0 )
				simulation_grid[ ( row+2 )*6 + col ] = simulation_grid[ ( row+1 )*6 + col ];
			simulation_grid[ ( row+1 )*6 + col ] = 0;
		}
	}

	auto graduations = get_graduations( simulation_grid, blue_turn, blue_pool_size, red_pool_size );
	if( graduations.size() > 0 )
	{

	}

	//TODO: need to check graduations T_T
}

