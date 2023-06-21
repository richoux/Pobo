//
// Created by flo on 21/06/2023.
//

#ifndef POBO_HAS_PIECE_HPP
#define POBO_HAS_PIECE_HPP

#include <jni.h>

#include <vector>
#include "../lib/include/ghost/constraint.hpp"

class HasPiece : public ghost::Constraint {
	jbyte *_pool;
	jint _pool_size;
	mutable double _cache_error;

public:
	HasPiece(const std::vector <ghost::Variable> &variables, jbyte *const pool, jint pool_size);

	double required_error(const std::vector<ghost::Variable *> &variables) const override;

//	double optional_delta_error(const std::vector<ghost::Variable *> &variables,
//								const std::vector<int> &indexes,
//								const std::vector<int> &candidate_values) const override;

//	void conditional_update_data_structures( const std::vector<ghost::Variable*>& variables,
//											 int variable_index,
//											 int new_value ) override;
};

#endif //POBO_HAS_PIECE_HPP
