//
// Created by flo on 10/10/2023.
//

#include <algorithm>
#include "heuristics.hpp"

#include <android/log.h>
//*
#define ALOG(...)
/*/
#define ALOG( ... ) __android_log_print(ANDROID_LOG_INFO, "pobotag C++", __VA_ARGS__)
//*/

double compute_partial_score( int from_row,
                              int from_col,
                              Direction direction,
                              int& jump_forward,
                              jbyte * const simulation_grid,
                              jboolean blue_turn,
                              jbyte * const blue_pool,
                              jint& blue_pool_size,
                              jbyte * const red_pool,
                              jint& red_pool_size )
{
	double score = 0.;
	bool is_player_piece = ( ( simulation_grid[ from_row*6 + from_col ] < 0 && blue_turn ) ||
	                         ( simulation_grid[ from_row*6 + from_col ] > 0 && !blue_turn ) );

	bool do_current_player_has_bo_in_pool = false;
	bool do_opponent_has_bo_in_pool = false;

	for( int i = 0 ; i < blue_pool_size ; ++i )
		if( blue_pool[i] == 2 )
		{
			if( blue_turn )
				do_current_player_has_bo_in_pool = true;
			else
				do_opponent_has_bo_in_pool = true;
		}

	for( int i = 0 ; i < red_pool_size ; ++i )
		if( red_pool[i] == 2 )
		{
			if( blue_turn )
				do_opponent_has_bo_in_pool = true;
			else
				do_current_player_has_bo_in_pool = true;
		}

	if( check_three_in_a_row( from_row, from_col, direction, BO, simulation_grid ))
	{
		score += is_player_piece ? 800 : -800; // 250/-250
		ALOG( "compute_partial_score 3 Bo aligned from (%d,%d), score=%.2f", from_row, from_col,
		      score );
		jump_forward = 2;
	}
	else
	{
		if( check_three_in_a_row( from_row, from_col, direction, PO, simulation_grid ))
		{
			score += is_player_piece ? 40 : -44;
//			score += is_player_piece ? 3 : -3;
			ALOG( "compute_partial_score 3 Po aligned from (%d,%d), score=%.2f", from_row, from_col,
			      score );
			jump_forward = 2;
		}
		else
		{
			if( check_three_in_a_row( from_row, from_col, direction, WHATEVER, simulation_grid ))
			{
				if( is_two_unblocked_bo_and_one_po(from_row, from_col, direction, simulation_grid) )
				{
					// to be treated as a 2 unblocked, not-in-the-corner, aligned Bo
					if( is_player_piece )
					{
						if( is_on_border( from_row, from_col, direction, 2, true ) && do_opponent_has_bo_in_pool )
							score += 0; // this can create the unique situation where making 2 lines of Bo on the border is not considered as interesting
						else
							if( do_current_player_has_bo_in_pool )
								score += 60;
							else
								score += 20;
					}
					else
					{
						if( do_opponent_has_bo_in_pool )
							score += -300; // because there is a severe risk to loose the game
						else
							score += -40;
					}
				}
				else
				{
					score += count_Po_in_a_row( from_row, from_col, direction, simulation_grid ) *
					         (is_player_piece ? 7 : -11); // 10/-11
//				score += count_Po_in_a_row( from_row, from_col, direction, simulation_grid );
					ALOG( "compute_partial_score 3 pieces aligned from (%d,%d), score=%.2f", from_row,
					      from_col,
					      score );
				}
				jump_forward = 1;
			}
			else
			{
				if( check_two_in_a_row( from_row, from_col, direction, BO, simulation_grid ))
				{
					if( is_two_in_a_row_in_corner( from_row, from_col, direction ))
					{
						score += is_player_piece ? -5 : 10;
//						score += is_player_piece ? 0 : 10;
						ALOG( "compute_partial_score 2 Bo in the corner from (%d,%d), score=%.2f", from_row,
						      from_col, score );
					}
					else
					{
						if( is_two_in_a_row_blocked( from_row, from_col, direction, simulation_grid ))
						{
							score += is_player_piece ? -5 : 0; // -5/10
							ALOG( "compute_partial_score 2 Bo aligned from (%d,%d) but blocked, score=%.2f",
							      from_row, from_col, score );
						}
						else // 2 Bo aligned, unblocked and not in the corner
						{
							if( is_player_piece )
							{
								if( is_on_border( from_row, from_col, direction, 2, true ) && do_opponent_has_bo_in_pool )
									score += 0; // this can create the unique situation where making 2 lines of Bo on the border is not considered as interesting
								else
									if( do_current_player_has_bo_in_pool )
										score += 60;
									else
										score += 20;
							}
							else
							{
								if( do_opponent_has_bo_in_pool )
									score += -300; // because there is a severe risk to loose the game
								else
									score += -40;
							}
							ALOG( "compute_partial_score 2 Bo aligned from (%d,%d), score=%.2f", from_row,
							      from_col, score );
						}
						jump_forward = 1;
					}
				}
				else // not 2 Bo aligned
				{
					if( check_two_in_a_row( from_row, from_col, direction, PO, simulation_grid ))
					{
						if( is_two_in_a_row_in_corner( from_row, from_col, direction ))
						{
							score += is_player_piece ? -1 : 5;
							ALOG( "compute_partial_score 2 Po in the corner from (%d,%d) but blocked, score=%.2f",
							      from_row, from_col, score );
						}
						else
						{
							if( is_two_in_a_row_blocked( from_row, from_col, direction, simulation_grid ))
							{
								score += is_player_piece ? -1 : 0;
								ALOG( "compute_partial_score 2 Po aligned from (%d,%d) but blocked, score=%.2f",
								      from_row, from_col, score );
							}
							else // 2 Po aligned, unblocked and not in the corner
							{
								if( is_player_piece )
								{
									if( is_on_border( from_row, from_col, direction, 2, true ) )
										score += 0;
									else
										score += 15; //20
								}
								else
									score += -22;
								ALOG( "compute_partial_score 2 Po aligned from (%d,%d), score=%.2f", from_row,
								      from_col, score );
							}
							jump_forward = 1;
						}
					}
					else
						if( check_two_in_a_row( from_row, from_col, direction, WHATEVER, simulation_grid ) )
						{
							if( is_two_in_a_row_in_corner( from_row, from_col, direction ))
							{
								score += is_player_piece ? -1 : 5;
								ALOG( "compute_partial_score 2 pieces in the corner from (%d,%d) but blocked, score=%.2f",
								      from_row, from_col, score );
							}
							else
							{
								if( is_two_in_a_row_blocked( from_row, from_col, direction, simulation_grid ))
								{
									score += is_player_piece ? -1 : 0;
									ALOG( "compute_partial_score 2 pieces aligned from (%d,%d) but blocked, score=%.2f",
									      from_row, from_col, score );
								}
								else // 2 pieces aligned, unblocked and not in the corner
								{
									if( is_player_piece )
									{
										if( is_on_border( from_row, from_col, direction, 2, true ) )
											score += 0;
										else
											score += 7; //10
									}
									else
										score += -11;
									ALOG( "compute_partial_score 2 pieces aligned from (%d,%d), score=%.2f", from_row,
									      from_col, score );
								}
								jump_forward = 1;
							}
						}
				}
			}
		}
	}

	return score;
}

double heuristic_state( jbyte *const simulation_grid,
                        jboolean blue_turn,
                        jbyte *const blue_pool,
                        jint blue_pool_size,
                        jbyte *const red_pool,
                        jint red_pool_size )
{
	std::string ss = "";
	for( int i = 0 ; i < 36 ; ++i )
	{
		int p = simulation_grid[i];
		if( p < 0 )
			p += 10;

		ss += std::to_string(p) + " ";

		if( (i+1) % 6 == 0 )
		{
			ss += "\n";
		}
	}
	ALOG("%s", ss.c_str());
	ss = "";
	for( int i = 0 ; i < blue_pool_size ; ++i )
		ss += std::to_string(blue_pool[i] ) + " ";
	ALOG("Blue pool: %s", ss.c_str());
	ss = "";
	for( int i = 0 ; i < red_pool_size ; ++i )
		ss += std::to_string(red_pool[i] ) + " ";
	ALOG("Red pool: %s", ss.c_str());
	ALOG("\n");

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
			if( simulation_grid[ row * 6 + col ] != 0 )
			{
				auto partial_score = compute_partial_score( row,
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
	for( int index = 0 ; index < ascendant.size() ; index = index + 1 + jump_forward )
	{
		jump_forward = 0;
		if( simulation_grid[ ascendant[ index ] ] != 0 )
		{
			auto partial_score = compute_partial_score( ascendant[ index ] / 6,
			                                            ascendant[ index ] % 6,
			                                            TOPRIGHT,
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

	for( int index = 0 ; index < descendant.size() ; index = index + 1 + jump_forward )
	{
		jump_forward = 0;
		if( simulation_grid[ descendant[ index ] ] != 0 )
		{
			auto partial_score = compute_partial_score( descendant[ index ] / 6,
			                                            descendant[ index ] % 6,
			                                            BOTTOMRIGHT,
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

	ALOG("diff_total_bo=%d\n"
			 "diff_bo=%d\n"
			 "diff_bo_central=%d\n"
			 "diff_bo_border=%d\n"
	     "diff_po=%d\n"
	     "diff_po_central=%d\n"
	     "diff_po_border=%d\n",
	     diff_total_bo,
			 diff_bo,
			 diff_bo_central,
			 diff_bo_border,
			 diff_po,
			 diff_po_central,
			 diff_po_border
			 );

	score += 25*diff_total_bo //20
	         + 9*diff_bo + 3*(diff_bo_central + diff_bo_border)
	         + 3*diff_po + diff_po_central + diff_po_border;

	ALOG("score before normalization=%.2f\n", score);

	// Score normalization [-1,1]
	score = std::min( 400.0, std::max( -400.0, score ) ) / 400;

	ALOG("score=%.3f\n", score);
	ALOG("\n");

	return score;
}

std::vector<double> heuristic_graduation( jbyte *const simulation_grid,
                                          std::vector<std::vector<Position> > groups )
{
	std::vector<double> scores( groups.size(), 0.0 );

	for( int i = 0 ; i < groups.size() ; ++i )
	{
		auto group = groups[i];
		if( group.size() == 1 )
		{
			if( std::abs( simulation_grid[ 6*group[0].row + group[0].column ] ) == 1 ) // if it is a Po
			{
				scores[i] += 5;
				if( is_fully_on_border( group ) )
					scores[i] += 2;
				else
					if( is_in_center( group[0] ) )
						scores[i] += -2;

				if( next_to_other_own_pieces( simulation_grid, group[0] ) )
					scores[i] += -3;

				if( is_blocking( simulation_grid, group[0] ) )
					scores[i] += -10;
			}

			//TODO: sometimes removing a Bo is necessary (like in the situation of the comment above)
		}
		else
		{
			int count_po = 0;
			for( auto pos : group )
				if( std::abs( simulation_grid[ 6*pos.row + pos.column ] ) == 1 )
					++count_po;

			scores[i] += count_po;
			if( count_po == 3 )
			{
				if( is_fully_on_border( group ) )
					scores[i] += 10;
				else
					if( is_partially_on_border( group ) )
						scores[i] += 1;
			}
		}
	}

	return scores;
}