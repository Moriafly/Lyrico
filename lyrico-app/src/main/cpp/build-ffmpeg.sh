#!/usr/bin/env bash
set -euo pipefail
mode=$1
source_dir=$2
build_dir=$3
cc=$4
target=$5
ar=$6
ranlib=$7
make_tool=$8
abi=$9
jobs=${10}
# Git for Windows supplies the POSIX utilities; compilation uses the Windows NDK.
# Convert filesystem paths for shell tools, but keep native paths for Clang/make.
if command -v cygpath >/dev/null 2>&1; then
    source_dir=$(cygpath -m "$source_dir")
    build_dir=$(cygpath -m "$build_dir")
    cc=$(cygpath -m "$cc")
    ar=$(cygpath -m "$ar")
    ranlib=$(cygpath -m "$ranlib")
    make_tool=$(cygpath -m "$make_tool")
fi
mkdir -p "$build_dir"
cd "$build_dir"
compiler_dir=$(dirname "$cc")
archive_dir=$(dirname "$ar")
if command -v cygpath >/dev/null 2>&1; then
    compiler_dir=$(cygpath -u "$compiler_dir")
    archive_dir=$(cygpath -u "$archive_dir")
fi
export PATH="$compiler_dir:$archive_dir:$PATH"
cc=$(basename "$cc")
ar=$(basename "$ar")
ranlib=$(basename "$ranlib")
mkdir -p tmp
export TMPDIR="$build_dir/tmp"
case "$abi" in
    arm64-v8a) arch=aarch64 ;;
    armeabi-v7a) arch=arm ;;
    *) echo "Unsupported FFmpeg ABI: $abi" >&2; exit 1 ;;
esac
if [[ $mode == configure ]]; then
    if [[ "$source_dir" == *" "* || "$build_dir" == *" "* ]]; then
        echo "FFmpeg requires a project/build path without spaces" >&2
        exit 1
    fi
    # ALAC/APE library targets do not use host table generators.
    # Reuse NDK Clang for configure's host C11 probe, without requiring GCC/MSVC.
    "$source_dir/configure" \
        --target-os=android --arch="$arch" --enable-cross-compile \
        --cc="$cc" --ld="$cc" --ar="$ar" --ranlib="$ranlib" \
        --nm=llvm-nm --strip=llvm-strip --disable-debug \
        --host-cc="$cc --target=$target" --enable-hardcoded-tables \
        --extra-cflags="--target=$target -O2 -fPIC -ffunction-sections -fdata-sections" \
        --extra-ldflags="--target=$target" \
        --enable-static --disable-shared --disable-programs --disable-doc \
        --disable-everything --disable-autodetect --disable-network \
        --disable-avdevice --disable-avfilter --disable-swscale \
        --disable-symver --enable-small --enable-pic \
        --enable-avformat --enable-avcodec --enable-avutil --enable-swresample \
        --enable-demuxer=mov,ape --enable-decoder=alac,ape
    if command -v cygpath >/dev/null 2>&1; then
        # configure records POSIX absolute paths; native Windows make needs drive paths.
        posix_source=$(cygpath -u "$source_dir")
        sed -i "s|$posix_source|$source_dir|g" Makefile ffbuild/config.mak ffbuild/config.sh
    fi
else
    # Windows GNU Make must execute recipes using Git Bash, rather than cmd.exe.
    if command -v cygpath >/dev/null 2>&1; then
        shell_path=$(cygpath -m /usr/bin/sh.exe)
        "$make_tool" -j"$jobs" "SHELL=$shell_path" libavformat/libavformat.a libavcodec/libavcodec.a libswresample/libswresample.a libavutil/libavutil.a
    else
        "$make_tool" -j"$jobs" libavformat/libavformat.a libavcodec/libavcodec.a libswresample/libswresample.a libavutil/libavutil.a
    fi
fi
