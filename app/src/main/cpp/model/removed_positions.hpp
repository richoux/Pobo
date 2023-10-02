//
// Created by flo on 02/10/2023.
//

#ifndef POBO_REMOVED_POSITIONS_HPP
#define POBO_REMOVED_POSITIONS_HPP

#include <jni.h>

#include <vector>
#include "../lib/include/ghost/constraint.hpp"

class RemovedPositions : public ghost::Constraint
{
		jbyte *_to_remove_row;
		jbyte *_to_remove_col;
		jbyte *_to_remove_p;
		jint _number_to_remove;

public:
		RemovedPositions( const std::vector<ghost::Variable> &variables,
						jbyte *const to_remove_row,
						jbyte *const to_remove_col,
						jbyte *const to_remove_p,
						jint number_to_remove );

		double required_error( const std::vector<ghost::Variable *> &variables ) const override;
};

#endif //POBO_REMOVED_POSITIONS_HPP
