# https://d.android.com/studio/projects/add-native-code.html
cmake_minimum_required(VERSION 3.18.1)

project("pobo")

set(DIR ${CMAKE_CURRENT_SOURCE_DIR})
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17")

if("${CMAKE_ANDROID_ARCH_ABI}" STREQUAL "arm64-v8a")
    set(ghost_android "ghost_android_arm64")
endif()

if("${CMAKE_ANDROID_ARCH_ABI}" STREQUAL "armeabi-v7a")
    set(ghost_android "ghost_android_armelf")
endif()

if("${CMAKE_ANDROID_ARCH_ABI}" STREQUAL "x86")
    set(ghost_android "ghost_android_x86")
endif()

if("${CMAKE_ANDROID_ARCH_ABI}" STREQUAL "x86_64")
    set(ghost_android "ghost_android_x86_64")
endif()

add_library(
        pobo

        SHARED

        ${DIR}/pobo.cpp
        ${DIR}/model/has_piece.cpp
        ${DIR}/model/free_position.cpp
        ${DIR}/model/removed_positions.cpp
        ${DIR}/model/pobo_objective.cpp
        ${DIR}/model/builder.cpp
        ${DIR}/helpers.cpp
        ${DIR}/heuristics.cpp
        ${DIR}/simulator.cpp
)

include_directories(${DIR}/lib/include/ ${DIR})
add_library( ghost_android SHARED IMPORTED )
set_target_properties( ghost_android PROPERTIES IMPORTED_LOCATION "${DIR}/lib/lib${ghost_android}.so" )

target_link_libraries(
        pobo

        ghost_android

        log )
