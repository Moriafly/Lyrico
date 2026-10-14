include(ExternalProject)
find_package(Git REQUIRED)
if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/configure")
    message(FATAL_ERROR "Initialize FFmpeg: git submodule update --init --recursive")
endif()
if(CMAKE_HOST_WIN32)
    get_filename_component(git_dir "${GIT_EXECUTABLE}" DIRECTORY)
    find_program(FFMPEG_BASH bash PATHS "${git_dir}/../bin" "${git_dir}/../usr/bin" NO_DEFAULT_PATH REQUIRED)
else()
    find_program(FFMPEG_BASH bash REQUIRED)
endif()
find_program(FFMPEG_MAKE NAMES gmake make REQUIRED)
execute_process(COMMAND "${GIT_EXECUTABLE}" rev-parse HEAD
    WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg"
    OUTPUT_VARIABLE ffmpeg_revision OUTPUT_STRIP_TRAILING_WHITESPACE COMMAND_ERROR_IS_FATAL ANY)
execute_process(COMMAND "${GIT_EXECUTABLE}" rev-parse --absolute-git-dir
    WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg"
    OUTPUT_VARIABLE ffmpeg_git_dir OUTPUT_STRIP_TRAILING_WHITESPACE COMMAND_ERROR_IS_FATAL ANY)
set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS
    "${ffmpeg_git_dir}/HEAD" "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/configure")
file(CONFIGURE OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/ffmpeg-revision" CONTENT "${ffmpeg_revision}\n")
set(ffmpeg_build "${CMAKE_CURRENT_BINARY_DIR}/ffmpeg-build")
set(ffmpeg_libraries)
foreach(lib avformat avcodec swresample avutil)
    list(APPEND ffmpeg_libraries "${ffmpeg_build}/lib${lib}/lib${lib}.a")
endforeach()
include(ProcessorCount)
ProcessorCount(ffmpeg_jobs)
if(NOT ffmpeg_jobs OR ffmpeg_jobs GREATER 8)
    set(ffmpeg_jobs 8)
endif()
set(ffmpeg_command "${CMAKE_COMMAND}" -E env
    "${FFMPEG_BASH}" --noprofile --norc "${CMAKE_CURRENT_SOURCE_DIR}/build-ffmpeg.sh")
set(ffmpeg_args "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg" "${ffmpeg_build}"
    "${CMAKE_C_COMPILER}" "${CMAKE_C_COMPILER_TARGET}" "${CMAKE_AR}" "${CMAKE_RANLIB}"
    "${FFMPEG_MAKE}" "${ANDROID_ABI}" "${ffmpeg_jobs}")
ExternalProject_Add(ffmpeg-native
    SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg"
    BINARY_DIR "${ffmpeg_build}"
    DOWNLOAD_COMMAND "" UPDATE_COMMAND ""
    CONFIGURE_COMMAND ${ffmpeg_command} configure ${ffmpeg_args}
    BUILD_COMMAND ${ffmpeg_command} build ${ffmpeg_args}
    INSTALL_COMMAND ""
    BUILD_BYPRODUCTS ${ffmpeg_libraries}
    BUILD_ALWAYS TRUE)
ExternalProject_Add_StepDependencies(ffmpeg-native configure
    "${CMAKE_CURRENT_SOURCE_DIR}/build-ffmpeg.sh" "${CMAKE_CURRENT_BINARY_DIR}/ffmpeg-revision")
add_library(ffmpeg-decoder SHARED ffmpeg_decoder.cpp)
add_dependencies(ffmpeg-decoder ffmpeg-native)
target_include_directories(ffmpeg-decoder PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg" "${ffmpeg_build}")
target_compile_definitions(ffmpeg-decoder PRIVATE _FILE_OFFSET_BITS=64)
target_link_libraries(ffmpeg-decoder PRIVATE ${ffmpeg_libraries} m z)
set_target_properties(ffmpeg-decoder PROPERTIES CXX_STANDARD 17 CXX_VISIBILITY_PRESET hidden)
target_link_options(ffmpeg-decoder PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
