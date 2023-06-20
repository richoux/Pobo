// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("pobo");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("pobo")
//      }
//    }

#include <array>

#include <jni.h>

struct game
{
    std::array< std::array<int, 6>, 6 > grid;
};

// From https://www.baeldung.com/jni
// See also https://developer.android.com/training/articles/perf-jni
extern "C"
JNIEXPORT jobject JNICALL
ghost_solver_call( JNIEnv *env, jobject k_this, jobject k_game )
{
    // Input object class: Game
    // Output object class: Move

    // Input //
    jclass kotlin_game_class = env->GetObjectClass(k_game);
    jfieldID kotlin_board = env->GetFieldID(kotlin_game_class , "board", "Lfr/richoux/pobo/engine/Board;");
    jfieldID kotlin_player = env->GetFieldID(kotlin_game_class , "currentPlayer", "Lfr/richoux/pobo/engine/PieceColor;");

    jclass cpp_board_class = env->FindClass("fr/richoux/pobo/engine/Board");
    jobject cpp_board = env->GetObjectField(k_game, kotlin_board);

    jclass cpp_player_class = env->FindClass("fr/richoux/pobo/engine/PieceColor");
    jobject cpp_player = env->GetObjectField(k_game, kotlin_player);

    jmethodID methodId = env->GetMethodID(kotlin_game_class, "getUserInfo", "()Ljava/lang/String;");

    //jstring result = (jstring)env->CallObjectMethod(userData, methodId);

    // Move search //


    // Output //
    jclass cpp_move_class = env->FindClass("fr/richoux/pobo/engine/Move");
    jobject cpp_move = env->AllocObject(cpp_move_class);

    jclass cpp_piece_class = env->FindClass("fr/richoux/pobo/engine/Piece");
    jobject cpp_piece = env->AllocObject(cpp_piece_class);

    jclass cpp_position_class = env->FindClass("fr/richoux/pobo/engine/Position");
    jobject cpp_position = env->AllocObject(cpp_position_class);

    jfieldID piece = env->GetFieldID(cpp_move_class , "piece", "Lfr/richoux/pobo/engine/Piece;");
    jfieldID position = env->GetFieldID(cpp_move_class , "to", "Lfr/richoux/pobo/engine/Position;");

    env->SetObjectField(cpp_move, piece, cpp_piece);
    env->SetObjectField(cpp_move, position, cpp_position);

    return cpp_move;
}