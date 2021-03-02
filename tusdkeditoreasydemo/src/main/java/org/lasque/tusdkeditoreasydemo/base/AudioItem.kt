/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base$
 *  @author  H.ys
 *  @Date    2020/11/26$ 14:22$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base

import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.VideoEditor
import com.tusdk.pulse.editor.clips.AudioFileClip

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/26  14:22
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AudioLayerItem(val path : String,val layerId : Long,val audioLayer : Layer,val layerMixPropertyBuilder: Layer.AudioMixPropertyBuilder) {

    companion object{
        public const val Clip_ID = 30

        private var Layer_ID : Long = 50

        fun createAudioLayerItem(path : String,editor : VideoEditor) : AudioLayerItem{
            val audioConfig = Config()
            audioConfig.setString(AudioFileClip.CONFIG_PATH,path)
            val audioClip = Clip(editor.context,AudioFileClip.TYPE_NAME)
            audioClip.setConfig(audioConfig)

            val audioLayer = ClipLayer(editor.context,false)
            val audioLayerConfig = Config()
            audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
            audioLayer.setConfig(audioLayerConfig)

            val mixPropertyBuilder = Layer.AudioMixPropertyBuilder()
            mixPropertyBuilder.holder.weight = 0.0

            val audioID = ++Layer_ID
            audioLayer.addClip(Clip_ID,audioClip)

            return AudioLayerItem(path, audioID,audioLayer,mixPropertyBuilder)
        }


    }


}