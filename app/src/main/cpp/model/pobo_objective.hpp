//
// Created by flo on 21/06/2023.
//

#ifndef POBO_OBJECTIVE_HPP
#define POBO_OBJECTIVE_HPP

#include <jni.h>

#include <vector>
#include "../lib/include/ghost/objective.hpp"

class PoboObjective : public ghost::Maximize
{
	jbyte *_grid;
	jboolean _blue_turn;
	jbyte *_blue_pool;
	jint _blue_pool_size;
	jbyte *_red_pool;
	jint _red_pool_size;

	mutable jbyte _simulation_grid[36];
	mutable jbyte _simulation_blue_pool[8];
	mutable jint  _simulation_blue_pool_size;
	mutable jbyte _simulation_red_pool[8];
	mutable jint  _simulation_red_pool_size;

public:
	PoboObjective( const std::vector <ghost::Variable> &variables,
								 jbyte *const grid,
	               jboolean blue_turn,
	               jbyte *const blue_pool,
								 jint blue_pool_size,
								 jbyte *const red_pool,
								 jint red_pool_size );

	double required_cost( const std::vector<ghost::Variable *> &variables ) const override;
};

#endif //POBO_OBJECTIVE_HPP
