//
// Created by flo on 21/06/2023.
//

#include "pobo_heuristic.hpp"

PoboHeuristic::PoboHeuristic( const std::vector<ghost::Variable>& variables, jbyte * const grid, jboolean blue_turn )
    : Maximize( variables, "pobo Heuristic" ),
      _grid( grid ),
      _blue_turn( blue_turn )
{ }

bool PoboHeuristic::check_three_in_a_row( int from_x, int from_y, Direction direction, PieceType type ) const {
    if( from_x > 5 || from_y < 0 || from_y > 5 )
        return false;

    int next_x = get_next_x( from_x, direction );
    int next_y = get_next_y( from_y, direction );

    if( _grid[from_y * 6 + from_x] == 0 || next_x > 5 || next_y < 0 || next_y > 5 )
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

    if( _grid[index] == 0 || next_x > 5 || next_y < 0 || next_y > 5 )
        return false;

    int next_index = next_y * 6 + next_x;

    switch( type ) {
        case WHATEVER: return _grid[index] * _grid[next_index] > 0;
        default: return  _grid[index] == _grid[next_index];
    }
}

int PoboHeuristic::count_Po_in_a_row( int from_x, int from_y, Direction direction ) const
{
    if( from_x > 5 || from_y < 0 || from_y > 5 )
        return 0;

    int next_x = get_next_x( from_x, direction );
    int next_y = get_next_y( from_y, direction );
    int index = from_y * 6 + from_x;

    if( _grid[index] == 0 || next_x > 5 || next_y < 0 || next_y > 5 )
        return 0;

    int next_next_x = get_next_x( next_x, direction );
    int next_next_y = get_next_y( next_y, direction );
    int next_index = next_y * 6 + next_x;

    if( _grid[next_index] == 0 || next_next_x > 5 || next_next_y < 0 || next_next_y > 5 )
        return 0;

    int next_next_index = next_next_y * 6 + next_next_x;

    int count = 0;
    if( std::abs( _grid[index] ) == 1 )
        ++count;
    if( std::abs( _grid[next_index] ) == 1 )
        ++count;
    if( std::abs( _grid[next_next_index] ) == 1 )
        ++count;

    return count;
}

int PoboHeuristic::get_next_x( int from_x, Direction direction ) const
{
    switch( direction ) {
        case TOPRIGHT: return from_x + 1;
        case RIGHT : return from_x + 1;
        case BOTTOMRIGHT : return from_x + 1;
        case BOTTOM : return from_x;
    }
}

int PoboHeuristic::get_next_y( int from_y, Direction direction ) const
{
    switch( direction ) {
        case TOPRIGHT: return from_y - 1;
        case RIGHT : return from_y;
        case BOTTOMRIGHT : return from_y + 1;
        case BOTTOM : return from_y + 1;
    }
}

double PoboHeuristic::compute_score( int from_x, int from_y, Direction direction, int coeff ) const
{
    double score = 0.;
    if (check_three_in_a_row(from_x, from_y, direction, BO))
        score += 200;
    else
    {
        if (check_three_in_a_row(from_x, from_y, direction, PO))
            score += 30;
        else
        {
            if (check_three_in_a_row(from_x, from_y, direction, WHATEVER))
                score += 10 * count_Po_in_a_row(from_x, from_y, direction);
            else
            {
                if( check_two_in_a_row(from_x, from_y, direction, BO) ) {
                    if (coeff > 0)
                        score += 10;
                    else
                        score += 100; // it will actually be -100 once the score is returned
                }
                else
                {
                    if( check_two_in_a_row(from_x, from_y, direction, PO) ) {
                        if (coeff > 0)
                            score += 2;
                        else
                            score += 20; // it will actually be -20 once the score is returned
                    }
                }
            }
        }
    }

    return coeff * score;
}

double PoboHeuristic::compute_score_positive( int from_x, int from_y, Direction direction ) const
{
    return compute_score( from_x, from_y, direction, 1 );
}

double PoboHeuristic::compute_score_negative( int from_x, int from_y, Direction direction ) const
{
    return compute_score( from_x, from_y, direction, -1 );
}

double PoboHeuristic::required_cost( const std::vector<ghost::Variable*>& variables ) const
{
    double score = 0.;

    int count_my_pieces = 0;
    int count_opponent_pieces = 0;

    int count_my_central_pieces = 0;
    int count_opponent_central_pieces = 0;

    int count_my_border_pieces = 0;
    int count_opponent_border_pieces = 0;

    if( _grid[14] < 0 && _blue_turn )
        ++count_my_central_pieces;
    if( _grid[14] > 0 && !_blue_turn )
        ++count_opponent_central_pieces;
    if( _grid[15] < 0 && _blue_turn )
        ++count_my_central_pieces;
    if( _grid[15] > 0 && !_blue_turn )
        ++count_opponent_central_pieces;
    if( _grid[20] < 0 && _blue_turn )
        ++count_my_central_pieces;
    if( _grid[20] > 0 && !_blue_turn )
        ++count_opponent_central_pieces;
    if( _grid[21] < 0 && _blue_turn )
        ++count_my_central_pieces;
    if( _grid[21] > 0 && !_blue_turn )
        ++count_opponent_central_pieces;

    // horizontal scans
    for( int row = 0 ; row < 6 ; ++row )
        for( int col = 0 ; col < 6 ; ++col )
        {
            if( _grid[6 * row + col] != 0 )
            {
                if (_blue_turn)
                {
                    if (_grid[6 * row + col] < 0)
                    {
                        ++count_my_pieces;
                        score += compute_score_positive(row, col, RIGHT);
                        if( row == 0 || row == 5 )
                            ++count_my_border_pieces;
                    }
                    else
                    {
                        ++count_opponent_pieces;
                        score += compute_score_negative(row, col, RIGHT);
                        if( row == 0 || row == 5 )
                            ++count_opponent_border_pieces;
                    }
                }
                else // red turn
                {
                    if (_grid[6 * row + col] < 0)
                    {
                        ++count_opponent_pieces;
                        score += compute_score_negative( row, col, RIGHT );
                        if( row == 0 || row == 5 )
                            ++count_opponent_border_pieces;
                    }
                    else
                    {
                        ++count_my_pieces;
                        score += compute_score_positive( row, col, RIGHT );
                        if( row == 0 || row == 5 )
                            ++count_my_border_pieces;
                    }
                }
            }
        }

    // vertical scans
    for( int col = 0 ; col < 6 ; ++col )
        for( int row = 0 ; row < 6 ; ++row )
        {
            if( _grid[6 * row + col] != 0 )
            {
                if (_blue_turn)
                {
                    if (_grid[6 * row + col] < 0)
                    {
                        ++count_my_pieces;
                        score += compute_score_positive(row, col, BOTTOM);
                        if( ( col == 0 || col == 5 ) && row > 0 && row < 5 )
                            ++count_my_border_pieces;
                    }
                    else
                    {
                        ++count_opponent_pieces;
                        score += compute_score_negative(row, col, BOTTOM);
                        if( ( col == 0 || col == 5 ) && row > 0 && row < 5 )
                            ++count_opponent_border_pieces;
                    }
                }
                else // red turn
                {
                    if (_grid[6 * row + col] < 0)
                    {
                        ++count_opponent_pieces;
                        score += compute_score_negative( row, col, BOTTOM );
                        if( ( col == 0 || col == 5 ) && row > 0 && row < 5 )
                            ++count_opponent_border_pieces;
                    }
                    else
                    {
                        ++count_my_pieces;
                        score += compute_score_positive( row, col, BOTTOM );
                        if( ( col == 0 || col == 5 ) && row > 0 && row < 5 )
                            ++count_my_border_pieces;
                    }
                }
            }
        }

    // ascendant diagonal scans
    for( int row = 0 ; row < 6 ; ++row )
        for( int col = 0 ; col < 6 ; ++col )
        {
            if( _grid[6 * row + col] != 0 )
            {
                if (_blue_turn)
                {
                    if (_grid[6 * row + col] < 0)
                        score += compute_score_positive(row, col, TOPRIGHT);
                    else
                        score += compute_score_negative(row, col, TOPRIGHT);
                }
                else // red turn
                {
                    if (_grid[6 * row + col] < 0)
                        score += compute_score_negative( row, col, TOPRIGHT );
                    else
                        score += compute_score_positive( row, col, TOPRIGHT );
                }
            }
        }

    // descendant diagonal scans
    for( int row = 0 ; row < 6 ; ++row )
        for( int col = 0 ; col < 6 ; ++col )
        {
            if( _grid[6 * row + col] != 0 )
            {
                if (_blue_turn)
                {
                    if (_grid[6 * row + col] < 0)
                        score += compute_score_positive(row, col, BOTTOMRIGHT);
                    else
                        score += compute_score_negative(row, col, BOTTOMRIGHT);
                }
                else // red turn
                {
                    if (_grid[6 * row + col] < 0)
                        score += compute_score_negative( row, col, BOTTOMRIGHT );
                    else
                        score += compute_score_positive( row, col, BOTTOMRIGHT );
                }
            }
        }

    score += (count_my_pieces - count_opponent_pieces) +
            (count_my_central_pieces - count_opponent_central_pieces) +
            ( count_opponent_border_pieces - count_my_border_pieces );

    return score;
}