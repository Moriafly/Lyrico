package com.lonx.lyrico.data.song.tag

import android.net.Uri
import com.lonx.audiotag.model.AudioPicture
import com.lonx.audiotag.model.AudioPictureType
import com.lonx.audiotag.model.AudioTagData
import com.lonx.audiotag.model.CustomTagField

object AudioTagMutationFactory {
    /**
     * @param picturesAuthored 调用方已成功读取完整图片列表并且用户改过它。用于区分「用户删光图片」
     *   （写空列表）与「这次没读到图片」（不写），否则会抹掉文件里原有的图片。
     */
    fun fromAudioTagData(
        data: AudioTagData,
        mode: AudioTagMutationMode,
        picturesAuthored: Boolean = false
    ): AudioTagMutation {
        val patchMode = mode == AudioTagMutationMode.Patch
        val fields = buildMap {
            putString(AudioTagFieldKey.Title, data.title, patchMode)
            putString(AudioTagFieldKey.Artist, data.artist, patchMode)
            putString(AudioTagFieldKey.Album, data.album, patchMode)
            putString(AudioTagFieldKey.AlbumArtist, data.albumArtist, patchMode)
            putString(AudioTagFieldKey.Genre, data.genre, patchMode)
            putString(AudioTagFieldKey.Date, data.date, patchMode)
            putString(AudioTagFieldKey.Language, data.language, patchMode)
            putString(AudioTagFieldKey.TrackNumber, data.trackNumber, patchMode)
            putInt(AudioTagFieldKey.DiscNumber, data.discNumber, patchMode)
            putString(AudioTagFieldKey.Composer, data.composer, patchMode)
            putString(AudioTagFieldKey.Lyricist, data.lyricist, patchMode)
            putString(AudioTagFieldKey.Comment, data.comment, patchMode)
            putString(AudioTagFieldKey.Lyrics, data.lyrics, patchMode)
            putString(AudioTagFieldKey.Copyright, data.copyright, patchMode)
            putInt(AudioTagFieldKey.Rating, data.rating, patchMode)
            putString(AudioTagFieldKey.ReplayGainTrackGain, data.replayGainTrackGain, patchMode)
            putString(AudioTagFieldKey.ReplayGainTrackPeak, data.replayGainTrackPeak, patchMode)
            putString(AudioTagFieldKey.ReplayGainAlbumGain, data.replayGainAlbumGain, patchMode)
            putString(AudioTagFieldKey.ReplayGainAlbumPeak, data.replayGainAlbumPeak, patchMode)
            putString(
                AudioTagFieldKey.ReplayGainReferenceLoudness,
                data.replayGainReferenceLoudness,
                patchMode
            )
        }

        val customFields = when {
            data.customFields.isNotEmpty() -> listOf(CustomTagFieldMutation.ReplaceAll(data.customFields))
            mode == AudioTagMutationMode.Overwrite -> listOf(CustomTagFieldMutation.ReplaceAll(emptyList()))
            else -> emptyList()
        }

        return AudioTagMutation(
            mode = mode,
            fields = fields,
            customFields = customFields,
            pictureUpdate = data.toPictureUpdate(mode, picturesAuthored)
        )
    }

    private fun MutableMap<AudioTagFieldKey, FieldMutation>.putString(
        key: AudioTagFieldKey,
        value: String?,
        patchMode: Boolean
    ) {
        when {
            value != null -> put(key, FieldMutation.Set(value))
            !patchMode -> put(key, FieldMutation.Clear)
        }
    }

    private fun MutableMap<AudioTagFieldKey, FieldMutation>.putInt(
        key: AudioTagFieldKey,
        value: Int?,
        patchMode: Boolean
    ) {
        when {
            value != null -> put(key, FieldMutation.Set(value.toString()))
            !patchMode -> put(key, FieldMutation.Clear)
        }
    }

    private fun AudioTagData.toPictureUpdate(
        mode: AudioTagMutationMode,
        picturesAuthored: Boolean
    ): PictureUpdate {
        val picUrl = picUrl
        if (picUrl != null) {
            val normalizedPicUrl = picUrl.trim()
            val basePictures = pictures.asOverwriteBase(mode, picturesAuthored)
            return if (normalizedPicUrl.isEmpty()) {
                PictureUpdate.RemovePicture(
                    type = AudioPictureType.FrontCover,
                    basePictures = basePictures
                )
            } else if (normalizedPicUrl.startsWith("http")) {
                PictureUpdate.ReplacePicture(
                    type = AudioPictureType.FrontCover,
                    source = PictureSource.UrlSource(normalizedPicUrl),
                    basePictures = basePictures
                )
            } else {
                PictureUpdate.ReplacePicture(
                    type = AudioPictureType.FrontCover,
                    source = PictureSource.UriSource(Uri.parse(normalizedPicUrl)),
                    basePictures = basePictures
                )
            }
        }

        // 用户的改动以这份列表为准：即使被删空也要写下去（picturesAuthored 为 true）
        return if (mode == AudioTagMutationMode.Overwrite && (picturesAuthored || pictures.isNotEmpty())) {
            PictureUpdate.ReplaceAll(pictures)
        } else {
            PictureUpdate.Unchanged
        }
    }

    /**
     * 换封面时承载其余图片的基准列表。改过图时**空列表也是有效基准**，否则删掉的海报会被
     * 磁盘上的旧图找回来；没改过图时返回 `null`，让下游沿用磁盘内容。
     */
    private fun List<AudioPicture>.asOverwriteBase(
        mode: AudioTagMutationMode,
        picturesAuthored: Boolean
    ): List<AudioPicture>? {
        return takeIf {
            mode == AudioTagMutationMode.Overwrite && (picturesAuthored || it.isNotEmpty())
        }
    }
}
