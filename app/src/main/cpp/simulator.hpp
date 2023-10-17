//
// Created by flo on 16/10/2023.
//

#ifndef POBO_SIMULATOR_HPP
#define POBO_SIMULATOR_HPP

#include "helpers.hpp"
#include "heuristics.hpp"

#include <jni.h>
#include <vector>
#include "lib/include/ghost/variable.hpp"

void simulate_move( const std::vector<ghost::Variable *> &variables,
                    jbyte * const simulation_grid,
                    jboolean blue_turn,
                    jbyte * const blue_pool,
                    jint & blue_pool_size,
                    jbyte * const red_pool,
                    jint & red_pool_size );

#endif //POBO_SIMULATOR_HPP
