cmake_minimum_required(VERSION 3.22.1)
project("voicebridge")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Configure build type
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

# Platform-specific optimizations
if(ANDROID)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -O3 -fno-finite-math-only -Wno-format")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra -O3 -fno-finite-math-only -Wno-format")
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O0 -g")
endif()

# Find required packages
find_library(log-lib log)
find_library(android-lib android)

# External dependencies paths
set(EXTERN_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../extern)
set(WHISPER_DIR ${EXTERN_DIR}/whisper.cpp)
set(LLAMA_DIR ${EXTERN_DIR}/llama.cpp)

# Whisper.cpp configuration
if(EXISTS ${WHISPER_DIR}/CMakeLists.txt)
    message(STATUS "Found Whisper.cpp at ${WHISPER_DIR}")
    
    # Configure Whisper for Android
    set(WHISPER_BUILD_TESTS OFF CACHE BOOL "")
    set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "")
    set(GGML_OPENMP OFF CACHE BOOL "")
    set(GGML_ACCELERATE OFF CACHE BOOL "")
    set(GGML_OPENBLAS OFF CACHE BOOL "")
    set(GGML_METAL OFF CACHE BOOL "")
    set(GGML_CUDA OFF CACHE BOOL "")
    set(GGML_HIPBLAS OFF CACHE BOOL "")
    set(GGML_CLBLAST OFF CACHE BOOL "")
    
    # Suppress format warnings for Whisper/GGML
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-format")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-format")
    
    add_subdirectory(${WHISPER_DIR} whisper_build)
    set(WHISPER_AVAILABLE TRUE)
    
    # Apply warning suppressions to whisper target
    if(TARGET whisper)
        target_compile_options(whisper PRIVATE -Wno-format -Wno-format-security)
    endif()
    if(TARGET ggml)
        target_compile_options(ggml PRIVATE -Wno-format -Wno-format-security)
    endif()
    
    # Get whisper include directory
    get_target_property(WHISPER_INCLUDE_DIR whisper INTERFACE_INCLUDE_DIRECTORIES)
    if(NOT WHISPER_INCLUDE_DIR)
        set(WHISPER_INCLUDE_DIR ${WHISPER_DIR}/include)
    endif()
else()
    message(WARNING "Whisper.cpp not found at ${WHISPER_DIR}")
    message(STATUS "Run scripts/setup_submodules.sh to initialize submodules")
    set(WHISPER_AVAILABLE FALSE)
endif()

# LLaMA.cpp configuration
if(EXISTS ${LLAMA_DIR}/CMakeLists.txt)
    message(STATUS "Found LLaMA.cpp at ${LLAMA_DIR}")
    
    # Configure LLaMA for Android
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "")
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "")
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "")
    set(GGML_OPENMP OFF CACHE BOOL "")
    set(GGML_ACCELERATE OFF CACHE BOOL "")
    set(GGML_OPENBLAS OFF CACHE BOOL "")
    set(GGML_METAL OFF CACHE BOOL "")
    set(GGML_CUDA OFF CACHE BOOL "")
    set(GGML_HIPBLAS OFF CACHE BOOL "")
    set(GGML_CLBLAST OFF CACHE BOOL "")
    
    add_subdirectory(${LLAMA_DIR} llama_build)
    set(LLAMA_AVAILABLE TRUE)
    
    # Get llama include directory
    get_target_property(LLAMA_INCLUDE_DIR llama INTERFACE_INCLUDE_DIRECTORIES)
    if(NOT LLAMA_INCLUDE_DIR)
        set(LLAMA_INCLUDE_DIR ${LLAMA_DIR}/include)
    endif()
else()
    message(WARNING "LLaMA.cpp not found at ${LLAMA_DIR}")
    message(STATUS "Run scripts/setup_submodules.sh to initialize submodules")
    set(LLAMA_AVAILABLE FALSE)
endif()

# VoiceBridge native library
add_library(voicebridge SHARED
    voicebridge.cpp
    audio_processor.cpp
    text_processor.cpp
)

# Set library properties
set_target_properties(voicebridge PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
    CXX_EXTENSIONS OFF
    POSITION_INDEPENDENT_CODE ON
)

# Include directories
target_include_directories(voicebridge PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Conditional includes based on availability
if(WHISPER_AVAILABLE)
    target_include_directories(voicebridge PRIVATE 
        ${WHISPER_INCLUDE_DIR}
        ${WHISPER_DIR}
        ${WHISPER_DIR}/include
        ${WHISPER_DIR}/ggml/include
    )
    target_compile_definitions(voicebridge PRIVATE WHISPER_AVAILABLE)
endif()

if(LLAMA_AVAILABLE)
    target_include_directories(voicebridge PRIVATE 
        ${LLAMA_INCLUDE_DIR}
        ${LLAMA_DIR}
        ${LLAMA_DIR}/include
        ${LLAMA_DIR}/ggml/include
    )
    target_compile_definitions(voicebridge PRIVATE LLAMA_AVAILABLE)
endif()

# Link libraries - base Android libraries
target_link_libraries(voicebridge
    ${log-lib}
    ${android-lib}
)

# Link AI libraries if available
if(WHISPER_AVAILABLE)
    target_link_libraries(voicebridge whisper)
    # Also link ggml if it's a separate target
    if(TARGET ggml)
        target_link_libraries(voicebridge ggml)
    endif()
endif()

if(LLAMA_AVAILABLE)
    target_link_libraries(voicebridge llama)
    # Also link ggml if it's a separate target
    if(TARGET ggml AND NOT WHISPER_AVAILABLE)
        target_link_libraries(voicebridge ggml)
    endif()
endif()

# Debug information
message(STATUS "VoiceBridge CMake Configuration:")
message(STATUS "  CMAKE_BUILD_TYPE: ${CMAKE_BUILD_TYPE}")
message(STATUS "  CMAKE_CXX_STANDARD: ${CMAKE_CXX_STANDARD}")
message(STATUS "  WHISPER_AVAILABLE: ${WHISPER_AVAILABLE}")
message(STATUS "  LLAMA_AVAILABLE: ${LLAMA_AVAILABLE}")
message(STATUS "  EXTERN_DIR: ${EXTERN_DIR}")

if(WHISPER_AVAILABLE)
    message(STATUS "  WHISPER_DIR: ${WHISPER_DIR}")
    message(STATUS "  WHISPER_INCLUDE_DIR: ${WHISPER_INCLUDE_DIR}")
endif()

if(LLAMA_AVAILABLE)
    message(STATUS "  LLAMA_DIR: ${LLAMA_DIR}")
    message(STATUS "  LLAMA_INCLUDE_DIR: ${LLAMA_INCLUDE_DIR}")
endif()