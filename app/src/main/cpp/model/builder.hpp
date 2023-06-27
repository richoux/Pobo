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
    jboolean _blue_turn;

    std::vector<int> piece;
    std::vector<int> coordinates;

public:
    Builder( jbyte * const grid, jbyte * const pool, jint pool_size, jboolean blue_turn );

    void declare_variables() override;
    void declare_constraints() override;
    void declare_objective() override;
};

#endif //POBO_BUILDER_HPP
