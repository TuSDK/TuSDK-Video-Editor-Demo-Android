/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/26$ 14:16$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.tusdk.pulse.Config
import com.tusdk.pulse.Property
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.effects.AudioTrimEffect
import kotlinx.android.synthetic.main.audio_mix_fragment.*
import kotlinx.android.synthetic.main.audio_mix_item_layout.view.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.AudioLayerItem
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import java.util.*
import kotlin.collections.ArrayList

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/26  14:16
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AudioMixFragment : BaseFragment(FunctionType.AudioMix) {

    private var mVideoItem: VideoItem? = null

    private var mAudioLayerItems: MutableList<AudioLayerItem>? = null

    private var mMainAudioLayer: Layer? = null

    private var mMainAudioMixProperty = Layer.AudioMixPropertyBuilder()


    override fun getLayoutId(): Int {
        return R.layout.audio_mix_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            if (!restoreLayer()) {
                initLayer()
            }

            runOnUiThread {
                view.lsq_mix_weight_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        if (!p2) return
                        mThreadPool?.execute {
                            val mixied = p1 / 100.0
                            mMainAudioMixProperty.holder.weight = mixied
                            mMainAudioLayer?.setProperty(Layer.PROP_MIX, mMainAudioMixProperty.makeProperty())
                        }
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                    }

                })

                view.lsq_mix_weight_bar.progress = (mMainAudioMixProperty.holder.weight * 100).toInt()

                val audioMixAdapter = AudioMixAdapter(mAudioLayerItems!!, requireContext(), mThreadPool!!)
                val layoutManager = LinearLayoutManager(requireContext())
                layoutManager.orientation = LinearLayoutManager.VERTICAL
                lsq_audio_list.layoutManager = layoutManager
                lsq_audio_list.adapter = audioMixAdapter
            }
        }
    }

    private fun restoreLayer(): Boolean {
        if (mEditor!!.videoComposition().allLayers.isEmpty()) {
            return false
        }
        val mainAudioLayer = mEditor!!.audioComposition().allLayers[1] as ClipLayer
        var mainProperty: Property? = mainAudioLayer.getProperty(Layer.PROP_MIX)
        val mainAudioHolder = if (mainProperty == null)Layer.AudioMixPropertyHolder() else Layer.AudioMixPropertyHolder(mainProperty)
        mMainAudioMixProperty.holder = mainAudioHolder
        mMainAudioLayer = mainAudioLayer

        val audioLayerMaps = mEditor!!.audioComposition().allLayers
        val audioLayerList = ArrayList<AudioLayerItem>()
        for (key in audioLayerMaps.keys) {
            if (key == 1) continue

            val subAudioLayer = audioLayerMaps[key] as ClipLayer
            val subAudioClip = subAudioLayer.getClip(AudioLayerItem.Clip_ID)
            val subAudioPath = subAudioClip.config.getString(AudioFileClip.CONFIG_PATH)
            val layerMixPropertyBuilder = Layer.AudioMixPropertyBuilder()
            val layerMixProperty: Property? = subAudioLayer.getProperty(Layer.PROP_MIX)
            val layerMixHolder = if (layerMixProperty == null)Layer.AudioMixPropertyHolder() else Layer.AudioMixPropertyHolder(layerMixProperty)
            layerMixPropertyBuilder.holder = layerMixHolder
            val audioLayerItem = AudioLayerItem(subAudioPath, key.toLong(), subAudioLayer, layerMixPropertyBuilder)
            audioLayerList.add(audioLayerItem)
        }
        audioLayerList.sortWith(Comparator { p0, p1 ->
            if (p0.layerId < p1.layerId) -1
            else if (p0.layerId > p1.layerId) 1
            else 0
        })

        mAudioLayerItems = audioLayerList
        return true
    }

    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path, mEditor!!, true, item.type == AlbumItemType.Video)

        val context = mEditor!!.context
        val videoLayer = ClipLayer(context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        videoLayer.setConfig(videoLayerConfig)

        val mainAudioLayer = ClipLayer(context, false)
        val mainAudioConfig = Config()
        mainAudioConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        mainAudioLayer.setConfig(mainAudioConfig)

        if (!videoLayer.addClip(mVideoItem!!.mId.toInt(), mVideoItem!!.videoClip)) {
            TLog.e("Video clip add error ${mVideoItem!!.mId}")
        }
        if (!mainAudioLayer.addClip(mVideoItem!!.mId.toInt(), mVideoItem!!.audioClip)) {
            TLog.e("Audio clip add error ${mVideoItem!!.mId}")
        }

        mEditor!!.videoComposition().addLayer(1, videoLayer)
        val effect = Effect(mEditor!!.context, AudioTrimEffect.TYPE_NAME)
        val effectConfig = Config()
        effectConfig.setNumber(AudioTrimEffect.CONFIG_BEGIN, 0)
        effectConfig.setNumber(AudioTrimEffect.CONFIG_END, mEditor!!.videoComposition().streamInfo.duration)
        effect.setConfig(effectConfig)

        mEditor!!.audioComposition().addLayer(1, mainAudioLayer)
        mEditor!!.audioComposition().effects().add(10, effect)

        mMainAudioLayer = mainAudioLayer

        initSubLayers()
    }

    private fun initSubLayers() {
        val audioList = mutableListOf<String>(
                "android_asset://audios/city_sunshine.mp3",
                "android_asset://audios/eye_of_forgiveness.mp3",
                "android_asset://audios/lovely_piano_song.mp3",
                "android_asset://audios/motions.mp3",
                "android_asset://audios/pickled_pink.mp3",
                "android_asset://audios/rush.mp3"
        )

        val audioLayerList = ArrayList<AudioLayerItem>()
        for (path in audioList) {
            val audioLayerItem = AudioLayerItem.createAudioLayerItem(path, mEditor!!)
            audioLayerList.add(audioLayerItem)
            mEditor!!.audioComposition().addLayer(audioLayerItem.layerId.toInt(), audioLayerItem.audioLayer)
        }
        refreshEditor()
        for (item in audioLayerList) {
            item.audioLayer.setProperty(Layer.PROP_MIX, item.layerMixPropertyBuilder.makeProperty())
        }


        mAudioLayerItems = audioLayerList
    }
}