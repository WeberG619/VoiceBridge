cmake_minimum_required(VERSION 3.22.1)
project("voicebridge")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_library(log-lib log)
find_library(android-lib android)

# Whisper.cpp configuration
set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../extern/whisper.cpp)
if(EXISTS ${WHISPER_DIR})
    add_subdirectory(${WHISPER_DIR} whisper.cpp)
    set(WHISPER_AVAILABLE TRUE)
else()
    message(WARNING "Whisper.cpp not found at ${WHISPER_DIR}")
    set(WHISPER_AVAILABLE FALSE)
endif()

# LLaMA.cpp configuration
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../extern/llama.cpp)
if(EXISTS ${LLAMA_DIR})
    add_subdirectory(${LLAMA_DIR} llama.cpp)
    set(LLAMA_AVAILABLE TRUE)
else()
    message(WARNING "LLaMA.cpp not found at ${LLAMA_DIR}")
    set(LLAMA_AVAILABLE FALSE)
endif()

# VoiceBridge native library
add_library(voicebridge SHARED
    voicebridge.cpp
    audio_processor.cpp
    text_processor.cpp
)

# Link libraries
target_link_libraries(voicebridge
    ${log-lib}
    ${android-lib}
)

# Link Whisper if available
if(WHISPER_AVAILABLE)
    target_link_libraries(voicebridge whisper)
    target_compile_definitions(voicebridge PRIVATE WHISPER_AVAILABLE)
endif()

# Link LLaMA if available
if(LLAMA_AVAILABLE)
    target_link_libraries(voicebridge llama)
    target_compile_definitions(voicebridge PRIVATE LLAMA_AVAILABLE)
endif()

# Include directories
target_include_directories(voicebridge PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

if(WHISPER_AVAILABLE)
    target_include_directories(voicebridge PRIVATE ${WHISPER_DIR})
endif()

if(LLAMA_AVAILABLE)
    target_include_directories(voicebridge PRIVATE ${LLAMA_DIR})
endif()