//
// Created by flo on 21/06/2023.
//

#include "pobo_heuristic.hpp"

PoboHeuristic::PoboHeuristic( const std::vector<ghost::Variable>& variables, jbyte * const grid, jboolean blue_turn )
				: Maximize( variables, "pobo Heuristic" ),
				  _grid( grid ),
				  _blue_turn( blue_turn )
{ }

bool PoboHeuristic::check_three_in_a_row( int from_x, int from_y, Direction direction, PieceType type ) const
{
	if( from_x > 5 || from_y < 0 || from_y > 5 )
		return false;

	int next_x = get_next_x( from_x, direction );
	int next_y = get_next_y( from_y, direction );

	if( _simulation_grid[from_y * 6 + from_x] == 0 || next_x > 5 || next_y < 0 || next_y > 5 )
		return false;

	return check_two_in_a_row( from_x, from_y, direction, type )
	       && check_two_in_a_row( next_x, next_y, direction, type );
}

bool PoboHeuristic::check_two_in_a_row( int from_x, int from_y, Direction direction, PieceType type ) const
{
	if( from_x > 5 || from_y < 0 || from_y > 5 )
		return false;

	int next_x = get_next_x( from_x, direction );
	int next_y = get_next_y( from_y, direction );

	int index = from_y * 6 + from_x;

	if( _simulation_grid[index] == 0 || next_x > 5 || next_y < 0 || next_y > 5 )
		return false;

	int next_index = next_y * 6 + next_x;

	switch( type )
	{
		case WHATEVER:
			return _simulation_grid[index] * _simulation_grid[next_index] > 0;
		default:
			return _simulation_grid[index] == _simulation_grid[next_index];
	}
}

int PoboHeuristic::count_Po_in_a_row( int from_x, int from_y, Direction direction ) const
{
	if( from_x > 5 || from_y < 0 || from_y > 5 )
		return 0;

	int next_x = get_next_x( from_x, direction );
	int next_y = get_next_y( from_y, direction );
	int index = from_y * 6 + from_x;

	if( _simulation_grid[index] == 0 || next_x > 5 || next_y < 0 || next_y > 5 )
		return 0;

	int next_next_x = get_next_x( next_x, direction );
	int next_next_y = get_next_y( next_y, direction );
	int next_index = next_y * 6 + next_x;

	if( _simulation_grid[next_index] == 0 || next_next_x > 5 || next_next_y < 0 || next_next_y > 5 )
		return 0;

	int next_next_index = next_next_y * 6 + next_next_x;

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

int PoboHeuristic::get_next_x( int from_x, Direction direction ) const
{
	switch( direction )
	{
		case TOPRIGHT:
			return from_x + 1;
		case RIGHT :
			return from_x + 1;
		case BOTTOMRIGHT :
			return from_x + 1;
		case BOTTOM :
			return from_x;
	}
}

int PoboHeuristic::get_next_y( int from_y, Direction direction ) const
{
	switch( direction )
	{
		case TOPRIGHT:
			return from_y - 1;
		case RIGHT :
			return from_y;
		case BOTTOMRIGHT :
			return from_y + 1;
		case BOTTOM :
			return from_y + 1;
	}
}

double PoboHeuristic::compute_partial_score( int from_x, int from_y, Direction direction, int& jump_forward ) const
{
	double score = 0.;
	bool is_player_piece = ( ( _simulation_grid[ from_y * 6 + from_x ] < 0 && _blue_turn ) ||
					( _simulation_grid[ from_y * 6 + from_x ] > 0 && !_blue_turn ) );

	if( check_three_in_a_row( from_x, from_y, direction, BO ))
	{
		score += is_player_piece ? 200 : -200;
		jump_forward = 2;
	}
	else
	{
		if( check_three_in_a_row( from_x, from_y, direction, PO ))
		{
			score += is_player_piece ? 30 : -30;
			jump_forward = 2;
		}
		else
		{
			if( check_three_in_a_row( from_x, from_y, direction, WHATEVER ))
			{
				score += count_Po_in_a_row( from_x, from_y, direction ) * ( is_player_piece ? 10 : -10 );
				jump_forward = 1;
			}
			else
			{
				if( check_two_in_a_row( from_x, from_y, direction, BO ))
				{
					score += is_player_piece ? 20 : -100;
					jump_forward = 1;
				}
				else
				{
					if( check_two_in_a_row( from_x, from_y, direction, PO ))
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
	int p = variables[0]->get_value() * _blue_turn ? -1 : 1;
	int x = variables[1]->get_value();
	int y = variables[2]->get_value();
	int index = y * 6 + x;

	_simulation_grid[index] = p;

	if( x - 1 >= 0 )
	{
		if( y - 1 >= 0 )
		{
			if( _simulation_grid[( y - 1 ) * 6 + x - 1] != 0 &&
			    ( x - 2 < 0 || y - 2 < 0 || _simulation_grid[( y - 2 ) * 6 + x - 2] == 0 ))
			{
				if( x - 2 >= 0 && y - 2 >= 0 && _simulation_grid[( y - 2 ) * 6 + x - 2] == 0 )
					_simulation_grid[( y - 2 ) * 6 + x - 2] = _simulation_grid[( y - 1 ) * 6 + x - 1];
				_simulation_grid[( y - 1 ) * 6 + x - 1] = 0;
			}
		}
		if( y + 1 <= 5 )
		{
			if( _simulation_grid[( y + 1 ) * 6 + x - 1] != 0 &&
			    ( x - 2 < 0 || y + 2 > 5 || _simulation_grid[( y + 2 ) * 6 + x - 2] == 0 ))
			{
				if( x - 2 >= 0 && y + 2 <= 5 && _simulation_grid[( y + 2 ) * 6 + x - 2] == 0 )
					_simulation_grid[( y + 2 ) * 6 + x - 2] = _simulation_grid[( y + 1 ) * 6 + x - 1];
				_simulation_grid[( y + 1 ) * 6 + x - 1] = 0;
			}
		}
		if( _simulation_grid[y * 6 + x - 1] != 0 &&
		    ( x - 2 < 0 || _simulation_grid[y * 6 + x - 2] == 0 ))
		{
			if( x - 2 >= 0 && _simulation_grid[y * 6 + x - 2] == 0 )
				_simulation_grid[y * 6 + x - 2] = _simulation_grid[y * 6 + x - 1];
			_simulation_grid[y * 6 + x - 1] = 0;
		}
	}
	if( x + 1 <= 5 )
	{
		if( y - 1 >= 0 )
		{
			if( _simulation_grid[( y - 1 ) * 6 + x + 1] != 0 &&
			    ( x + 2 > 5 || y - 2 < 0 || _simulation_grid[( y - 2 ) * 6 + x + 2] == 0 ))
			{
				if( x + 2 <= 5 && y - 2 >= 0 && _simulation_grid[( y - 2 ) * 6 + x + 2] == 0 )
					_simulation_grid[( y - 2 ) * 6 + x + 2] = _simulation_grid[( y - 1 ) * 6 + x + 1];
				_simulation_grid[( y - 1 ) * 6 + x + 1] = 0;
			}
		}
		if( y + 1 <= 5 )
		{
			if( _simulation_grid[( y + 1 ) * 6 + x + 1] != 0 &&
			    ( x + 2 > 5 || y + 2 > 5 || _simulation_grid[( y + 2 ) * 6 + x + 2] == 0 ))
			{
				if( x + 2 <= 5 && y + 2 <= 5 && _simulation_grid[( y + 2 ) * 6 + x + 2] == 0 )
					_simulation_grid[( y + 2 ) * 6 + x + 2] = _simulation_grid[( y + 1 ) * 6 + x + 1];
				_simulation_grid[( y + 1 ) * 6 + x + 1] = 0;
			}
		}
		if( _simulation_grid[y * 6 + x + 1] != 0 &&
		    ( x + 2 > 5 || _simulation_grid[y * 6 + x + 2] == 0 ))
		{
			if( x + 2 <= 5 && _simulation_grid[y * 6 + x + 2] == 0 )
				_simulation_grid[y * 6 + x + 2] = _simulation_grid[y * 6 + x + 1];
			_simulation_grid[y * 6 + x + 1] = 0;
		}
	}
	if( y - 1 >= 0 )
	{
		if( _simulation_grid[( y - 1 ) * 6 + x] != 0 &&
		    ( y - 2 < 0 || _simulation_grid[( y - 2 ) * 6 + x] == 0 ))
		{
			if( y - 2 >= 0 && _simulation_grid[( y - 2 ) * 6 + x] == 0 )
				_simulation_grid[( y - 2 ) * 6 + x] = _simulation_grid[( y - 1 ) * 6 + x];
			_simulation_grid[( y - 1 ) * 6 + x] = 0;
		}
	}
	if( y + 1 <= 5 )
	{
		if( _simulation_grid[( y + 1 ) * 6 + x] != 0 &&
		    ( y + 2 > 5 || _simulation_grid[( y + 2 ) * 6 + x] == 0 ))
		{
			if( y + 2 <= 5 && _simulation_grid[( y + 2 ) * 6 + x] == 0 )
				_simulation_grid[( y + 2 ) * 6 + x] = _simulation_grid[( y + 1 ) * 6 + x];
			_simulation_grid[( y + 1 ) * 6 + x] = 0;
		}
	}
}

double PoboHeuristic::required_cost( const std::vector<ghost::Variable *> &variables ) const
{
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

	if( _simulation_grid[14] < 0 )
		++count_blue_central_pieces;
	if( _simulation_grid[14] > 0 )
		++count_red_central_pieces;
	if( _simulation_grid[15] < 0 )
		++count_blue_central_pieces;
	if( _simulation_grid[15] > 0 )
		++count_red_central_pieces;
	if( _simulation_grid[20] < 0 )
		++count_blue_central_pieces;
	if( _simulation_grid[20] > 0 )
		++count_red_central_pieces;
	if( _simulation_grid[21] < 0 )
		++count_blue_central_pieces;
	if( _simulation_grid[21] > 0 )
		++count_red_central_pieces;

	// horizontal scans
	for( int row = 0; row < 6; ++row )
	{
		int jump_forward = 0;
		for( int col = 0; col < 5; col = col + 1 + jump_forward )
		{
			if( _simulation_grid[6 * row + col] != 0 )
			{
				if( _simulation_grid[6 * row + col] < 0 )
				{
					++count_blue_pieces;
					score += compute_partial_score( row, col, RIGHT, jump_forward );
					if( row == 0 || row == 5 )
						++count_blue_border_pieces;
				} else
				{
					++count_red_pieces;
					score += compute_partial_score( row, col, RIGHT, jump_forward );
					if( row == 0 || row == 5 )
						++count_red_border_pieces;
				}
			}
		}
	}

	// vertical scans
	for( int col = 0; col < 6; ++col )
	{
		int jump_forward = 0;
		for( int row = 0; row < 5; row = row + 1 + jump_forward )
		{
			if( _simulation_grid[6 * row + col] != 0 )
			{
				if( _simulation_grid[6 * row + col] < 0 )
				{
					++count_blue_pieces;
					score += compute_partial_score( row, col, BOTTOM, jump_forward );
					if(( col == 0 || col == 5 ) && row > 0 )
						++count_blue_border_pieces;
				} else
				{
					++count_red_pieces;
					score += compute_partial_score( row, col, BOTTOM, jump_forward );
					if(( col == 0 || col == 5 ) && row > 0 )
						++count_red_border_pieces;
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
			if( _simulation_grid[index] < 0 )
				score += compute_partial_score( index % 6, index / 6, TOPRIGHT, fake_jump );
			else
				score += compute_partial_score( index % 6, index / 6, TOPRIGHT, fake_jump );
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
			if( _simulation_grid[index] < 0 )
				score += compute_partial_score( index % 6, index / 6, BOTTOMRIGHT, fake_jump );
			else
				score += compute_partial_score( index % 6, index / 6, BOTTOMRIGHT, fake_jump );
		}

	if( _blue_turn )
		score += 3 * ( count_blue_pieces - count_red_pieces ) +
	           ( count_blue_central_pieces - count_red_central_pieces ) +
	           ( count_red_border_pieces - count_blue_border_pieces );
	else
		score += 3 * ( count_red_pieces - count_blue_pieces ) +
		         ( count_red_central_pieces - count_blue_central_pieces ) +
		         ( count_blue_border_pieces - count_red_border_pieces );

	return score;
}