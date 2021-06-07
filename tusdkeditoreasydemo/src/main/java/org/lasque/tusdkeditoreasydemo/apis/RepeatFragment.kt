/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/9$ 16:41$
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
import kotlinx.android.synthetic.main.movie_cut_fragment.view.*
import kotlinx.android.synthetic.main.repeat_fragment.*
import kotlinx.android.synthetic.main.repeat_fragment.view.lsq_start_bar
import org.jetbrains.anko.support.v4.runOnUiThread
import org.jetbrains.anko.support.v4.toast
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import kotlin.math.ceil
import kotlin.math.min

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/9  16:41
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class RepeatFragment : BaseFragment(FunctionType.RepeatEffect) {

    private var mVideoItem : VideoItem? = null

    private var mStartTime = 0L

    private var mEndTime = 0L

    private var mVideoClip: Clip? = null

    private var mAudioClip: Clip? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mVideoRepeatEffectConfig = Config()

    private var mVideoRepeatEffect : Effect? = null

    private var mAudioRepeatEffectConfig = Config()

    private var mAudioRepeatEffect : Effect? = null

    private var mCurrentRepeat = 1L

    private var mMaxDuration = 0L

    private var isLayout = false

    private var isRestore = false


    override fun getLayoutId(): Int {
        return R.layout.repeat_fragment
    }

    private fun setCurrentState() {
        val currentVideoHour = mStartTime / 3600000
        val currentVideoMinute = (mStartTime % 3600000) / 60000

        val currentVideoSecond = (mStartTime % 60000.00 / 1000.00)

        val durationVideoHour = mEndTime / 3600000

        val durationVideoMinute = (mEndTime % 3600000) / 60000

        val durationVideoSecond = (mEndTime % 60000.00 / 1000.00)
        lsq_editor_current_state.setText("当前重复区间 开始时间 : ${currentVideoHour}:$currentVideoMinute:$currentVideoSecond 结束时间 : $durationVideoHour:$durationVideoMinute:$durationVideoSecond \n" +
                "重复次数 : ${mCurrentRepeat - 1}")
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

        mVideoRepeatEffect = videoClip.effects().get(100)
        mVideoRepeatEffectConfig = mVideoRepeatEffect!!.config

        mAudioRepeatEffect = audioClip.effects().get(100)
        mAudioRepeatEffectConfig = mAudioRepeatEffect!!.config

        mStartTime = mVideoRepeatEffectConfig.getIntNumber(VideoRepeatEffect.CONFIG_BEGIN)
        mEndTime = mVideoRepeatEffectConfig.getIntNumber(VideoRepeatEffect.CONFIG_END)
        mCurrentRepeat = mVideoRepeatEffectConfig.getIntNumber(VideoRepeatEffect.CONFIG_REPEAT_COUNT)

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        return true
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            isRestore = restoreLayer()
            if (!isRestore){
                initLayer()
                mEndTime = 0
            }

            val videoPath = mVideoClip!!.config.getString(VideoFileClip.CONFIG_PATH)
//            mMaxDuration = MediaInspector.shared().inspect(videoPath).streams[0].duration
            mMaxDuration = mVideoClip!!.originStreamInfo.duration

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
                        val startTime = (mMaxDuration * minPercentage / 100).toLong()
                        val endTime = (mMaxDuration * maxPercentage / 100).toLong()

                        if (startTime == endTime){
                            toast("重复区间不能为0")
                            view.lsq_start_bar.minValue = (mStartTime / mMaxDuration.toFloat() * 100.0).toInt()
                            view.lsq_start_bar.maxValue = (mEndTime / mMaxDuration.toFloat() * 100.0).toInt()
                            view.lsq_start_bar.invalidate()
                            return
                        }
                        mStartTime = startTime
                        mEndTime = endTime
                        setCurrentState()

                        mThreadPool?.execute {
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mVideoRepeatEffectConfig.setNumber(VideoRepeatEffect.CONFIG_BEGIN,mStartTime)
                            mAudioRepeatEffectConfig.setNumber(AudioRepeatEffect.CONFIG_BEGIN,mStartTime)
                            mVideoRepeatEffectConfig.setNumber(VideoRepeatEffect.CONFIG_END,mEndTime)
                            mAudioRepeatEffectConfig.setNumber(AudioRepeatEffect.CONFIG_END,mEndTime)
                            mVideoRepeatEffect?.setConfig(mVideoRepeatEffectConfig)
                            mAudioRepeatEffect?.setConfig(mAudioRepeatEffectConfig)
                            refreshEditor()
                            playerUnlock()
                            mEditor?.player?.seekTo(mStartTime)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }
                    }

                    override fun getMinMaxString(value: Int, value1: Int): String {
                        return super.getMinMaxString(value, value1)
                    }

                })
                view.lsq_start_bar.post {
                    val clazz = view.lsq_start_bar.javaClass
                    val minModeFiled = clazz.getDeclaredField("isMinMode")
                    minModeFiled.isAccessible = true
                    minModeFiled.set(view.lsq_start_bar,false)
                    view.lsq_start_bar.minValue = ((mStartTime / mMaxDuration.toDouble()) * 100).toInt()
                    view.lsq_start_bar.maxValue = ((mEndTime / mMaxDuration.toDouble()) * 100).toInt()
                    view.lsq_start_bar.invalidate()
                }

                lsq_repeat_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        if (!p2) return
                        mCurrentRepeat = p1.toLong() + 1
                        setCurrentState()
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        mThreadPool?.execute {
                            mEditor!!.player.pause()
                        }
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        mThreadPool?.execute {
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mVideoRepeatEffectConfig.setNumber(VideoRepeatEffect.CONFIG_REPEAT_COUNT,mCurrentRepeat)
                            mAudioRepeatEffectConfig.setNumber(AudioRepeatEffect.CONFIG_REPEAT_COUNT,mCurrentRepeat)
                            mVideoRepeatEffect?.setConfig(mVideoRepeatEffectConfig)
                            mAudioRepeatEffect?.setConfig(mAudioRepeatEffectConfig)
                            refreshEditor()
                            playerUnlock()
                            mEditor?.player?.seekTo(mStartTime)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }
                    }

                })
                if (isRestore){
                    lsq_repeat_bar.progress = (mCurrentRepeat - 1).toInt()
                }
            }

        }
    }

    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video,item.audioPath)
        val videoClip = mVideoItem!!.mVideoClip
        val audioClip = mVideoItem!!.mAudioClip

        val duration = videoClip.streamInfo.duration
        var audioDuration = audioClip.streamInfo.duration
        mStartTime = 0
        mEndTime = 1


        val videoLayer = ClipLayer(mEditor!!.context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        if (!videoLayer.addClip(100,videoClip)){
            return
        }

        TLog.e("video duration ${duration} audio duration ${audioDuration}")

        val audioConfig = audioClip.config
        audioConfig.setNumber(AudioFileClip.CONFIG_TRIM_DURATION,duration)
        val res = audioClip.setConfig(audioConfig)

        val audioLayer = ClipLayer(mEditor!!.context,false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        audioLayer.setConfig(audioLayerConfig)

        if (!audioLayer.addClip(100, audioClip)){
            return
        }
        audioDuration = audioClip.streamInfo.duration
        TLog.e("video duration ${duration} audio duration ${audioDuration} res ${res}")


        val resizeConfig = Config()
        val resizeEffect = Effect(mEditor!!.context, CanvasResizeEffect.TYPE_NAME)
        resizeEffect.setConfig(resizeConfig)
        videoClip.effects().add(1,resizeEffect)

        mVideoRepeatEffectConfig.setNumber(VideoRepeatEffect.CONFIG_BEGIN,mStartTime)
        mVideoRepeatEffectConfig.setNumber(VideoRepeatEffect.CONFIG_END,mEndTime)
        mVideoRepeatEffectConfig.setNumber(VideoRepeatEffect.CONFIG_REPEAT_COUNT,mCurrentRepeat)
        mVideoRepeatEffect = Effect(mEditor!!.context,VideoRepeatEffect.TYPE_NAME)
        mVideoRepeatEffect?.setConfig(mVideoRepeatEffectConfig)
        videoClip.effects().add(100,mVideoRepeatEffect)

        mAudioRepeatEffectConfig.setNumber(AudioRepeatEffect.CONFIG_BEGIN,mStartTime)
        mAudioRepeatEffectConfig.setNumber(AudioRepeatEffect.CONFIG_END,mEndTime)
        mAudioRepeatEffectConfig.setNumber(AudioRepeatEffect.CONFIG_REPEAT_COUNT,mCurrentRepeat)
        mAudioRepeatEffect = Effect(mEditor!!.context,AudioRepeatEffect.TYPE_NAME)
        mAudioRepeatEffect?.setConfig(mAudioRepeatEffectConfig)
        audioClip.effects().add(100,mAudioRepeatEffect)

        mEditor!!.videoComposition().addLayer(11,videoLayer)
        mEditor!!.audioComposition().addLayer(11,audioLayer)

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

    }
}