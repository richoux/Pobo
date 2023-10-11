//
// Created by flo on 21/06/2023.
//

#include "pobo_objective.hpp"
#include "../helpers.hpp"
#include "../heuristics.hpp"

//#include <android/log.h>
//#define ALOG(...) __android_log_print(ANDROID_LOG_INFO, "pobotag C++", __VA_ARGS__)

PoboObjective::PoboObjective( const std::vector<ghost::Variable>& variables,
															jbyte * const grid,
															jboolean blue_turn,
															jbyte *const blue_pool,
															jint blue_pool_size,
															jbyte *const red_pool,
															jint red_pool_size )
				: Maximize( variables, "pobo Heuristic" ),
				  _grid( grid ),
				  _blue_turn( blue_turn ),
					_blue_pool( blue_pool ),
					_blue_pool_size( blue_pool_size ),
					_red_pool( red_pool ),
					_red_pool_size( red_pool_size )
{ }

double PoboObjective::required_cost( const std::vector<ghost::Variable *> &variables ) const
{
	double score = 0.;

	for( int i = 0; i < 36; ++i )
		_simulation_grid[i] = _grid[i];

//	std::string s = "Before simulation\n";
//	for (int i = 0 ; i < 36 ; ++i)
//	{
//		int p = _simulation_grid[i];
//		if (p < 0)
//			p += 10;
//		s += std::to_string(p) + " ";
//		if( (i + 1) % 6 == 0 )
//			s += "\n";
//	}
//	ALOG("%s", s.c_str());

	simulate_move( variables,
								 _simulation_grid,
								 _blue_turn,
								 _blue_pool,
								 _blue_pool_size,
								 _red_pool,
								 _red_pool_size );

//	s = "After simulation\n";
//	for (int i = 0 ; i < 36 ; ++i)
//	{
//		int p = _simulation_grid[i];
//		if (p < 0)
//			p += 10;
//		s += std::to_string(p) + " ";
//		if( (i + 1) % 6 == 0 )
//			s += "\n";
//	}
//	ALOG("%s", s.c_str());

	score = heuristic_state( _simulation_grid,
	                         _blue_turn,
	                         _blue_pool,
	                         _blue_pool_size,
	                         _red_pool,
	                         _red_pool_size );

//	std::cout << "diff_pieces: " << diff_pieces
//	          << ", diff_pieces_central: " << diff_pieces_central
//						<< ", diff_pieces_border: " << diff_pieces_border << "\n";
//
//	std::cout << "Score for piece " << variables[0]->get_value() * (_blue_turn ? -1 : 1)
//						<< " at (" << (char)('a'+variables[1]->get_value()) << "," << variables[2]->get_value()+1 << "): "
//						<< score << "\n";

	return score;
}