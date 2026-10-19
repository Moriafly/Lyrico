package com.lonx.lyrico.domain.song.usecase

import com.lonx.audiotag.model.AudioTagData
import com.lonx.lyrico.data.song.tag.AudioTagMutationFactory
import com.lonx.lyrico.data.song.tag.AudioTagMutationMode

class OverwriteSongTagsUseCase(
    private val saveAudioTagsUseCase: SaveAudioTagsUseCase
) {
    /**
     * @param picturesAuthored 用户是否改动过成功读取到的图片列表；为 true 时图片被删空也会写入。
     */
    suspend operator fun invoke(
        uri: String,
        tagData: AudioTagData,
        picturesAuthored: Boolean = false
    ): SaveAudioTagsResult {
        val mutation = AudioTagMutationFactory.fromAudioTagData(
            data = tagData,
            mode = AudioTagMutationMode.Overwrite,
            picturesAuthored = picturesAuthored
        )
        return saveAudioTagsUseCase(uri, mutation)
    }
}
