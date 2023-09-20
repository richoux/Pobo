#include <jni.h>

#include <vector>
#include "lib/include/ghost/solver.hpp"
#include "model/builder.hpp"

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
				jboolean k_blue_turn )
{
	// Inputs //
	jbyte cpp_grid[36];
	jint pool_size = k_blue_turn ? k_blue_pool_size : k_red_pool_size;
	jbyte pool[pool_size];

	env->GetByteArrayRegion( k_grid, 0, 36, cpp_grid );
	if( k_blue_turn )
		env->GetByteArrayRegion( k_blue_pool, 0, pool_size, pool );
	else
		env->GetByteArrayRegion( k_red_pool, 0, pool_size, pool );

	// Move search //
	Builder builder( cpp_grid, pool, pool_size, k_blue_turn );
	ghost::Solver solver( builder );

	double cost;
	std::vector<int> solution;

	bool success = solver.fast_search( cost, solution, 1 );

	// Output: Move (Piece + Position) + Cost
	if( !success )
		solution[0] = 42;

	solution.push_back(static_cast<int>(cost) );
	jintArray sol = env->NewIntArray( 4 );
	env->SetIntArrayRegion( sol, 0, 4, (jint *) &solution[0] );

	return sol;
}