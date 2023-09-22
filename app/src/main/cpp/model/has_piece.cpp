//
// Created by flo on 21/06/2023.
//

#include <iostream>
#include "../androidbuf.hpp"

#include "has_piece.hpp"

HasPiece::HasPiece( const std::vector<int>& variables_index, jbyte * const pool, jint pool_size )
        : Constraint( variables_index ),
          _pool( pool ),
          _pool_size( pool_size )
{ }

double HasPiece::required_error( const std::vector<ghost::Variable *> &variables ) const
{
    int piece = variables[0]->get_value();
    bool piece_found = false;

    for( int i = 0 ; i < _pool_size ; ++i )
        if( piece == static_cast<int>( _pool[i] ) )
        {
            piece_found = true;
            break;
        }

    _cache_error = piece_found ? 0. : 1.;
    return _cache_error;
}
