//
// Created by flo on 21/06/2023.
//

#include "pobo_heuristic.hpp"
//#include "../androidbuf.hpp"

PoboHeuristic::PoboHeuristic( const std::vector<ghost::Variable>& variables, jbyte * const grid, jboolean blue_turn )
				: Maximize( variables, "pobo Heuristic" ),
				  _grid( grid ),
				  _blue_turn( blue_turn )
{ }

bool PoboHeuristic::check_three_in_a_row( int from_row, int from_col, Direction direction, PieceType type ) const
{
	if( from_col > 5 || from_row < 0 || from_row > 5 )
		return false;

	int next_row = get_next_row( from_row, direction );
	int next_col = get_next_col( from_col, direction );

	if( _simulation_grid[ from_row*6 + from_col ] == 0 || next_col > 5 || next_row < 0 || next_row > 5 )
		return false;

	return check_two_in_a_row( from_row, from_col, direction, type )
	       && check_two_in_a_row( next_row, next_col, direction, type );
}

bool PoboHeuristic::check_two_in_a_row( int from_row, int from_col, Direction direction, PieceType type ) const
{
	if( from_col > 5 || from_row < 0 || from_row > 5 )
		return false;

	int next_row = get_next_row( from_row, direction );
	int next_col = get_next_col( from_col, direction );

	int index = from_row*6 + from_col;

	if( _simulation_grid[index] == 0 || next_col > 5 || next_row < 0 || next_row > 5 )
		return false;

	int next_index = next_row*6 + next_col;

	switch( type )
	{
		case WHATEVER:
			return _simulation_grid[index] * _simulation_grid[next_index] > 0;
		default:
			return _simulation_grid[index] == _simulation_grid[next_index];
	}
}

int PoboHeuristic::count_Po_in_a_row( int from_row, int from_col, Direction direction ) const
{
	if( from_col > 5 || from_row < 0 || from_row > 5 )
		return 0;

	int next_row = get_next_row( from_row, direction );
	int next_col = get_next_col( from_col, direction );
	int index = from_row*6 + from_col;

	if( _simulation_grid[index] == 0 || next_col > 5 || next_row < 0 || next_row > 5 )
		return 0;

	int next_next_row = get_next_row( next_row, direction );
	int next_next_col = get_next_col( next_col, direction );
	int next_index = next_row*6 + next_col;

	if( _simulation_grid[next_index] == 0 || next_next_col > 5 || next_next_row < 0 || next_next_row > 5 )
		return 0;

	int next_next_index = next_next_row*6 + next_next_col;

	int count = 0;
	// no need to check the color of these Po pieces: count_Po_in_a_row is only called after
	// checking if pieces are of the same color, with check_three_in_a_row
	if( std::abs( _simulation_grid[index] ) == 1 )
		++count;
	if( std::abs( _simulation_grid[next_index] ) == 1 )
		++count;
	if( std::abs( _simulation_grid[next_next_index] ) == 1 )
		++count;

	return count;
}

int PoboHeuristic::get_next_row( int from_row, Direction direction ) const
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

int PoboHeuristic::get_next_col( int from_col, Direction direction ) const
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

double PoboHeuristic::compute_partial_score( int from_row, int from_col, Direction direction, int& jump_forward ) const
{
	double score = 0.;
	bool is_player_piece = ( ( _simulation_grid[ from_row*6 + from_col ] < 0 && _blue_turn ) ||
					( _simulation_grid[ from_row*6 + from_col ] > 0 && !_blue_turn ) );

	if( check_three_in_a_row( from_row, from_col, direction, BO ))
	{
		score += is_player_piece ? 200 : -200;
		jump_forward = 2;
	}
	else
	{
		if( check_three_in_a_row( from_row, from_col, direction, PO ))
		{
			score += is_player_piece ? 30 : -30;
			jump_forward = 2;
		}
		else
		{
			if( check_three_in_a_row( from_row, from_col, direction, WHATEVER ))
			{
				score += count_Po_in_a_row( from_row, from_col, direction ) * ( is_player_piece ? 10 : -10 );
				jump_forward = 1;
			}
			else
			{
				if( check_two_in_a_row( from_row, from_col, direction, BO ))
				{
					score += is_player_piece ? 20 : -100;
					jump_forward = 1;
				}
				else
				{
					if( check_two_in_a_row( from_row, from_col, direction, PO ))
					{
						score += is_player_piece ? 5 : -10;
						jump_forward = 1;
					}
				}
			}
		}
	}

	return score;
}

void PoboHeuristic::simulate_move( const std::vector<ghost::Variable *> &variables ) const
{
	int p = variables[0]->get_value() * (_blue_turn ? -1 : 1);
	int row = variables[1]->get_value();
	int col = variables[2]->get_value();
	int index = row*6 + col;

	_simulation_grid[index] = p;

	if( col-1 >= 0 )
	{
		// Top Left
		if( row-1 >= 0 )
		{
			if( _simulation_grid[ ( row-1 )*6 + col-1 ] != 0 &&
					_simulation_grid[ ( row-1 )*6 + col-1 ] <= _simulation_grid[ index ] &&
			    ( col-2 < 0 || row-2 < 0 || _simulation_grid[ ( row-2 )*6 + col-2 ] == 0 ))
			{
				if( col-2 >= 0 && row-2 >= 0 && _simulation_grid[ ( row-2 )*6 + col-2 ] == 0 )
					_simulation_grid[ ( row-2 )*6 + col-2 ] = _simulation_grid[ ( row-1 )*6 + col-1 ];
				_simulation_grid[ ( row-1 )*6 + col-1 ] = 0;
			}
		}

		// Bottom Left
		if( row+1 <= 5 )
		{
			if( _simulation_grid[ ( row+1 )*6 + col-1 ] != 0 &&
			    _simulation_grid[ ( row+1 )*6 + col-1 ] <= _simulation_grid[ index ] &&
			    ( col-2 < 0 || row+2 > 5 || _simulation_grid[ ( row+2 )*6 + col-2 ] == 0 ))
			{
				if( col-2 >= 0 && row+2 <= 5 && _simulation_grid[ ( row+2 )*6 + col-2 ] == 0 )
					_simulation_grid[ ( row+2 )*6 + col-2 ] = _simulation_grid[ ( row+1 )*6 + col-1 ];
				_simulation_grid[ ( row+1 )*6 + col-1 ] = 0;
			}
		}

		// Left
		if( _simulation_grid[ row*6 + col-1 ] != 0 &&
		    _simulation_grid[ row*6 + col-1 ] <= _simulation_grid[ index ] &&
		    ( col-2 < 0 || _simulation_grid[ row*6 + col-2 ] == 0 ))
		{
			if( col-2 >= 0 && _simulation_grid[ row*6 + col-2 ] == 0 )
				_simulation_grid[ row*6 + col-2 ] = _simulation_grid[ row*6 + col-1 ];
			_simulation_grid[ row*6 + col-1 ] = 0;
		}
	}

	if( col+1 <= 5 )
	{
		// Top Right
		if( row-1 >= 0 )
		{
			if( _simulation_grid[ ( row-1 )*6 + col+1 ] != 0 &&
			    _simulation_grid[ ( row-1 )*6 + col+1 ] <= _simulation_grid[ index ] &&
			    ( col+2 > 5 || row-2 < 0 || _simulation_grid[ ( row-2 )*6 + col+2 ] == 0 ))
			{
				if( col+2 <= 5 && row-2 >= 0 && _simulation_grid[ ( row-2 )*6 + col+2 ] == 0 )
					_simulation_grid[ ( row-2 )*6 + col+2 ] = _simulation_grid[ ( row-1 )*6 + col+1 ];
				_simulation_grid[ ( row-1 )*6 + col+1 ] = 0;
			}
		}

		// Bottom Right
		if( row+1 <= 5 )
		{
			if( _simulation_grid[ ( row+1 )*6 + col+1 ] != 0 &&
			    _simulation_grid[ ( row+1 )*6 + col+1 ] <= _simulation_grid[ index ] &&
			    ( col+2 > 5 || row+2 > 5 || _simulation_grid[ ( row+2 )*6 + col+2 ] == 0 ) )
			{
				if( col+2 <= 5 && row+2 <= 5 && _simulation_grid[ ( row+2 )*6 + col+2 ] == 0 )
					_simulation_grid[ ( row+2 )*6 + col+2 ] = _simulation_grid[ ( row+1 )*6 + col+1 ];
				_simulation_grid[ ( row+1 )*6 + col+1 ] = 0;
			}
		}

		// Right
		if( _simulation_grid[ row*6 + col+1 ] != 0 &&
		    _simulation_grid[ row*6 + col+1 ] <= _simulation_grid[ index ] &&
		    ( col+2 > 5 || _simulation_grid[ row*6 + col+2 ] == 0 ))
		{
			if( col+2 <= 5 && _simulation_grid[ row*6 + col+2 ] == 0 )
				_simulation_grid[ row*6 + col+2 ] = _simulation_grid[ row*6 + col+1 ];
			_simulation_grid[ row*6 + col+1 ] = 0;
		}
	}

	// Top
	if( row-1 >= 0 )
	{
		if( _simulation_grid[ ( row-1 )*6 + col ] != 0 &&
		    _simulation_grid[ ( row-1 )*6 + col ] <= _simulation_grid[ index ] &&
		    ( row-2 < 0 || _simulation_grid[ ( row-2 )*6 + col ] == 0 ))
		{
			if( row-2 >= 0 && _simulation_grid[ ( row-2 )*6 + col ] == 0 )
				_simulation_grid[ ( row-2 )*6 + col ] = _simulation_grid[ ( row-1 )*6 + col ];
			_simulation_grid[ ( row-1 )*6 + col ] = 0;
		}
	}

	 // Bottom
	if( row+1 <= 5 )
	{
		if( _simulation_grid[ ( row+1 )*6 + col ] != 0 &&
		    _simulation_grid[ ( row+1 )*6 + col ] <= _simulation_grid[ index ] &&
		    ( row+2 > 5 || _simulation_grid[ ( row+2 )*6 + col ] == 0 ))
		{
			if( row+2 <= 5 && _simulation_grid[ ( row+2 )*6 + col ] == 0 )
				_simulation_grid[ ( row+2 )*6 + col ] = _simulation_grid[ ( row+1 )*6 + col ];
			_simulation_grid[ ( row+1 )*6 + col ] = 0;
		}
	}
}

double PoboHeuristic::required_cost( const std::vector<ghost::Variable *> &variables ) const
{
//	std::cout.rdbuf(new androidbuf); // to redirect std::cout to android logs

	double score = 0.;

	for( int i = 0; i < 36; ++i )
		_simulation_grid[i] = _grid[i];

	simulate_move( variables );

	int count_blue_pieces = 0;
	int count_red_pieces = 0;

	int count_blue_central_pieces = 0;
	int count_red_central_pieces = 0;

	int count_blue_border_pieces = 0;
	int count_red_border_pieces = 0;

	for( int row = 0 ; row < 6 ; ++row )
		for( int col = 0 ; col < 6 ; ++col )
		{
			int index = row*6 + col;

			if( _simulation_grid[ index ] < 0 )
				++count_blue_pieces;
			if( _simulation_grid[ index ] > 0 )
				++count_red_pieces;

			if( row == 0 || row == 5 )
			{
				if( _simulation_grid[ index ] < 0 )
					++count_blue_border_pieces;
				if( _simulation_grid[ index ] > 0 )
					++count_red_border_pieces;
			}
			else {
				if( col == 0 || col == 5 ) {
					if( _simulation_grid[ index ] < 0 )
						++count_blue_border_pieces;
					if( _simulation_grid[ index ] > 0 )
						++count_red_border_pieces;
				}
				else {
					if( index == 14 || index == 15 || index == 20 || index == 21 ) {
						if( _simulation_grid[ index ] < 0 )
							++count_blue_central_pieces;
						if( _simulation_grid[ index ] > 0 )
							++count_red_central_pieces;
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
			if( _simulation_grid[row * 6 + col] != 0 )
			{
				auto partial_score = compute_partial_score( row, col, RIGHT, jump_forward );
				if( partial_score > 0 )
				{
					score += partial_score;
//					std::cout << "Score horizontal scan from (" << row << "," << col << "): "
//									  << partial_score << ", jump=" << jump_forward << "\n";
				}
			}
		}
	}

	// vertical scans
	for( int col = 0; col < 6; ++col )
	{
		for( int row = 0; row < 5; row = row + 1 + jump_forward )
		{
			jump_forward = 0;
			if( _simulation_grid[ row*6 + col ] != 0 )
			{
				auto partial_score= compute_partial_score( row, col, BOTTOM, jump_forward );
				if( partial_score > 0 )
				{
					score += partial_score;
//					std::cout << "Score horizontal scan from (" << row << "," << col << "): "
//					          << partial_score << ", jump=" << jump_forward << "\n";
				}
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
		if( _simulation_grid[index] != 0 )
		{
			auto partial_score = compute_partial_score( index / 6, index % 6, TOPRIGHT, fake_jump );
			if( partial_score > 0 )
			{
				score += partial_score;
//				std::cout << "Score horizontal scan from (" << index / 6 << "," << index % 6 << "): "
//				          << partial_score << ", jump=" << jump_forward << "\n";
			}
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
		if( _simulation_grid[index] != 0 )
		{
			auto partial_score = compute_partial_score( index / 6, index % 6, BOTTOMRIGHT, fake_jump );
			if( partial_score > 0 )
			{
				score += partial_score;
//				std::cout << "Score horizontal scan from (" << index / 6 << "," << index % 6 << "): "
//				          << partial_score << ", jump=" << jump_forward << "\n";
			}
		}

	int diff_pieces = 0;
	int diff_pieces_central = 0;
	int diff_pieces_border = 0;

	if( _blue_turn )
	{
		diff_pieces = count_blue_pieces - count_red_pieces;
		diff_pieces_central = count_blue_central_pieces - count_red_central_pieces;
		diff_pieces_border = count_red_border_pieces - count_blue_border_pieces;
	}
	else
	{
		diff_pieces = count_red_pieces - count_blue_pieces;
		diff_pieces_central = count_red_central_pieces - count_blue_central_pieces;
		diff_pieces_border = count_blue_border_pieces - count_red_border_pieces;
	}

	score += 3 * diff_pieces + diff_pieces_central + diff_pieces_border;

//	std::cout << "diff_pieces: " << diff_pieces
//	          << ", diff_pieces_central: " << diff_pieces_central
//						<< ", diff_pieces_border: " << diff_pieces_border << "\n";
//
//	std::cout << "Score for piece " << variables[0]->get_value() * (_blue_turn ? -1 : 1)
//						<< " at (" << (char)('a'+variables[1]->get_value()) << "," << variables[2]->get_value()+1 << "): "
//						<< score << "\n";

	//delete std::cout.rdbuf(0); // to avoid memory leaks

	return score;
}