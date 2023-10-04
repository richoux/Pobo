//
// Created by flo on 29/09/2023.
//

#include "heuristics.hpp"

bool check_three_in_a_row( int from_row, int from_col, Direction direction, PieceType type, jbyte * const simulation_grid )
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

bool check_two_in_a_row( int from_row, int from_col, Direction direction, PieceType type, jbyte * const simulation_grid )
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

bool is_two_in_a_row_in_corner( int from_row, int from_col, Direction direction )
{
	return ( direction == TOPRIGHT && ( (from_row == 1 && from_col == 0) || (from_row == 5 && from_col == 4) ) )
			|| ( direction == BOTTOMRIGHT && ( (from_row == 4 && from_col == 0) || (from_row == 0 && from_col == 4) ) );
}

bool is_two_in_a_row_blocked( int from_row, int from_col, Direction direction, jbyte * const simulation_grid )
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

int count_Po_in_a_row( int from_row, int from_col, Direction direction, jbyte * const simulation_grid )
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

double compute_partial_score( int from_row, int from_col, Direction direction, int& jump_forward, jbyte * const simulation_grid, jboolean blue_turn )
{
	double score = 0.;
	bool is_player_piece = ( ( simulation_grid[ from_row*6 + from_col ] < 0 && blue_turn ) ||
	                         ( simulation_grid[ from_row*6 + from_col ] > 0 && !blue_turn ) );

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
							score += is_player_piece ? 60 : -60;
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

void simulate_move( const std::vector<ghost::Variable *> &variables, jbyte * const simulation_grid, jboolean blue_turn )
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
}

double heuristic( jbyte * const simulation_grid, jboolean blue_turn ) {
	double score = 0.0;

	int count_blue_po = 0;
	int count_blue_bo = 0;
	int count_red_po = 0;
	int count_red_bo = 0;

	int count_blue_central_po = 0;
	int count_blue_central_bo = 0;
	int count_red_central_po = 0;
	int count_red_central_bo = 0;

	int count_blue_border_po = 0;
	int count_blue_border_bo = 0;
	int count_red_border_po = 0;
	int count_red_border_bo = 0;

	for( int row = 0 ; row < 6 ; ++row )
		for( int col = 0 ; col < 6 ; ++col )
		{
			int index = row*6 + col;

			if( simulation_grid[ index ] == -1 )
				++count_blue_po;
			if( simulation_grid[ index ] == -2 )
				++count_blue_bo;
			if( simulation_grid[ index ] == 1 )
				++count_red_po;
			if( simulation_grid[ index ] == 2 )
				++count_red_bo;

			if( row == 0 || row == 5 )
			{
				if( simulation_grid[ index ] == -1 )
					++count_blue_border_po;
				if( simulation_grid[ index ] == -2 )
					++count_blue_border_bo;
				if( simulation_grid[ index ] == 1 )
					++count_red_border_po;
				if( simulation_grid[ index ] == 2 )
					++count_red_border_bo;
			}
			else {
				if( col == 0 || col == 5 ) {
					if( simulation_grid[ index ] == -1 )
						++count_blue_border_po;
					if( simulation_grid[ index ] == -2 )
						++count_blue_border_bo;
					if( simulation_grid[ index ] == 1 )
						++count_red_border_po;
					if( simulation_grid[ index ] == 2 )
						++count_red_border_bo;
				}
				else {
					if( index == 14 || index == 15 || index == 20 || index == 21 ) {
						if( simulation_grid[ index ] == -1 )
							++count_blue_central_po;
						if( simulation_grid[ index ] == -2 )
							++count_blue_central_bo;
						if( simulation_grid[ index ] == 1 )
							++count_red_central_po;
						if( simulation_grid[ index ] == 2 )
							++count_red_central_bo;
					}
				}
			}
		}

	int jump_forward;
	// horizontal scans
	for( int row = 0; row < 6; ++row )
	{
		for( int col = 0; col < 5; col = col + 1 + jump_forward )
		{
			jump_forward = 0;
			if( simulation_grid[row * 6 + col] != 0 )
			{
				auto partial_score = compute_partial_score( row, col, RIGHT, jump_forward, simulation_grid, blue_turn );
				score += partial_score;
//					std::cout << "Score horizontal scan from (" << row << "," << col << "): "
//									  << partial_score << ", jump=" << jump_forward << "\n";
			}
		}
	}

	// vertical scans
	for( int col = 0; col < 6; ++col )
	{
		for( int row = 0; row < 5; row = row + 1 + jump_forward )
		{
			jump_forward = 0;
			if( simulation_grid[ row*6 + col ] != 0 )
			{
				auto partial_score= compute_partial_score( row, col, BOTTOM, jump_forward, simulation_grid, blue_turn );
				score += partial_score;
//					std::cout << "Score horizontal scan from (" << row << "," << col << "): "
//					          << partial_score << ", jump=" << jump_forward << "\n";
			}
		}
	}

	// ascendant diagonal scans
	std::vector<int> ascendant{ 6,
	                            12, 7,
	                            18, 13, 8,
	                            24, 19, 14, 9,
	                            30, 25, 20, 15, 10,
	                            31, 26, 21, 16,
	                            32, 27, 22,
	                            33, 28,
	                            34 };
	int fake_jump;
	for( auto index: ascendant )
		if( simulation_grid[index] != 0 )
		{
			auto partial_score = compute_partial_score( index / 6, index % 6, TOPRIGHT, fake_jump, simulation_grid, blue_turn );
			score += partial_score;
//				std::cout << "Score horizontal scan from (" << index / 6 << "," << index % 6 << "): "
//				          << partial_score << ", jump=" << jump_forward << "\n";
		}

	// descendant diagonal scans
	std::vector<int> descendant{ 24,
	                             18, 25,
	                             12, 19, 26,
	                             6, 13, 20, 27,
	                             0, 7, 14, 21, 28,
	                             1, 8, 15, 22,
	                             2, 9, 16,
	                             3, 10,
	                             4 };

	for( auto index: descendant )
		if( simulation_grid[index] != 0 )
		{
			auto partial_score = compute_partial_score( index / 6, index % 6, BOTTOMRIGHT, fake_jump, simulation_grid, blue_turn );
			score += partial_score;
//				std::cout << "Score horizontal scan from (" << index / 6 << "," << index % 6 << "): "
//				          << partial_score << ", jump=" << jump_forward << "\n";
		}

	int diff_po = 0;
	int diff_bo = 0;
	int diff_po_central = 0;
	int diff_bo_central = 0;
	int diff_po_border = 0;
	int diff_bo_border = 0;

	if( blue_turn )
	{
		diff_po = count_blue_po - count_red_po;
		diff_bo = count_blue_bo - count_red_bo;

		diff_po_central = count_blue_central_po - count_red_central_po;
		diff_bo_central = count_blue_central_bo - count_red_central_bo;

		diff_po_border = count_red_border_po - count_blue_border_po;
		diff_bo_border = count_red_border_bo - count_blue_border_bo;
	}
	else
	{
		diff_po = count_red_po - count_blue_po;
		diff_bo = count_red_bo - count_blue_bo;

		diff_po_central = count_red_central_po - count_blue_central_po;
		diff_bo_central = count_red_central_bo - count_blue_central_bo;

		diff_po_border = count_blue_border_po - count_red_border_po;
		diff_bo_border = count_blue_border_bo - count_red_border_bo;
	}

	score += 3 * (diff_bo + diff_bo_central + diff_bo_border) + diff_po + diff_po_central + diff_po_border;
	return score;
}