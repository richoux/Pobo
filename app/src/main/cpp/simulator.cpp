#include "simulator.hpp"
#include "lib/include/ghost/thirdparty/randutils.hpp"

#include <android/log.h>
//*
#define ALOG(...)
/*/
#define ALOG( ... ) __android_log_print(ANDROID_LOG_INFO, "pobotag C++", __VA_ARGS__)
//*/

void simulate_move( const std::vector<ghost::Variable *> &variables,
                    jbyte * const simulation_grid,
                    jboolean blue_turn,
                    jbyte * const blue_pool,
                    jint & blue_pool_size,
                    jbyte * const red_pool,
                    jint & red_pool_size )
{
	jbyte v_p = variables[0]->get_value();
	int p = v_p * (blue_turn ? -1 : 1);
	int row = variables[1]->get_value();
	int col = variables[2]->get_value();
	int index = row*6 + col;

	if( blue_turn )
	{
		int i = blue_pool_size - 1;
		while( blue_pool[i] != v_p && i >= 0 )
			--i;

		if( i < 0 )
			ALOG("THIS SHOULD NEVER HAPPEN: piece selected by the solver not in Blue pool");

		while( i < blue_pool_size - 1 )
		{
			blue_pool[i] = blue_pool[i+1];
			++i;
		}

		blue_pool[blue_pool_size-1] = 0;
		--blue_pool_size;
	}
	else
	{
		int i = red_pool_size - 1;
		while( red_pool[i] != v_p && i >= 0 )
			--i;

		if( i < 0 )
			ALOG("THIS SHOULD NEVER HAPPEN: piece selected by the solver not in Red pool");

		while( i < red_pool_size - 1 )
		{
			red_pool[i] = red_pool[i+1];
			++i;
		}

		red_pool[red_pool_size-1] = 0;
		--red_pool_size;
	}

	simulation_grid[index] = p;

	if( col-1 >= 0 )
	{
		// Top Left
		if( row - 1 >= 0 )
		{
			if( simulation_grid[ (row - 1) * 6 + col - 1 ] != 0 &&
			    std::abs( simulation_grid[ (row - 1) * 6 + col - 1 ] ) <=
			    std::abs( simulation_grid[ index ] ) &&
			    (col - 2 < 0 || row - 2 < 0 || simulation_grid[ (row - 2) * 6 + col - 2 ] == 0))
			{
				if( col - 2 >= 0 && row - 2 >= 0 )
				{
					if( simulation_grid[ (row - 2) * 6 + col - 2 ] == 0 )
						simulation_grid[ (row - 2) * 6 + col - 2 ] = simulation_grid[ (row - 1) * 6 + col - 1 ];
				}
				else // if a piece has been ejected out the board, we need to put it into the right pool
				{
					if( simulation_grid[ (row - 1) * 6 + col - 1 ] > 0 )
					{
						// in every case, either a Po or a Bo has been ejected out the board
						red_pool[ red_pool_size ] = 1;

						if( simulation_grid[ (row - 1) * 6 + col - 1 ] == 2 ) // if it is a Bo though
						{
							int i = 0;
							while( red_pool[ i ] == 2 )
								++i;
							red_pool[ i ] = 2;
						}

						++red_pool_size;
					}
					else
					{
						blue_pool[ blue_pool_size ] = 1;

						if( simulation_grid[ (row - 1) * 6 + col - 1 ] == -2 ) // if it is a Bo though
						{
							int i = 0;
							while( blue_pool[ i ] == 2 )
								++i;
							blue_pool[ i ] = 2;
						}

						++blue_pool_size;
					}
				}

				simulation_grid[ (row - 1) * 6 + col - 1 ] = 0;
			}
		}

		// Bottom Left
		if( row + 1 <= 5 )
		{
			if( simulation_grid[ (row + 1) * 6 + col - 1 ] != 0 &&
			    std::abs( simulation_grid[ (row + 1) * 6 + col - 1 ] ) <=
			    std::abs( simulation_grid[ index ] ) &&
			    (col - 2 < 0 || row + 2 > 5 || simulation_grid[ (row + 2) * 6 + col - 2 ] == 0))
			{
				if( col - 2 >= 0 && row + 2 <= 5 )
				{
					if( simulation_grid[ (row + 2) * 6 + col - 2 ] == 0 )
						simulation_grid[ (row + 2) * 6 + col - 2 ] = simulation_grid[ (row + 1) * 6 + col - 1 ];
				}
				else // if a piece has been ejected out the board, we need to put it into the right pool
				{
					if( simulation_grid[ (row + 1) * 6 + col - 1 ] > 0 )
					{
						// in every case, either a Po or a Bo has been ejected out the board
						red_pool[ red_pool_size ] = 1;

						if( simulation_grid[ (row + 1) * 6 + col - 1 ] == 2 ) // if it is a Bo though
						{
							int i = 0;
							while( red_pool[ i ] == 2 )
								++i;
							red_pool[ i ] = 2;
						}

						++red_pool_size;
					}
					else
					{
						blue_pool[ blue_pool_size ] = 1;

						if( simulation_grid[ (row + 1) * 6 + col - 1 ] == -2 ) // if it is a Bo though
						{
							int i = 0;
							while( blue_pool[ i ] == 2 )
								++i;
							blue_pool[ i ] = 2;
						}

						++blue_pool_size;
					}
				}

				simulation_grid[ (row + 1) * 6 + col - 1 ] = 0;
			}
		}

		// Left
		if( simulation_grid[ row * 6 + col - 1 ] != 0 &&
		    std::abs( simulation_grid[ row * 6 + col - 1 ] ) <= std::abs( simulation_grid[ index ] ) &&
		    (col - 2 < 0 || simulation_grid[ row * 6 + col - 2 ] == 0))
		{
			if( col - 2 >= 0 )
			{
				if( simulation_grid[ row * 6 + col - 2 ] == 0 )
					simulation_grid[ row * 6 + col - 2 ] = simulation_grid[ row * 6 + col - 1 ];
			}
			else // if a piece has been ejected out the board, we need to put it into the right pool
			{
				if( simulation_grid[ row * 6 + col - 1 ] > 0 )
				{
					// in every case, either a Po or a Bo has been ejected out the board
					red_pool[ red_pool_size ] = 1;

					if( simulation_grid[ row * 6 + col - 1 ] == 2 ) // if it is a Bo though
					{
						int i = 0;
						while( red_pool[ i ] == 2 )
							++i;
						red_pool[ i ] = 2;
					}

					++red_pool_size;
				}
				else
				{
					blue_pool[ blue_pool_size ] = 1;

					if( simulation_grid[ row * 6 + col - 1 ] == -2 ) // if it is a Bo though
					{
						int i = 0;
						while( blue_pool[ i ] == 2 )
							++i;
						blue_pool[ i ] = 2;
					}

					++blue_pool_size;
				}
			}
			simulation_grid[ row * 6 + col - 1 ] = 0;
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
				if( col + 2 <= 5 && row - 2 >= 0 )
				{
					if( simulation_grid[ (row - 2) * 6 + col + 2 ] == 0 )
						simulation_grid[ (row - 2) * 6 + col + 2 ] = simulation_grid[ (row - 1) * 6 + col + 1 ];
				}
				else // if a piece has been ejected out the board, we need to put it into the right pool
				{
					if( simulation_grid[ (row - 1) * 6 + col + 1 ] > 0 )
					{
						// in every case, either a Po or a Bo has been ejected out the board
						red_pool[ red_pool_size ] = 1;

						if( simulation_grid[ (row - 1) * 6 + col + 1 ] == 2 ) // if it is a Bo though
						{
							int i = 0;
							while( red_pool[ i ] == 2 )
								++i;
							red_pool[ i ] = 2;
						}

						++red_pool_size;
					}
					else
					{
						blue_pool[ blue_pool_size ] = 1;

						if( simulation_grid[ (row - 1) * 6 + col + 1 ] == -2 ) // if it is a Bo though
						{
							int i = 0;
							while( blue_pool[ i ] == 2 )
								++i;
							blue_pool[ i ] = 2;
						}

						++blue_pool_size;
					}
				}

				simulation_grid[ (row - 1) * 6 + col + 1 ] = 0;
			}
		}

		// Bottom Right
		if( row+1 <= 5 )
		{
			if( simulation_grid[ ( row+1 )*6 + col+1 ] != 0 &&
			    std::abs( simulation_grid[ ( row+1 )*6 + col+1 ] ) <= std::abs( simulation_grid[ index ] ) &&
			    ( col+2 > 5 || row+2 > 5 || simulation_grid[ ( row+2 )*6 + col+2 ] == 0 ) )
			{
				if( col + 2 <= 5 && row + 2 <= 5 )
				{
					if( simulation_grid[ (row + 2) * 6 + col + 2 ] == 0 )
						simulation_grid[ (row + 2) * 6 + col + 2 ] = simulation_grid[ (row + 1) * 6 + col + 1 ];
				}
				else // if a piece has been ejected out the board, we need to put it into the right pool
				{
					if( simulation_grid[ (row + 1) * 6 + col + 1 ] > 0 )
					{
						// in every case, either a Po or a Bo has been ejected out the board
						red_pool[ red_pool_size ] = 1;

						if( simulation_grid[ (row + 1) * 6 + col + 1 ] == 2 ) // if it is a Bo though
						{
							int i = 0;
							while( red_pool[ i ] == 2 )
								++i;
							red_pool[ i ] = 2;
						}

						++red_pool_size;
					}
					else
					{
						blue_pool[ blue_pool_size ] = 1;

						if( simulation_grid[ (row + 1) * 6 + col + 1 ] == -2 ) // if it is a Bo though
						{
							int i = 0;
							while( blue_pool[ i ] == 2 )
								++i;
							blue_pool[ i ] = 2;
						}

						++blue_pool_size;
					}
				}

				simulation_grid[ (row + 1) * 6 + col + 1 ] = 0;
			}
		}

		// Right
		if( simulation_grid[ row*6 + col+1 ] != 0 &&
		    std::abs( simulation_grid[ row*6 + col+1 ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( col+2 > 5 || simulation_grid[ row*6 + col+2 ] == 0 ))
		{
			if( col + 2 <= 5 )
			{
				if( simulation_grid[ row * 6 + col + 2 ] == 0 )
					simulation_grid[ row * 6 + col + 2 ] = simulation_grid[ row * 6 + col + 1 ];
			}
			else // if a piece has been ejected out the board, we need to put it into the right pool
			{
				if( simulation_grid[ row * 6 + col + 1 ] > 0 )
				{
					// in every case, either a Po or a Bo has been ejected out the board
					red_pool[ red_pool_size ] = 1;

					if( simulation_grid[ row * 6 + col + 1 ] == 2 ) // if it is a Bo though
					{
						int i = 0;
						while( red_pool[ i ] == 2 )
							++i;
						red_pool[ i ] = 2;
					}

					++red_pool_size;
				}
				else
				{
					blue_pool[ blue_pool_size ] = 1;

					if( simulation_grid[ row * 6 + col + 1 ] == -2 ) // if it is a Bo though
					{
						int i = 0;
						while( blue_pool[ i ] == 2 )
							++i;
						blue_pool[ i ] = 2;
					}

					++blue_pool_size;
				}
			}

			simulation_grid[ row * 6 + col + 1 ] = 0;
		}
	}

	// Top
	if( row-1 >= 0 )
	{
		if( simulation_grid[ ( row-1 )*6 + col ] != 0 &&
		    std::abs( simulation_grid[ ( row-1 )*6 + col ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( row-2 < 0 || simulation_grid[ ( row-2 )*6 + col ] == 0 ))
		{
			if( row - 2 >= 0 )
			{
				if( simulation_grid[ (row - 2) * 6 + col ] == 0 )
					simulation_grid[ (row - 2) * 6 + col ] = simulation_grid[ (row - 1) * 6 + col ];
			}
			else // if a piece has been ejected out the board, we need to put it into the right pool
			{
				if( simulation_grid[ (row - 1) * 6 + col ] > 0 )
				{
					// in every case, either a Po or a Bo has been ejected out the board
					red_pool[ red_pool_size ] = 1;

					if( simulation_grid[ (row - 1) * 6 + col ] == 2 ) // if it is a Bo though
					{
						int i = 0;
						while( red_pool[ i ] == 2 )
							++i;
						red_pool[ i ] = 2;
					}

					++red_pool_size;
				}
				else
				{
					blue_pool[ blue_pool_size ] = 1;

					if( simulation_grid[ (row - 1) * 6 + col ] == -2 ) // if it is a Bo though
					{
						int i = 0;
						while( blue_pool[ i ] == 2 )
							++i;
						blue_pool[ i ] = 2;
					}

					++blue_pool_size;
				}
			}

			simulation_grid[ (row - 1) * 6 + col ] = 0;
		}
	}

	// Bottom
	if( row+1 <= 5 )
	{
		if( simulation_grid[ ( row+1 )*6 + col ] != 0 &&
		    std::abs( simulation_grid[ ( row+1 )*6 + col ] ) <= std::abs( simulation_grid[ index ] ) &&
		    ( row+2 > 5 || simulation_grid[ ( row+2 )*6 + col ] == 0 ))
		{
			if( row + 2 <= 5 )
			{
				if( simulation_grid[ (row + 2) * 6 + col ] == 0 )
					simulation_grid[ (row + 2) * 6 + col ] = simulation_grid[ (row + 1) * 6 + col ];
			}
			else // if a piece has been ejected out the board, we need to put it into the right pool
			{
				if( simulation_grid[ (row + 1) * 6 + col ] > 0 )
				{
					// in every case, either a Po or a Bo has been ejected out the board
					red_pool[ red_pool_size ] = 1;

					if( simulation_grid[ (row + 1) * 6 + col ] == 2 ) // if it is a Bo though
					{
						int i = 0;
						while( red_pool[ i ] == 2 )
							++i;
						red_pool[ i ] = 2;
					}

					++red_pool_size;
				}
				else
				{
					blue_pool[ blue_pool_size ] = 1;

					if( simulation_grid[ (row + 1) * 6 + col ] == -2 ) // if it is a Bo though
					{
						int i = 0;
						while( blue_pool[ i ] == 2 )
							++i;
						blue_pool[ i ] = 2;
					}

					++blue_pool_size;
				}
			}

			simulation_grid[ (row + 1) * 6 + col ] = 0;
		}
	}

	auto groups = get_promotions( simulation_grid, blue_turn, blue_pool_size, red_pool_size );
	std::vector< Position > group_to_promote;

	if( groups.size() > 0 )
	{
		if( groups.size() == 1 )
			group_to_promote = groups[0];
		else
		{
			randutils::mt19937_rng rng;
			auto scores = heuristic_promotions( simulation_grid, groups );
			double best_score = -10000.0;
			std::vector<int> best_groups;

			for( int i = 0; i < groups.size(); ++i )
			{
				if(groups[i].size() == 1)
					ALOG("Group[%d] {(%d,%d)} score = %.2f\n", i, groups[i][0].row, groups[i][0].column, scores[i]);
				else
					ALOG("Group[%d] {(%d,%d), (%d,%d), (%d,%d)} score = %.2f\n", i, groups[i][0].row, groups[i][0].column, groups[i][1].row, groups[i][1].column, groups[i][2].row, groups[i][2].column, scores[i]);

				if( best_score < scores[ i ] )
				{
					best_score = scores[ i ];
					best_groups.clear();
					best_groups.push_back( i );
					ALOG("Group[%d] is the new best group\n", i);
				}
				else
					if( best_score == scores[ i ] )
					{
						best_groups.push_back( i );
						ALOG("Group[%d] is ex aequo\n", i);
					}
			}

			auto picked_group = rng.pick( best_groups );
			ALOG("Group[%d] has been selected\n", picked_group);
			group_to_promote = groups[ picked_group ];
		}

		for( auto pos : group_to_promote )
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