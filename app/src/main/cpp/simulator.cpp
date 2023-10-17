#include "simulator.hpp"
#include "lib/include/ghost/thirdparty/randutils.hpp"

void simulate_move( const std::vector<ghost::Variable *> &variables,
                    jbyte * const simulation_grid,
                    jboolean blue_turn,
                    jbyte * const blue_pool,
                    jint & blue_pool_size,
                    jbyte * const red_pool,
                    jint & red_pool_size )
{
	int p = variables[0]->get_value() * (blue_turn ? -1 : 1);
	int row = variables[1]->get_value();
	int col = variables[2]->get_value();
	int index = row*6 + col;

	simulation_grid[index] = p;

	if( col-1 >= 0 )
	{
		// Top Left
		if( row-1 >= 0 )
		{
			if( simulation_grid[ ( row-1 )*6 + col-1 ] != 0 &&
			    std::abs( simulation_grid[ ( row-1 )*6 + col-1 ] ) <= std::abs( simulation_grid[ index ] ) &&
			    ( col-2 < 0 || row-2 < 0 || simulation_grid[ ( row-2 )*6 + col-2 ] == 0 ))
			{
				if( col-2 >= 0 && row-2 >= 0 && simulation_grid[ ( row-2 )*6 + col-2 ] == 0 )
					simulation_grid[ ( row-2 )*6 + col-2 ] = simulation_grid[ ( row-1 )*6 + col-1 ];
				simulation_grid[ ( row-1 )*6 + col-1 ] = 0;
			}
		}

		// Bottom Left
		if( row+1 <= 5 )
		{
			if( simulation_grid[ ( row+1 )*6 + col-1 ] != 0 &&
			    std::abs( simulation_grid[ ( row+1 )*6 + col-1 ] ) <= std::abs( simulation_grid[ index ] ) &&
			    ( col-2 < 0 || row+2 > 5 || simulation_grid[ ( row+2 )*6 + col-2 ] == 0 ))
			{
				if( col-2 >= 0 && row+2 <= 5 && simulation_grid[ ( row+2 )*6 + col-2 ] == 0 )
					simulation_grid[ ( row+2 )*6 + col-2 ] = simulation_grid[ ( row+1 )*6 + col-1 ];
				simulation_grid[ ( row+1 )*6 + col-1 ] = 0;
			}
		}

		// Left
		if( simulation_grid[ row*6 + col-1 ] != 0 &&
		    std::abs( simulation_grid[ row*6 + col-1 ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( col-2 < 0 || simulation_grid[ row*6 + col-2 ] == 0 ))
		{
			if( col-2 >= 0 && simulation_grid[ row*6 + col-2 ] == 0 )
				simulation_grid[ row*6 + col-2 ] = simulation_grid[ row*6 + col-1 ];
			simulation_grid[ row*6 + col-1 ] = 0;
		}
	}

	if( col+1 <= 5 )
	{
		// Top Right
		if( row-1 >= 0 )
		{
			if( simulation_grid[ ( row-1 )*6 + col+1 ] != 0 &&
			    std::abs( simulation_grid[ ( row-1 )*6 + col+1 ] ) <= std::abs( simulation_grid[ index ] ) &&
			    ( col+2 > 5 || row-2 < 0 || simulation_grid[ ( row-2 )*6 + col+2 ] == 0 ))
			{
				if( col+2 <= 5 && row-2 >= 0 && simulation_grid[ ( row-2 )*6 + col+2 ] == 0 )
					simulation_grid[ ( row-2 )*6 + col+2 ] = simulation_grid[ ( row-1 )*6 + col+1 ];
				simulation_grid[ ( row-1 )*6 + col+1 ] = 0;
			}
		}

		// Bottom Right
		if( row+1 <= 5 )
		{
			if( simulation_grid[ ( row+1 )*6 + col+1 ] != 0 &&
			    std::abs( simulation_grid[ ( row+1 )*6 + col+1 ] ) <= std::abs( simulation_grid[ index ] ) &&
			    ( col+2 > 5 || row+2 > 5 || simulation_grid[ ( row+2 )*6 + col+2 ] == 0 ) )
			{
				if( col+2 <= 5 && row+2 <= 5 && simulation_grid[ ( row+2 )*6 + col+2 ] == 0 )
					simulation_grid[ ( row+2 )*6 + col+2 ] = simulation_grid[ ( row+1 )*6 + col+1 ];
				simulation_grid[ ( row+1 )*6 + col+1 ] = 0;
			}
		}

		// Right
		if( simulation_grid[ row*6 + col+1 ] != 0 &&
		    std::abs( simulation_grid[ row*6 + col+1 ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( col+2 > 5 || simulation_grid[ row*6 + col+2 ] == 0 ))
		{
			if( col+2 <= 5 && simulation_grid[ row*6 + col+2 ] == 0 )
				simulation_grid[ row*6 + col+2 ] = simulation_grid[ row*6 + col+1 ];
			simulation_grid[ row*6 + col+1 ] = 0;
		}
	}

	// Top
	if( row-1 >= 0 )
	{
		if( simulation_grid[ ( row-1 )*6 + col ] != 0 &&
		    std::abs( simulation_grid[ ( row-1 )*6 + col ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( row-2 < 0 || simulation_grid[ ( row-2 )*6 + col ] == 0 ))
		{
			if( row-2 >= 0 && simulation_grid[ ( row-2 )*6 + col ] == 0 )
				simulation_grid[ ( row-2 )*6 + col ] = simulation_grid[ ( row-1 )*6 + col ];
			simulation_grid[ ( row-1 )*6 + col ] = 0;
		}
	}

	// Bottom
	if( row+1 <= 5 )
	{
		if( simulation_grid[ ( row+1 )*6 + col ] != 0 &&
		    std::abs( simulation_grid[ ( row+1 )*6 + col ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( row+2 > 5 || simulation_grid[ ( row+2 )*6 + col ] == 0 ))
		{
			if( row+2 <= 5 && simulation_grid[ ( row+2 )*6 + col ] == 0 )
				simulation_grid[ ( row+2 )*6 + col ] = simulation_grid[ ( row+1 )*6 + col ];
			simulation_grid[ ( row+1 )*6 + col ] = 0;
		}
	}

	auto groups = get_graduations( simulation_grid, blue_turn, blue_pool_size, red_pool_size );
	std::vector< Position > group_to_graduate;

	if( groups.size() > 0 )
	{
		if( groups.size() == 1 )
			group_to_graduate = groups[0];
		else
		{
			randutils::mt19937_rng rng;
			auto scores = heuristic_graduation( simulation_grid, groups, blue_turn, blue_pool,
			                                    blue_pool_size, red_pool, red_pool_size );
			double best_score = -10000.0;
			std::vector<int> best_groups;

			for( int i = 0; i < groups.size(); ++i )
			{
				if( best_score < scores[ i ] )
				{
					best_score = scores[ i ];
					best_groups.clear();
					best_groups.push_back( i );
				}
				else
					if( best_score == scores[ i ] )
						best_groups.push_back( i );
			}

			group_to_graduate = groups[ rng.pick( best_groups ) ];
		}

		for( auto pos : group_to_graduate )
		{
			simulation_grid[ 6*pos.row + pos.column ] = 0;
			if( blue_turn )
			{
				blue_pool[ blue_pool_size ] = 2;
				++blue_pool_size;
			}
			else
			{
				red_pool[ red_pool_size ] = 2;
				++red_pool_size;
			}
		}
	}
}