/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/9$ 15:23$
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
import com.tusdk.pulse.editor.effects.*
import kotlinx.android.synthetic.main.movie_cut_fragment.*
import kotlinx.android.synthetic.main.slow_fragment.*
import kotlinx.android.synthetic.main.slow_fragment.lsq_editor_current_state
import kotlinx.android.synthetic.main.slow_fragment.view.lsq_start_bar
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
 * @Date        2020/11/9  15:23
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class SlowFragment : BaseFragment(FunctionType.SlowEffect) {

    private var mStartTime = 0L

    private var mEndTime = 0L

    private var mVideoClip: Clip? = null

    private var mAudioClip: Clip? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mVideoSlowEffectConfig = Config()

    private var mVideoSlowEffect : Effect? = null

    private var mAudioSlowEffectConfig = Config()

    private var mAudioSlowEffect : Effect? = null

    private var mCurrentStretch = 1L

    private var mMaxDuration = 0L

    private var isRestore = false


    override fun getLayoutId(): Int {
        return R.layout.slow_fragment
    }

    private fun setCurrentState() {
        val currentVideoHour = mStartTime / 3600000
        val currentVideoMinute = (mStartTime % 3600000) / 60000

        val currentVideoSecond = (mStartTime % 60000 / 1000)

        val durationVideoHour = mEndTime / 3600000

        val durationVideoMinute = (mEndTime % 3600000) / 60000

        val durationVideoSecond = (mEndTime % 60000 / 1000)
        lsq_editor_current_state.setText("慢动作作用范围 开始时间 : ${currentVideoHour}:$currentVideoMinute:$currentVideoSecond 结束时间 : $durationVideoHour:$durationVideoMinute:$durationVideoSecond \n" +
                "慢动作倍数 : ${mCurrentStretch}")
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
                        mEndTime = value.toLong()
                        return super.getMaxString(value)
                    }

                    override fun getMinString(value: Int): String {
                        mStartTime = value.toLong()
                        return super.getMinString(value)
                    }

                    override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                        mStartTime = (mMaxDuration * minPercentage / 100).toLong()
                        mEndTime = (mMaxDuration * maxPercentage / 100).toLong()
                        if (mEndTime - mStartTime < 100){
                            mEndTime = mStartTime + 100
                        }
                        setCurrentState()
                        mThreadPool?.execute {
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mVideoSlowEffectConfig.setNumber(VideoStretchEffect.CONFIG_BEGIN,mStartTime)
                            mAudioSlowEffectConfig.setNumber(AudioStretchEffect.CONFIG_BEGIN,mStartTime)
                            mVideoSlowEffectConfig.setNumber(VideoStretchEffect.CONFIG_END,mEndTime)
                            mAudioSlowEffectConfig.setNumber(AudioStretchEffect.CONFIG_END,mEndTime)
                            mVideoSlowEffect?.setConfig(mVideoSlowEffectConfig)
                            mAudioSlowEffect?.setConfig(mAudioSlowEffectConfig)
                            refreshEditor()
                            playerUnlock()
                            mEditor!!.player.seekTo(mStartTime)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }
                    }

                    override fun getMinMaxString(value: Int, value1: Int): String {
                        return super.getMinMaxString(value, value1)
                    }

                })

                lsq_slow_multiple_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        if (!p2) return
                        mCurrentStretch = p1.toLong()
                        setCurrentState()
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        mThreadPool?.execute {
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                        }
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        mThreadPool?.execute {
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mVideoSlowEffectConfig.setNumber(VideoStretchEffect.CONFIG_STRETCH,mCurrentStretch)
                            mAudioSlowEffectConfig.setNumber(AudioStretchEffect.CONFIG_STRETCH,mCurrentStretch)
                            mVideoSlowEffect?.setConfig(mVideoSlowEffectConfig)
                            mAudioSlowEffect?.setConfig(mAudioSlowEffectConfig)
                            refreshEditor()
                            playerUnlock()
                            mEditor!!.player.seekTo(mStartTime)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }
                    }

                })

                if (isRestore){
                    view.lsq_start_bar.post {
                        view.lsq_start_bar.maxValue = ((mEndTime / mMaxDuration.toDouble()) * 100).toInt()
                        view.lsq_start_bar.minValue = ((mStartTime / mMaxDuration.toDouble()) * 100).toInt()
                        view.lsq_start_bar.invalidate()
                    }
                    lsq_slow_multiple_bar.progress = mCurrentStretch.toInt()
                }
            }
        }
    }

    private fun restoreLayer(): Boolean{
        if (mEditor!!.videoComposition().allLayers.isEmpty()) {
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor!!.audioComposition().allLayers[11] as ClipLayer

        val videoClipMap = videoLayer.allClips
        val videoClip = videoLayer.getClip(videoClipMap.keys.first())

        val audioClipMap = audioLayer.allClips
        val audioClip = audioLayer.getClip(audioClipMap.keys.first())

        mVideoSlowEffect = videoClip.effects().get(100)
        mVideoSlowEffectConfig = mVideoSlowEffect!!.config

        mAudioSlowEffect = audioClip.effects().get(100)
        mAudioSlowEffectConfig = mAudioSlowEffect!!.config

        mStartTime = mVideoSlowEffectConfig.getIntNumber(VideoStretchEffect.CONFIG_BEGIN)
        mEndTime = mVideoSlowEffectConfig.getIntNumber(VideoStretchEffect.CONFIG_END)
        mCurrentStretch = mVideoSlowEffectConfig.getIntNumber(VideoStretchEffect.CONFIG_STRETCH)

        mVideoClip = videoClip
        mAudioClip = audioClip

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
        return true
    }

    private fun initLayer() {
        val item = mVideoList!![0]
        var mVideoItem = VideoItem.createVideoItem(item.path, mEditor!!, true, item.type == AlbumItemType.Video,item.audioPath)

        val videoClip = mVideoItem.mVideoClip
        val audioClip = mVideoItem.mAudioClip

        val duration = videoClip.streamInfo.duration
        mStartTime = 0
        mEndTime = duration

        val videoLayer = ClipLayer(mEditor!!.context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        if (!videoLayer.addClip(100,videoClip)){
            return
        }

        val audioLayer = ClipLayer(mEditor!!.context,false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        audioLayer.setConfig(audioLayerConfig)

        if (!audioLayer.addClip(100, audioClip)){
            return
        }

        mVideoSlowEffectConfig.setNumber(VideoStretchEffect.CONFIG_BEGIN,0)
        mVideoSlowEffectConfig.setNumber(VideoStretchEffect.CONFIG_END,duration)
        mVideoSlowEffect = Effect(mEditor!!.context,VideoStretchEffect.TYPE_NAME)
        mVideoSlowEffect?.setConfig(mVideoSlowEffectConfig)
        videoClip.effects().add(100,mVideoSlowEffect)

        mAudioSlowEffectConfig.setNumber(AudioStretchEffect.CONFIG_BEGIN,0)
        mAudioSlowEffectConfig.setNumber(AudioStretchEffect.CONFIG_END,duration)
        mAudioSlowEffect = Effect(mEditor!!.context,AudioStretchEffect.TYPE_NAME)
        mAudioSlowEffect?.setConfig(mAudioSlowEffectConfig)
        audioClip.effects().add(100,mAudioSlowEffect)


        mEditor!!.videoComposition().addLayer(11,videoLayer)
        mEditor!!.audioComposition().addLayer(11,audioLayer)

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
    }
}