cmake_minimum_required(VERSION 3.22.1)
project("cyberguard_secrets")

add_library(${CMAKE_PROJECT_NAME} SHARED secrets.cpp)
target_link_libraries(${CMAKE_PROJECT_NAME} android log)
