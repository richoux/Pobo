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
		mutable jbyte _simulation_grid[36];
		jboolean _blue_turn;

		bool check_three_in_a_row( int from_row, int from_col, Direction direction, PieceType type ) const;
		bool check_two_in_a_row( int from_row, int from_col, Direction direction, PieceType Type ) const;
		int	count_Po_in_a_row( int from_row, int from_col, Direction direction ) const;
		double compute_partial_score( int from_row, int from_col, Direction direction, int& jump_forward ) const;
		int get_next_row( int from_row, Direction direction ) const;
		int get_next_col( int from_col, Direction direction ) const;
		void simulate_move( const std::vector<ghost::Variable *> &variables ) const;

public:
		PoboHeuristic( const std::vector<ghost::Variable> &variables, jbyte *const grid, jboolean blue_turn );

		double required_cost( const std::vector<ghost::Variable *> &variables ) const override;
};

#endif //POBO_POBO_HEURISTIC_HPP
