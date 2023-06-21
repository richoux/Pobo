//
// Created by flo on 21/06/2023.
//

#ifndef POBO_FREE_POSITION_HPP
#define POBO_FREE_POSITION_HPP

#include <jni.h>

#include <vector>
#include "../lib/include/ghost/constraint.hpp"

class FreePosition : public ghost::Constraint {
    jbyte *_grid;
    mutable double _cache_error;

public:
    FreePosition(const std::vector <ghost::Variable> &variables, jbyte *const grid );

    double required_error(const std::vector<ghost::Variable *> &variables) const override;

//	double optional_delta_error(const std::vector<ghost::Variable *> &variables,
//								const std::vector<int> &indexes,
//								const std::vector<int> &candidate_values) const override;

//	void conditional_update_data_structures( const std::vector<ghost::Variable*>& variables,
//											 int variable_index,
//											 int new_value ) override;
};

#endif //POBO_FREE_POSITION_HPP
