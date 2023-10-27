#include <jni.h>

#include <vector>

#include "lib/include/ghost/solver.hpp"
#include "model/builder.hpp"
#include "lib/include/ghost/thirdparty/randutils.hpp"
#include "heuristics.hpp"

// From https://manski.net/2012/05/logging-from-c-on-android/
#include <android/log.h>
//*
#define ALOG(...)
/*/
#define ALOG( ... ) __android_log_print(ANDROID_LOG_INFO, "pobotag C++", __VA_ARGS__)
//*/

using namespace std::literals::chrono_literals;

// From https://www.baeldung.com/jni
// See also https://developer.android.com/training/articles/perf-jni

extern "C"
JNIEXPORT jintArray JNICALL
Java_fr_richoux_pobo_engine_ai_MCTS_1GHOST_00024Companion_ghost_1solver_1call(
				JNIEnv *env,
				jobject thiz,
				jbyteArray k_grid,
				jbyteArray k_blue_pool,
				jbyteArray k_red_pool,
				jint k_blue_pool_size,
				jint k_red_pool_size,
				jboolean k_blue_turn,
				jbyteArray k_to_remove_row,
				jbyteArray k_to_remove_col,
				jbyteArray k_to_remove_p,
				jint k_number_to_remove )
{
	randutils::mt19937_rng rng;

	// Inputs //
	jbyte cpp_grid[36];
	jbyte blue_pool[k_blue_pool_size];
  jbyte red_pool[k_red_pool_size];

	env->GetByteArrayRegion( k_grid, 0, 36, cpp_grid );
	env->GetByteArrayRegion( k_blue_pool, 0, k_blue_pool_size, blue_pool );
	env->GetByteArrayRegion( k_red_pool, 0, k_red_pool_size, red_pool );

	jbyte to_remove_row[k_number_to_remove];
	env->GetByteArrayRegion( k_to_remove_row, 0, k_number_to_remove, to_remove_row );

	jbyte to_remove_col[k_number_to_remove];
	env->GetByteArrayRegion( k_to_remove_col, 0, k_number_to_remove, to_remove_col );

	jbyte to_remove_p[k_number_to_remove];
	env->GetByteArrayRegion( k_to_remove_p, 0, k_number_to_remove, to_remove_p );

	// Move search //
	Builder builder( cpp_grid,
                     blue_pool,
                     k_blue_pool_size,
                     red_pool,
                     k_red_pool_size,
                     k_blue_turn,
                     to_remove_row,
                     to_remove_col,
                     to_remove_p,
                     k_number_to_remove);

	ghost::Solver solver(builder);

	double cost = std::numeric_limits<int>::min();
	std::vector<int> solution;
	/*
	bool success = solver.fast_search( cost, solution, 1 );
	/*/
	std::vector<double> costs;
	std::vector< std::vector<int> > solutions;

	bool success = solver.complete_search( costs, solutions );

	std::vector<int> best_solutions_index;
	for( int i = 0 ; i < static_cast<int>( solutions.size() ) ; ++i )
	{
//		ALOG("Solution %d: [%d, (%d,%d)], score=%f", i, solutions[i][0], solutions[i][1], solutions[i][2], costs[i]);

		if( cost < costs[i] )
		{
			best_solutions_index.clear();
			best_solutions_index.push_back( i );
			cost = costs[i];
		}
		else
			if( cost == costs[i] )
				best_solutions_index.push_back( i );
	}

	if( !success )
	{
		solution[0] = 42;
		solution[1] = 0;
		solution[2] = 0;
	}
	else
	{
		int index = rng.pick( best_solutions_index );
		solution = solutions[index];
	}
	//*/

	// Output: Move (Piece + Position) + Cost
	solution.push_back(static_cast<int>(cost) );
	jintArray sol = env->NewIntArray( 4 );
	env->SetIntArrayRegion( sol, 0, 4, (jint *) &solution[0] );

	return sol;
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_fr_richoux_pobo_engine_ai_MCTS_1GHOST_00024Companion_heuristic_1state_1cpp(
				JNIEnv *env,
				jobject thiz,
				jbyteArray k_grid,
				jboolean k_blue_turn,
				jbyteArray k_blue_pool,
				jint k_blue_pool_size,
				jbyteArray k_red_pool,
				jint k_red_pool_size )
{
	jbyte cpp_grid[36];
	jbyte blue_pool[k_blue_pool_size];
	jbyte red_pool[k_red_pool_size];

	env->GetByteArrayRegion( k_grid, 0, 36, cpp_grid );
	env->GetByteArrayRegion( k_blue_pool, 0, k_blue_pool_size, blue_pool );
	env->GetByteArrayRegion( k_red_pool, 0, k_red_pool_size, red_pool );

	jdouble score = heuristic_state( cpp_grid,
	                                 k_blue_turn,
	                                 blue_pool,
	                                 k_blue_pool_size,
	                                 red_pool,
	                                 k_red_pool_size );

	return score;
}
extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_fr_richoux_pobo_engine_ai_MCTS_1GHOST_00024Companion_compute_1graduations_1cpp( JNIEnv *env,
                                          jobject thiz,
																					jbyteArray k_grid,
																					jboolean k_blue_turn,
																					jint k_blue_pool_size,
																					jint k_red_pool_size )
{
	jbyte cpp_grid[36];

	env->GetByteArrayRegion( k_grid, 0, 36, cpp_grid );

	ALOG("%d", __LINE__);
	auto groups = get_graduations( cpp_grid,
																 k_blue_turn,
																 k_blue_pool_size,
																 k_red_pool_size );
	ALOG("%d", __LINE__);
	//std::vector< Position > group_to_graduate;
	std::vector<double> scores;

	if( groups.size() > 0 )
	{
		if( groups.size() == 1 )
			//group_to_graduate = groups[0];
			scores.push_back( 1.0 );
		else
		{
			randutils::mt19937_rng rng;
			scores = heuristic_graduation( cpp_grid, groups );

			/*
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
		  */
		}
	}
	else
		scores.push_back( -1.0 );

	jdoubleArray returned_scores = env->NewDoubleArray( scores.size() );
	env->SetDoubleArrayRegion( returned_scores, 0, scores.size(), (jdouble *) &scores[0] );


	ALOG("%d", __LINE__);
	//return thiz;
	return returned_scores;
}