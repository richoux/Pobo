# https://d.android.com/studio/projects/add-native-code.html
cmake_minimum_required(VERSION 3.18.1)

project("pobo")

set(DIR ${CMAKE_CURRENT_SOURCE_DIR})
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17")

add_library(
        pobo

        SHARED

        ${DIR}/pobo.cpp
        ${DIR}/model/has_piece.cpp
        ${DIR}/model/free_position.cpp
        ${DIR}/model/pobo_heuristic.cpp
        ${DIR}/model/builder.cpp
)

include_directories(${DIR}/lib/include/)
add_library( ghost SHARED IMPORTED )
set_target_properties( ghost PROPERTIES IMPORTED_LOCATION "${DIR}/lib/libghost.so" )

target_link_libraries(
        pobo

        ghost

        log )
