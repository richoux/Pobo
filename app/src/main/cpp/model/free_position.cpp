//
// Created by flo on 21/06/2023.
//

#include "free_position.hpp"

FreePosition::FreePosition(const std::vector <ghost::Variable> &variables, jbyte *const grid )
    : Constraint( variables ),
      _grid( grid )
{ }

double FreePosition::required_error(const std::vector<ghost::Variable *> &variables) const
{
    _cache_error = _grid[ 6 * variables[1]->get_value() + variables[0]->get_value() ] == 0 ? 0. : 1.;
    return _cache_error;
}