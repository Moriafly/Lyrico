# Third-Party Notices

This file lists third-party source components that are vendored in this repository or built into the app outside the ordinary Gradle dependency metadata.

Gradle and Android dependencies are shown in the app's open-source licenses screen, generated with AboutLibraries.

| Component | Usage | License | Notice |
| --- | --- | --- | --- |
| [quickjs-ng](https://github.com/quickjs-ng/quickjs) | Embedded JavaScript runtime for source plugins | MIT | See [LICENSE](./lyrico-app/src/main/cpp/quickjs-ng/LICENSE). |
| [TagLib](https://github.com/taglib/taglib) | Native audio metadata reader/writer used by `lyrico-audiotag` | LGPL-2.1 or MPL-1.1 | See [COPYING.LGPL](./lyrico-audiotag/src/main/cpp/taglib/COPYING.LGPL) and [COPYING.MPL](./lyrico-audiotag/src/main/cpp/taglib/COPYING.MPL). |
| [libebur128](https://github.com/jiixyj/libebur128) | EBU R128 loudness measurement | MIT | See [COPYING](./lyrico-app/src/main/cpp/ebur128/COPYING). |
| [FFmpeg](https://github.com/FFmpeg/FFmpeg) | ALAC and APE fallback decoding for ReplayGain analysis | LGPL-2.1-or-later | See [COPYING.LGPLv2.1](./lyrico-app/src/main/cpp/ffmpeg/COPYING.LGPLv2.1). GPL and nonfree components are disabled. |

FFmpeg is pinned by the Git submodule and built from source using
`lyrico-app/src/main/cpp/build-ffmpeg.sh`. Source, build configuration and JNI wrapper
are available in this repository. The LGPL also applies when distributing the linked
native library; retain its license and provide the source and relinking materials required
by the license with binary releases.
