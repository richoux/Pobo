//
// Created by flo on 29/09/2023.
//

#ifndef HEURISTICS_HPP
#define HEURISTICS_HPP

#include <jni.h>
#include <vector>
#include "lib/include/ghost/variable.hpp"

enum Direction { TOPRIGHT, RIGHT, BOTTOMRIGHT, BOTTOM };
enum PieceType { PO, BO, WHATEVER };

bool check_three_in_a_row( int from_row, int from_col, Direction direction, PieceType type, jbyte * const simulation_grid );
bool check_two_in_a_row( int from_row, int from_col, Direction direction, PieceType Type, jbyte * const simulation_grid );
int	count_Po_in_a_row( int from_row, int from_col, Direction direction, jbyte * const simulation_grid );
double compute_partial_score( int from_row, int from_col, Direction direction, int& jump_forward, jbyte * const simulation_grid, jboolean blue_turn );
int get_next_row( int from_row, Direction direction );
int get_next_col( int from_col, Direction direction );
void simulate_move( const std::vector<ghost::Variable *> &variables, jbyte * const simulation_grid, jboolean blue_turn );
double heuristic( jbyte * const simulation_grid, jboolean blue_turn );

#endif //HEURISTICS_HPP
