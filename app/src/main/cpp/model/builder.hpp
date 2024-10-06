//
// Created by flo on 21/06/2023.
//

#ifndef POBO_BUILDER_HPP
#define POBO_BUILDER_HPP

#include <jni.h>

#include <vector>
#include "../lib/include/ghost/model_builder.hpp"

class Builder : public ghost::ModelBuilder
{
	jbyte *_grid;
	jbyte *_pool;
	jint _pool_size;
	jbyte *_blue_pool;
	jint _blue_pool_size;
	jbyte *_red_pool;
	jint _red_pool_size;
	jboolean _blue_turn;
	jbyte *_to_remove_row;
	jbyte *_to_remove_col;
	jbyte *_to_remove_p;
	jint _number_to_remove;

	std::vector<int> piece;
	std::vector<int> coordinates;

public:
	Builder( jbyte *const grid,
	         jbyte *const blue_pool,
	         jint blue_pool_size,
	         jbyte *const red_pool,
	         jint red_pool_size,
	         jboolean blue_turn,
	         jbyte *const to_remove_row = nullptr,
	         jbyte *const to_remove_col = nullptr,
	         jbyte *const to_remove_p = nullptr,
	         jint number_to_remove = 0 );

	void declare_variables() override;

	void declare_constraints() override;

	void declare_objective() override;
};

#endif //POBO_BUILDER_HPP
