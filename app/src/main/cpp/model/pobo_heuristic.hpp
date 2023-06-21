//
// Created by flo on 21/06/2023.
//

#ifndef POBO_POBO_HEURISTIC_HPP
#define POBO_POBO_HEURISTIC_HPP

#include <jni.h>

#include <vector>
#include "../lib/include/ghost/objective.hpp"

enum Direction { TOPRIGHT, RIGHT, BOTTOMRIGHT, BOTTOM };
enum PieceType { PO, BO, WHATEVER };

class PoboHeuristic : public ghost::Maximize
{
    jbyte *_grid;
    jboolean _blue_turn;

    bool check_three_in_a_row( int from_x, int from_y, Direction direction, PieceType type ) const;
    bool check_two_in_a_row( int from_x, int from_y, Direction direction, PieceType Type ) const;
    int count_Po_in_a_row( int from_x, int from_y, Direction direction ) const;
    double compute_score( int from_x, int from_y, Direction direction, int coeff ) const;
    double compute_score_positive( int from_x, int from_y, Direction direction ) const;
    double compute_score_negative( int from_x, int from_y, Direction direction ) const;
    int get_next_x( int from_x, Direction direction ) const;
    int get_next_y( int from_y, Direction direction ) const;

public:
    PoboHeuristic( const std::vector<ghost::Variable>& variables, jbyte * const grid, jboolean blue_turn );

    double required_cost( const std::vector<ghost::Variable*>& variables ) const override;
};

#endif //POBO_POBO_HEURISTIC_HPP
