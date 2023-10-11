//
// Created by flo on 10/10/2023.
//

#include "heuristics.hpp"

//#include <android/log.h>
//#define ALOG(...) __android_log_print(ANDROID_LOG_INFO, "pobotag C++", __VA_ARGS__)

double heuristic_state( jbyte *const simulation_grid,
                        jboolean blue_turn,
                        jbyte *const blue_pool,
                        jint blue_pool_size,
                        jbyte *const red_pool,
                        jint red_pool_size )
{
	double score = 0.0;

	int count_blue_po = 0;
	int count_blue_bo = 0;
	int count_red_po = 0;
	int count_red_bo = 0;

	int count_blue_central_po = 0;
	int count_blue_central_bo = 0;
	int count_red_central_po = 0;
	int count_red_central_bo = 0;

	int count_blue_border_po = 0;
	int count_blue_border_bo = 0;
	int count_red_border_po = 0;
	int count_red_border_bo = 0;

	for( int row = 0 ; row < 6 ; ++row )
		for( int col = 0 ; col < 6 ; ++col )
		{
			int index = row*6 + col;

			if( simulation_grid[ index ] == -1 )
				++count_blue_po;
			if( simulation_grid[ index ] == -2 )
				++count_blue_bo;
			if( simulation_grid[ index ] == 1 )
				++count_red_po;
			if( simulation_grid[ index ] == 2 )
				++count_red_bo;

			if( row == 0 || row == 5 )
			{
				if( simulation_grid[ index ] == -1 )
					++count_blue_border_po;
				if( simulation_grid[ index ] == -2 )
					++count_blue_border_bo;
				if( simulation_grid[ index ] == 1 )
					++count_red_border_po;
				if( simulation_grid[ index ] == 2 )
					++count_red_border_bo;
			}
			else {
				if( col == 0 || col == 5 ) {
					if( simulation_grid[ index ] == -1 )
						++count_blue_border_po;
					if( simulation_grid[ index ] == -2 )
						++count_blue_border_bo;
					if( simulation_grid[ index ] == 1 )
						++count_red_border_po;
					if( simulation_grid[ index ] == 2 )
						++count_red_border_bo;
				}
				else {
					if( index == 14 || index == 15 || index == 20 || index == 21 ) {
						if( simulation_grid[ index ] == -1 )
							++count_blue_central_po;
						if( simulation_grid[ index ] == -2 )
							++count_blue_central_bo;
						if( simulation_grid[ index ] == 1 )
							++count_red_central_po;
						if( simulation_grid[ index ] == 2 )
							++count_red_central_bo;
					}
				}
			}
		}

	int jump_forward;
	// horizontal scans
	for( int row = 0; row < 6; ++row )
	{
		for( int col = 0; col < 5; col = col + 1 + jump_forward )
		{
			jump_forward = 0;
			if( simulation_grid[row * 6 + col] != 0 )
			{
				auto partial_score = compute_partial_score( row,
				                                            col,
				                                            RIGHT,
				                                            jump_forward,
				                                            simulation_grid,
				                                            blue_turn,
				                                            blue_pool,
				                                            blue_pool_size,
				                                            red_pool,
				                                            red_pool_size );
				score += partial_score;
			}
		}
	}

	// vertical scans
	for( int col = 0; col < 6; ++col )
	{
		for( int row = 0; row < 5; row = row + 1 + jump_forward )
		{
			jump_forward = 0;
			if( simulation_grid[ row*6 + col ] != 0 )
			{
				auto partial_score= compute_partial_score( row,
				                                           col,
				                                           BOTTOM,
				                                           jump_forward,
				                                           simulation_grid,
				                                           blue_turn,
				                                           blue_pool,
				                                           blue_pool_size,
				                                           red_pool,
				                                           red_pool_size );
				score += partial_score;
			}
		}
	}

	// ascendant diagonal scans
	std::vector<int> ascendant{ 6,
	                            12, 7,
	                            18, 13, 8,
	                            24, 19, 14, 9,
	                            30, 25, 20, 15, 10,
	                            31, 26, 21, 16,
	                            32, 27, 22,
	                            33, 28,
	                            34 };
	int fake_jump;
	for( auto index: ascendant )
		if( simulation_grid[index] != 0 )
		{
			auto partial_score = compute_partial_score( index / 6,
			                                            index % 6,
			                                            TOPRIGHT,
			                                            fake_jump,
			                                            simulation_grid,
			                                            blue_turn,
			                                            blue_pool,
			                                            blue_pool_size,
			                                            red_pool,
			                                            red_pool_size );
			score += partial_score;
		}

	// descendant diagonal scans
	std::vector<int> descendant{ 24,
	                             18, 25,
	                             12, 19, 26,
	                             6, 13, 20, 27,
	                             0, 7, 14, 21, 28,
	                             1, 8, 15, 22,
	                             2, 9, 16,
	                             3, 10,
	                             4 };

	for( auto index: descendant )
		if( simulation_grid[index] != 0 )
		{
			auto partial_score = compute_partial_score( index / 6,
			                                            index % 6,
			                                            BOTTOMRIGHT,
			                                            fake_jump,
			                                            simulation_grid,
			                                            blue_turn,
			                                            blue_pool,
			                                            blue_pool_size,
			                                            red_pool,
			                                            red_pool_size );
			score += partial_score;
		}

	int diff_po = 0;
	int diff_bo = 0;
	int diff_po_central = 0;
	int diff_bo_central = 0;
	int diff_po_border = 0;
	int diff_bo_border = 0;

	int total_blue_bo = count_blue_bo;
	for( int i = 0 ; i < blue_pool_size ; ++i )
		if( blue_pool[i] == 2 )
			++total_blue_bo;

	int total_red_bo = count_red_bo;
	for( int i = 0 ; i < red_pool_size ; ++i )
		if( red_pool[i] == 2 )
			++total_red_bo;

	int diff_total_bo = 0;

	if( blue_turn )
	{
		diff_po = count_blue_po - count_red_po;
		diff_bo = count_blue_bo - count_red_bo;

		diff_po_central = count_blue_central_po - count_red_central_po;
		diff_bo_central = count_blue_central_bo - count_red_central_bo;

		diff_po_border = count_red_border_po - count_blue_border_po;
		diff_bo_border = count_red_border_bo - count_blue_border_bo;

		diff_total_bo = total_blue_bo - total_red_bo;
	}
	else
	{
		diff_po = count_red_po - count_blue_po;
		diff_bo = count_red_bo - count_blue_bo;

		diff_po_central = count_red_central_po - count_blue_central_po;
		diff_bo_central = count_red_central_bo - count_blue_central_bo;

		diff_po_border = count_blue_border_po - count_red_border_po;
		diff_bo_border = count_blue_border_bo - count_red_border_bo;

		diff_total_bo = total_red_bo - total_blue_bo;
	}

	score += 3 * (diff_bo + diff_bo_central + diff_bo_border)
	         + 2 * diff_total_bo
	         + diff_po + diff_po_central + diff_po_border;
	return score;
}

std::vector<double> heuristic_graduation( jbyte *const simulation_grid,
                                          std::vector<std::vector<Position> > groups,
                                          jboolean blue_turn,
                                          jbyte *const blue_pool,
                                          jint blue_pool_size,
                                          jbyte *const red_pool,
                                          jint red_pool_size )
{
	std::vector<double> scores( groups.size(), 0.0 );
	bool eight_pieces = groups.size() >= 8;

	for( int i = 0 ; i < groups.size() ; ++i )
	{
		auto group = groups[i];
		if( group.size() == 1 )
		{
			if( std::abs( simulation_grid[ 6*group[0].row + group[0].column ] ) == 1 ) // if it is a Po
			{
				scores[i] += 5;
				if( is_on_border( group ) )
					scores[i] += 2;
				//TODO: malus if in a 3-piece group, regarding the number of po
			}
		}
		else
		{
			int count_po = 0;
			for( auto pos : group )
				if( std::abs( simulation_grid[ 6*pos.row + pos.column ] ) == 1 )
					++count_po;

			scores[i] += count_po;
			if( count_po == 3 && is_on_border( group ) )
				scores[i] += 10;
		}
	}

	return scores;
}