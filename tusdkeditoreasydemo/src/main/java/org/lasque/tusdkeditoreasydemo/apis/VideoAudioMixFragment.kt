/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/10/28$ 10:12$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import cn.bar.DoubleHeadedDragonBar
import com.tusdk.pulse.Config
import com.tusdk.pulse.MediaInspector
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.AudioTrimEffect
import kotlinx.android.synthetic.main.movie_cut_fragment.view.lsq_start_bar
import kotlinx.android.synthetic.main.video_audio_mix_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/28  10:12
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class VideoAudioMixFragment : BaseFragment(FunctionType.VideoAudioMix) {

    private var mVideoLayer : Layer? = null
    private var mMainAudioLayer : Layer? = null
    private var mSubAudioLayer : Layer? = null

    private var mMainConfig : Config? = null
    private var mSubConfig : Config? = null

    private var mSubTrimEffect : Effect? = null
    private var mSubTrimConfig : Config? = null

    private var mMainProperty : Layer.AudioMixPropertyBuilder = Layer.AudioMixPropertyBuilder()
    private var mSubProperty : Layer.AudioMixPropertyBuilder = Layer.AudioMixPropertyBuilder()


    private var mVideoItem : VideoItem? = null

    private var mSubClip: Clip? = null

    private var mVideoClip : Clip? = null

    private var mSubStart = 0
    private var mSubEnd = 0

    private var mCurrentMainPos = 0L
    private var mSubStartTime = 0L
    private var mSubEndTime = 0L
    private var mMainVolume = 1.0
    private var mSubVolume = 1.0

    private var mMainAudioDuration = 1L
    private var mSubAudioDuration = 1L

    private var mMaxDuration = 0L

    private var isRestore = false

    override fun getLayoutId(): Int {
       return R.layout.video_audio_mix_fragment
    }

    public fun setCurrentState(){
        val currentVideoHour = mSubStartTime / 3600000
        val currentVideoMinute = (mSubStartTime % 3600000) / 60000

        val currentVideoSecond = (mSubStartTime % 60000 / 1000)

        val durationVideoHour = mSubEndTime / 3600000

        val durationVideoMinute = (mSubEndTime % 3600000) / 60000

        val durationVideoSecond = (mSubEndTime % 60000 / 1000)

        val subDurationVideoHour = mCurrentMainPos / 3600000

        val subDurationVideoMinute = (mCurrentMainPos % 3600000) / 60000

        val subDurationVideoSecond = (mCurrentMainPos % 60000 / 1000)

        lsq_editor_current_state.setText("副音轨素材素材信息 开始时间 : ${currentVideoHour}:$currentVideoMinute:$currentVideoSecond 结束时间 : $durationVideoHour:$durationVideoMinute:$durationVideoSecond \n " +
                "副音轨位于主音轨位置 : $subDurationVideoHour:$subDurationVideoMinute:$subDurationVideoSecond")
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {

            isRestore = restoreLayer()
            if (!isRestore){
                initLayer()
            }

            val videoPath = mVideoClip!!.config.getString(VideoFileClip.CONFIG_PATH)
            mMaxDuration = MediaInspector.shared().inspect(videoPath).streams[0].duration



            runOnUiThread {
                setCurrentState()

                view.lsq_start_bar.setCallBack(object : DoubleHeadedDragonBar.DhdBarCallBack() {
                    override fun getMaxString(value: Int): String {
                        return super.getMaxString(value)
                    }

                    override fun getMinString(value: Int): String {
                        return super.getMinString(value)
                    }

                    override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                        mSubStartTime = (mSubAudioDuration * minPercentage / 100).toLong()
                        mSubEndTime = (mSubAudioDuration * maxPercentage / 100).toLong()
                        if (mSubEndTime - mSubStartTime < 100){
                            mSubEndTime = mSubStartTime + 100
                        }
                        setCurrentState()
                        mThreadPool?.execute {
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mSubTrimConfig!!.setNumber(AudioTrimEffect.CONFIG_BEGIN,mSubStartTime)
                            mSubTrimConfig!!.setNumber(AudioTrimEffect.CONFIG_END,mSubEndTime)
                            mSubTrimEffect!!.setConfig(mSubTrimConfig)
                            refreshEditor()
                            playerUnlock()
                            mEditor!!.player.seekTo(mCurrentMainPos)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }
                    }

                    override fun getMinMaxString(value: Int, value1: Int): String {
                        return super.getMinMaxString(value, value1)
                    }

                })
                view.lsq_start_bar.post {
                    view.lsq_start_bar.minValue = ((mSubStartTime / mMaxDuration.toDouble()) * 100).toInt()
                    view.lsq_start_bar.maxValue = ((mSubEndTime / mMaxDuration.toDouble()) * 100).toInt()
                    view.lsq_start_bar.invalidate()
                }


                lsq_sub_in_main_bar.max = mMainAudioDuration.toInt()
                lsq_sub_in_main_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        mCurrentMainPos = p1.toLong()
                        setCurrentState()
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        mThreadPool?.execute {
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                        }
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        mThreadPool?.execute {
                            playerLock()
                            mSubConfig!!.setNumber(Layer.CONFIG_START_POS,mCurrentMainPos)
                            mSubAudioLayer!!.setConfig(mSubConfig)
                            refreshEditor()
                            playerUnlock()
                            mEditor!!.player.seekTo(mCurrentMainPos)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }
                    }

                })

                lsq_main_audio_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        mMainVolume = p1 / 100.0
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        mThreadPool?.execute {
                            mMainProperty.holder.weight = mMainVolume
                            mMainAudioLayer!!.setProperty(Layer.PROP_MIX,mMainProperty.makeProperty())
                        }
                    }

                })

                lsq_sub_audio_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        mSubVolume = p1 / 100.0
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        mThreadPool?.execute {
                            mSubProperty.holder.weight = mSubVolume
                            mSubAudioLayer!!.setProperty(Layer.PROP_MIX,mSubProperty.makeProperty())

                        }
                    }

                })

                if (isRestore){
                    lsq_sub_in_main_bar.progress = mSubConfig!!.getNumberOr(Layer.CONFIG_START_POS,0.0).toInt()
                    lsq_main_audio_bar.progress = (mMainVolume * 100).toInt()

                    val sub_in_main = mSubConfig!!.getIntNumberOr(Layer.CONFIG_START_POS,0)
                    lsq_sub_in_main_bar.progress = sub_in_main.toInt()

                    lsq_sub_audio_bar.progress = (mSubVolume * 100).toInt()
                }

            }
        }



    }

    private fun playerPause() {
        mThreadPool?.execute {
            mEditor!!.player.pause()
        }
    }

    private fun restoreLayer(): Boolean{
        if (mEditor!!.videoComposition().allLayers.isEmpty()){
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[1] as ClipLayer
        val mainAudioLayer = mEditor!!.audioComposition().allLayers[1] as ClipLayer
        val subAudioLayer = mEditor!!.audioComposition().allLayers[2] as ClipLayer

        val videoClipMap = videoLayer.allClips
        val videoClip = videoLayer.getClip(videoClipMap.keys.first())

        val audioClipMap = mainAudioLayer.allClips
        val mainAudioClip = mainAudioLayer.getClip(audioClipMap.keys.first())

        val subAudioClip = subAudioLayer.getClip(1)

        val duration = mainAudioClip.streamInfo.duration

        val subAudioPath = subAudioClip.config.getString(AudioFileClip.CONFIG_PATH)
        val subDuration = MediaInspector.shared().inspect(subAudioPath).streams[0].duration

        val audioTrimEffect = subAudioClip.effects().get(100)
        val audioTrimEffectConfig = audioTrimEffect.config
        if (mainAudioLayer.getProperty(Layer.PROP_MIX) != null){
            val mainMixHolder = Layer.AudioMixPropertyHolder(mainAudioLayer.getProperty(Layer.PROP_MIX))
            mMainVolume = mainMixHolder.weight
        }
        if (subAudioLayer.getProperty(Layer.PROP_MIX) != null){
            val subMixHolder = Layer.AudioMixPropertyHolder(subAudioLayer.getProperty(Layer.PROP_MIX))
            mSubVolume = subMixHolder.weight
        }
        mSubClip = subAudioClip
        mVideoClip = videoClip

        mMainAudioDuration = duration

        mSubAudioDuration = subDuration

        mSubStartTime = audioTrimEffectConfig.getIntNumber(AudioTrimEffect.CONFIG_BEGIN)
        mSubEndTime = audioTrimEffectConfig.getIntNumber(AudioTrimEffect.CONFIG_END)

        mVideoLayer = videoLayer
        mMainAudioLayer = mainAudioLayer
        mSubAudioLayer = subAudioLayer

        mMainConfig = mainAudioLayer.config
        mSubConfig = subAudioLayer.config

        mSubTrimEffect = audioTrimEffect
        mSubTrimConfig = audioTrimEffectConfig


        return true
    }

    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video)

        val duration = mVideoItem!!.mAudioClip!!.streamInfo.duration

        val context = mEditor!!.context
        val videoLayer = ClipLayer(context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        val mainAudioLayer = ClipLayer(context,false)
        val mainAudioConfig = Config()
        mainAudioConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        mainAudioLayer.setConfig(mainAudioConfig)

        val subAudioLayer = ClipLayer(context,false)
        val subAudioConfig = Config()
        subAudioConfig.setNumber(Layer.PROP_MIX,1)
        subAudioLayer.setConfig(subAudioConfig)

        val subAudioClipConfig = Config()
        subAudioClipConfig.setString(AudioFileClip.CONFIG_PATH,"android_asset://audios/city_sunshine.mp3")
        val subAudioClip = Clip(context,AudioFileClip.TYPE_NAME)
        subAudioClip.setConfig(subAudioClipConfig)

        if (!subAudioClip.activate()){

        }

        val subDuration = subAudioClip.streamInfo.duration

        val trimConfig = Config()
        trimConfig.setNumber(AudioTrimEffect.CONFIG_BEGIN,0)
        trimConfig.setNumber(AudioTrimEffect.CONFIG_END,subDuration)

        val trimEffect = Effect(context,AudioTrimEffect.TYPE_NAME)
        trimEffect.setConfig(trimConfig)
        subAudioClip.effects().add(100,trimEffect)


        if (!videoLayer.addClip(mVideoItem!!.mId.toInt(),mVideoItem!!.videoClip)){
            TLog.e("Video clip add error ${mVideoItem!!.mId}")
        }
        if (!mainAudioLayer.addClip(mVideoItem!!.mId.toInt(),mVideoItem!!.audioClip)){
            TLog.e("Audio clip add error ${mVideoItem!!.mId}")
        }

        if (!subAudioLayer.addClip(1,subAudioClip)){

        }
        mEditor!!.videoComposition().addLayer(1,videoLayer)
        TLog.e("video duration ${mEditor!!.videoComposition().streamInfo.duration}")
        val effect = Effect(mEditor!!.context,AudioTrimEffect.TYPE_NAME)
        val effectConfig = Config()
        effectConfig.setNumber(AudioTrimEffect.CONFIG_BEGIN,0)
        effectConfig.setNumber(AudioTrimEffect.CONFIG_END,mEditor!!.videoComposition().streamInfo.duration)
        effect.setConfig(effectConfig)

        mEditor!!.audioComposition().addLayer(1,mainAudioLayer)
        mEditor!!.audioComposition().addLayer(2,subAudioLayer)
        mEditor!!.audioComposition().effects().add(10,effect)

        mVideoClip = mVideoItem!!.mVideoClip
        mSubClip = subAudioClip

        mMainAudioDuration = duration
        mSubAudioDuration = subDuration

        mSubStartTime = 0
        mSubEndTime = subDuration

        mVideoLayer = videoLayer
        mMainAudioLayer = mainAudioLayer
        mSubAudioLayer = subAudioLayer

        mMainConfig = mainAudioConfig
        mSubConfig = subAudioConfig

        mSubTrimEffect = trimEffect
        mSubTrimConfig = trimConfig
    }
}