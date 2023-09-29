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
		mutable jbyte _simulation_grid[36];
		jboolean _blue_turn;

public:
		PoboObjective( const std::vector<ghost::Variable> &variables, jbyte *const grid, jboolean blue_turn );

		double required_cost( const std::vector<ghost::Variable *> &variables ) const override;
};

#endif //POBO_OBJECTIVE_HPP
