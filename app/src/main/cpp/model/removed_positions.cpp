//
// Created by flo on 02/10/2023.
//

#include "removed_positions.hpp"

RemovedPositions::RemovedPositions(const std::vector<ghost::Variable> &variables,
																	 jbyte *const to_remove_row,
																	 jbyte *const to_remove_col,
																	 jbyte *const to_remove_p,
																	 jint number_to_remove)
	: Constraint( variables ),
	  _to_remove_row( to_remove_row ),
	  _to_remove_col( to_remove_col ),
	  _to_remove_p( to_remove_p ),
	  _number_to_remove( number_to_remove )
{ }

double RemovedPositions::required_error( const std::vector<ghost::Variable *> &variables ) const
{
	for( int i = 0 ; i < _number_to_remove ; ++i )
	{
		if( _to_remove_p[i] == variables[0]->get_value()
				&& _to_remove_row[i] == variables[1]->get_value()
				&& _to_remove_col[i] == variables[2]->get_value() )
			return 1.0;
	}

	return 0.0;
}