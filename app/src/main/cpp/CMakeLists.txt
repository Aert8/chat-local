cmake_minimum_required(VERSION 3.22.1)

project("lfm_android")

list(APPEND CMAKE_PREFIX_PATH "${CMAKE_CURRENT_LIST_DIR}/cmake")
set(SPIRV-Headers_DIR "${CMAKE_CURRENT_LIST_DIR}/cmake/SPIRV-Headers")

# Forzar C++17 para llama.cpp y soporte de corrutinas/vectores
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Desactivar componentes innecesarios en Android para acelerar la compilación
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_CPU_KLEIDIAI ON CACHE BOOL "" FORCE)
set(GGML_VULKAN ON CACHE BOOL "" FORCE)
set(GGML_VULKAN_DEBUG OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN_CHECK_RESULTS OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN_VALIDATE OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN_COOPMAT_GLSLC_SUPPORT OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN_COOPMAT2_GLSLC_SUPPORT OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN_COOPMAT2_DECODE_VECTOR_GLSLC_SUPPORT OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN_INTEGER_DOT_GLSLC_SUPPORT OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN_BFLOAT16_GLSLC_SUPPORT OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN_FLOAT_E2M1_GLSLC_SUPPORT OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN_FLOAT_E4M3_GLSLC_SUPPORT OFF CACHE BOOL "" FORCE)

if (ANDROID)
    set(_ndk_root "${CMAKE_ANDROID_NDK}")
    if (NOT _ndk_root AND DEFINED ANDROID_NDK)
        set(_ndk_root "${ANDROID_NDK}")
    endif()

    if (EXISTS "/usr/include/vulkan/vulkan.hpp")
        set(VULKAN_HPP_INCLUDE_DIR "/usr/include")
    elseif (EXISTS "/usr/local/include/vulkan/vulkan.hpp")
        set(VULKAN_HPP_INCLUDE_DIR "/usr/local/include")
    endif()
    if (VULKAN_HPP_INCLUDE_DIR)
        include_directories(SYSTEM "${VULKAN_HPP_INCLUDE_DIR}")
    endif()
    if (CMAKE_HOST_SYSTEM_NAME STREQUAL "Linux")
        set(_ndk_host_tag "linux-x86_64")
    elseif (CMAKE_HOST_SYSTEM_NAME STREQUAL "Darwin")
        set(_ndk_host_tag "darwin-x86_64")
    elseif (CMAKE_HOST_SYSTEM_NAME STREQUAL "Windows")
        set(_ndk_host_tag "windows-x86_64")
    endif()
    set(_ndk_glslc "${_ndk_root}/shader-tools/${_ndk_host_tag}/glslc")
    if (EXISTS "${_ndk_glslc}")
        set(Vulkan_GLSLC_EXECUTABLE "${_ndk_glslc}" CACHE FILEPATH "Android NDK glslc" FORCE)
    endif()
endif()

# 1. Agregar el código fuente de llama.cpp al árbol de compilación
add_subdirectory(llama.cpp)
target_link_libraries(ggml-vulkan PRIVATE log)

# 2. Definir tu biblioteca nativa puente (JNI)
add_library(
        native-lib
        SHARED
        native-lib.cpp
)

# 3. Incluir las cabeceras (.h) de llama.cpp y ggml
target_include_directories(
        native-lib
        PRIVATE
        llama.cpp/include
        llama.cpp/ggml/include
)

# 4. Enlazar tu biblioteca con llama.cpp y el log de Android
target_link_libraries(
        native-lib
        llama
        log
        android
)