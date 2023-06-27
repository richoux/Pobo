#include <jni.h>

#include <vector>
#include "lib/include/ghost/solver.hpp"
#include "model/builder.hpp"

using namespace std::literals::chrono_literals;

// From https://www.baeldung.com/jni
// See also https://developer.android.com/training/articles/perf-jni

extern "C"
JNIEXPORT jintArray JNICALL
Java_fr_richoux_pobo_engine_ai_MCTS_00024Companion_ghost_1solver_1call(
        JNIEnv *env,
        jobject thiz,
        jbyteArray k_grid,
        jbyteArray k_blue_pool,
        jbyteArray k_red_pool,
        jint k_blue_pool_size,
        jint k_red_pool_size,
        jboolean k_blue_turn) {
    // Inputs //
    jbyte cpp_grid[36];
    jint pool_size = k_blue_turn ? k_blue_pool_size : k_red_pool_size;
    jbyte pool[pool_size];

    env->GetByteArrayRegion(k_grid, 0, 36, cpp_grid);
    if( k_blue_turn )
        env->GetByteArrayRegion(k_blue_pool, 0, pool_size, pool);
    else
        env->GetByteArrayRegion(k_red_pool, 0, pool_size, pool);
    //env->GetByteArrayRegion(k_red_pool, 0, k_red_pool_size, cpp_red_pool);

//    jclass kotlin_game_class = env->GetObjectClass(k_game);
//    jfieldID kotlin_board = env->GetFieldID(kotlin_game_class , "board", "Lfr/richoux/pobo/engine/Board;");
//    jfieldID kotlin_player = env->GetFieldID(kotlin_game_class , "currentPlayer", "Lfr/richoux/pobo/engine/PieceColor;");
//
//    jclass cpp_board_class = env->FindClass("fr/richoux/pobo/engine/Board");
//    jobject cpp_board = env->GetObjectField(k_game, kotlin_board);
//
//    jclass cpp_player_class = env->FindClass("fr/richoux/pobo/engine/PieceColor");
//    jobject cpp_player = env->GetObjectField(k_game, kotlin_player);
//
//    jmethodID methodId = env->GetMethodID(kotlin_game_class, "getUserInfo", "()Ljava/lang/String;");
//
//    //jstring result = (jstring)env->CallObjectMethod(userData, methodId);

    // Move search //
    jclass cpp_piece_class = env->FindClass("fr/richoux/pobo/engine/Piece");
    jobject cpp_piece = env->AllocObject(cpp_piece_class);

    jclass cpp_position_class = env->FindClass("fr/richoux/pobo/engine/Position");
    jobject cpp_position = env->AllocObject(cpp_position_class);

    Builder builder(cpp_grid, pool, pool_size, k_blue_turn);
    ghost::Options options;
//  options.parallel_runs = true;
    ghost::Solver solver(builder);

    double error;
    std::vector<int> solution;

    bool success = solver.solve(error, solution, 5ms, options);

    // Output: Move (Piece + Position)
//    jfieldID code = env->GetFieldID(cpp_piece_class, "code", "B");
//    jfieldID x_coord = env->GetFieldID(cpp_position_class, "x", "I");
//    jfieldID y_coord = env->GetFieldID(cpp_position_class, "y", "I");
//
//    auto piece_code = solution[0] * k_blue_turn ? -1 : 1;
//    env->SetByteField(cpp_piece, code, piece_code);
//
//    auto x = solution[1];
//    auto y = solution[2];
//
//    env->SetIntField(cpp_position, x_coord, x);
//    env->SetIntField(cpp_position, y_coord, y);
//
//    jclass cpp_move_class = env->FindClass("fr/richoux/pobo/engine/Move");
//    jobject cpp_move = env->AllocObject(cpp_move_class);
//
//    jfieldID piece = env->GetFieldID(cpp_move_class, "piece", "Lfr/richoux/pobo/engine/Piece;");
//    jfieldID position = env->GetFieldID(cpp_move_class, "to", "Lfr/richoux/pobo/engine/Position;");
//
//    env->SetObjectField(cpp_move, piece, cpp_piece);
//    env->SetObjectField(cpp_move, position, cpp_position);

		if( !success )
			solution[0] = 42;

    jintArray sol = env->NewIntArray(3);
    env->SetIntArrayRegion(sol, 0, 3, (jint*)&solution[0]);

    return sol;
}