//
// Created by flo on 21/06/2023.
//

#include <string>
#include "builder.hpp"
#include "has_piece.hpp"
#include "free_position.hpp"
#include "removed_positions.hpp"
#include "pobo_objective.hpp"

Builder::Builder( jbyte * const grid,
									jbyte * const pool,
									jint pool_size,
									jboolean blue_turn,
									jbyte * const to_remove_row,
									jbyte * const to_remove_col,
									jbyte * const to_remove_p,
									jint number_to_remove )
	: ModelBuilder(),
	  _grid( grid ),
	  _pool( pool ),
	  _pool_size( pool_size ),
	  _blue_turn( blue_turn ),
	  _to_remove_row( to_remove_row ),
	  _to_remove_col( to_remove_col ),
	  _to_remove_p( to_remove_p ),
	  _number_to_remove( number_to_remove )
{
	piece.push_back(0); // Piece variable is at index 0 of the Variable vector
	coordinates.push_back(1);
	coordinates.push_back(2); // Coordinates (row,column) at respectively at indexes 1 and 2
}

void Builder::declare_variables()
{
  variables.emplace_back( 1, 2, std::string("piece") );
  variables.emplace_back( 0, 6, std::string("row") );
  variables.emplace_back( 0, 6, std::string("col") );
}

void Builder::declare_constraints()
{
	constraints.emplace_back( std::make_shared<HasPiece>( piece, _pool, _pool_size ) );
	constraints.emplace_back( std::make_shared<FreePosition>( coordinates, _grid ) );

	if( _number_to_remove > 0 )
		constraints.emplace_back( std::make_shared<RemovedPositions>( variables, _to_remove_row, _to_remove_col, _to_remove_p, _number_to_remove ) );
}

void Builder::declare_objective()
{
	objective = std::make_shared<PoboObjective>( variables, _grid, _blue_turn );
}
