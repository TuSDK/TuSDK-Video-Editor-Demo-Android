/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base$
 *  @author  H.ys
 *  @Date    2020/10/28$ 10:47$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base

import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.VideoEditor
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.clips.ImageClip
import com.tusdk.pulse.editor.clips.SilenceClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.CanvasResizeEffect
import org.lasque.tusdkpulse.core.utils.TLog

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/28  10:47
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class VideoItem(val path: String, val id: Long, val videoClip: Clip, val audioClip: Clip) {

    companion object {
        private var Clip_Count = 0
        fun createVideoItem(path: String, editor: VideoEditor, needResize: Boolean = true, isVideoClip: Boolean = true): VideoItem {
            if (isVideoClip) {
                var hasAudio = true
                val videoConfig = Config()
                videoConfig.setString(VideoFileClip.CONFIG_PATH, path)
//                videoConfig.setBoolean(VideoFileClip.CONFIG_IS_SYNC, true);
                val videoClip = Clip(editor.context, VideoFileClip.TYPE_NAME)
                videoClip.setConfig(videoConfig)

                val audioConfig = Config()
                audioConfig.setString(AudioFileClip.CONFIG_PATH, path)
                var audioClip = Clip(editor.context, AudioFileClip.TYPE_NAME)
                audioClip.setConfig(audioConfig)
                if (needResize) {
                    val resizeConfig = Config()
                    val resizeEffect = Effect(editor.context, CanvasResizeEffect.TYPE_NAME)
                    resizeEffect.setConfig(resizeConfig)
                    videoClip.effects().add(50, resizeEffect)
                }

                if (!videoClip.activate()) {
                    TLog.e("Clip activate failed")
                    throw UnsupportedOperationException()
                } else if (!audioClip.activate()) {
                    audioClip = Clip(editor.context,SilenceClip.TYPE_NAME)
                    val audioConfig = Config()
                    audioConfig.setNumber(SilenceClip.CONFIG_DURATION,videoClip.streamInfo.duration)
                    audioClip.setConfig(audioConfig)
                }

                return VideoItem(path, (++Clip_Count).toLong(), videoClip, audioClip)
            } else {
                val videoConfig = Config()
                videoConfig.setString(ImageClip.CONFIG_PATH, path)
                videoConfig.setNumber(ImageClip.CONFIG_DURATION,3000)
                val videoClip = Clip(editor.context, ImageClip.TYPE_NAME)
                videoClip.setConfig(videoConfig)
                if (needResize) {
                    val resizeConfig = Config()
                    val resizeEffect = Effect(editor.context, CanvasResizeEffect.TYPE_NAME)
                    resizeEffect.setConfig(resizeConfig)
                    videoClip.effects().add(50, resizeEffect)
                }
                val audioClip = Clip(editor.context,SilenceClip.TYPE_NAME)
                val audioConfig = Config()
                audioConfig.setNumber(SilenceClip.CONFIG_DURATION,3000)
                audioClip.setConfig(audioConfig)
                if (!videoClip.activate()) {
                    TLog.e("Clip activate failed")
                    throw UnsupportedOperationException()
                } else {
                    return VideoItem(path, (++Clip_Count).toLong(), videoClip, audioClip)
                }
            }

        }

        fun copy(editor: VideoEditor,item : VideoItem) : VideoItem{
            val videoClip = Clip(editor.context,item.mVideoClip.model)



            val audioClip = Clip(editor.context,item.mAudioClip.model)

            if (!videoClip.activate()) {
                TLog.e("Clip activate failed")
                throw UnsupportedOperationException()
            }

            if (!audioClip.activate()){
                TLog.e("Clip activate failed")
            }

            return VideoItem(item.path,(++Clip_Count).toLong(),videoClip, audioClip)
        }

        fun resetID() {
            Clip_Count = 0
        }
    }

    val mPath = path
    var mId = id
    val mVideoClip = videoClip
    val mAudioClip = audioClip


}