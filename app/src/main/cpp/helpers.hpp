//
// Created by flo on 29/09/2023.
//

#ifndef HELPERS_HPP
#define HELPERS_HPP

#include <jni.h>
#include <vector>
#include "lib/include/ghost/variable.hpp"

enum Direction { TOPRIGHT, RIGHT, BOTTOMRIGHT, BOTTOM };
enum PieceType { PO, BO, WHATEVER };

struct Position
{
	int row;
	int column;

	Position(int row, int col)
	: row(row),
	  column(col)
	{ }
};

bool check_three_in_a_row( int from_row,
													 int from_col,
													 Direction direction,
													 PieceType type,
													 jbyte * const simulation_grid );

bool check_two_in_a_row( int from_row,
												 int from_col,
												 Direction direction,
												 PieceType Type,
												 jbyte * const simulation_grid );

bool is_two_in_a_row_in_corner( int from_row,
																int from_col,
																Direction direction );

bool is_two_in_a_row_blocked( int from_row,
															int from_col,
															Direction direction,
															jbyte * const simulation_grid );

int	count_Po_in_a_row( int from_row,
												int from_col,
												Direction direction,
												jbyte * const simulation_grid );

double compute_partial_score( int from_row,
															int from_col,
															Direction direction,
															int& jump_forward,
															jbyte * const simulation_grid,
															jboolean blue_turn,
															jbyte * const blue_pool,
															jint& blue_pool_size,
															jbyte * const red_pool,
															jint& red_pool_size );

int get_next_row( int from_row,
									Direction direction );

int get_next_col( int from_col,
									Direction direction );

Position get_position_toward( const Position &position, Direction direction );
Position get_position_toward( const Position &position, int direction );

bool is_valid_position( const Position &position );
bool is_empty_position( jbyte * const simulation_grid, int row, int col );
bool is_blue_piece_on( jbyte * const simulation_grid, int row, int col );
bool is_empty_position( jbyte * const simulation_grid, const Position &position );
bool is_blue_piece_on( jbyte * const simulation_grid, const Position &position );
bool is_on_border( const std::vector<Position> &group );

std::vector< std::vector<Position> > get_graduations( jbyte * const simulation_grid,
																											jboolean blue_turn,
																											jint blue_pool_size,
																											jint red_pool_size );

void simulate_move( const std::vector<ghost::Variable *> &variables,
										jbyte * const simulation_grid,
										jboolean blue_turn,
										jbyte * const blue_pool,
										jint & blue_pool_size,
										jbyte * const red_pool,
										jint & red_pool_size );

#endif //HELPERS_HPP
