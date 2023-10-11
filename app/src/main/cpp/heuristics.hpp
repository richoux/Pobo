//
// Created by flo on 10/10/2023.
//

#ifndef POBO_HEURISTICS_HPP
#define POBO_HEURISTICS_HPP

#include <jni.h>
#include <vector>
#include "helpers.hpp"

double heuristic_state( jbyte *const simulation_grid,
                        jboolean blue_turn,
                        jbyte *const blue_pool,
                        jint blue_pool_size,
                        jbyte *const red_pool,
                        jint red_pool_size );

std::vector<double> heuristic_graduation( jbyte *const simulation_grid,
                                          std::vector< std::vector<Position> > groups,
                                          jboolean blue_turn,
                                          jbyte *const blue_pool,
                                          jint blue_pool_size,
                                          jbyte *const red_pool,
                                          jint red_pool_size );

#endif //POBO_HEURISTICS_HPP
