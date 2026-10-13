set(SPIRV-Headers_FOUND TRUE)

if(NOT TARGET SPIRV-Headers::SPIRV-Headers)
    add_library(SPIRV-Headers::SPIRV-Headers INTERFACE IMPORTED)
    if(CMAKE_ANDROID_NDK)
        set(_spirv_ndk "${CMAKE_ANDROID_NDK}")
    elseif(ANDROID_NDK)
        set(_spirv_ndk "${ANDROID_NDK}")
    endif()
    if(_spirv_ndk)
        set(_spirv_headers_dir
            "${_spirv_ndk}/sources/third_party/shaderc/third_party/spirv-tools/external/spirv-headers/include")
        if(EXISTS "${_spirv_headers_dir}")
            set_target_properties(SPIRV-Headers::SPIRV-Headers PROPERTIES
                INTERFACE_INCLUDE_DIRECTORIES "${_spirv_headers_dir}")
        endif()
    endif()
endif()
