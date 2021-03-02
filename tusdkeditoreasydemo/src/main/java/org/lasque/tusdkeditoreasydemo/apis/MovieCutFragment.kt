/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/10/23$ 9:57$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import android.widget.Toast
import cn.bar.DoubleHeadedDragonBar
import com.tusdk.pulse.Config
import com.tusdk.pulse.MediaInspector
import com.tusdk.pulse.Producer
import com.tusdk.pulse.Transcoder
import com.tusdk.pulse.editor.*
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.AudioTrimEffect
import com.tusdk.pulse.editor.effects.VideoTrimEffect
import kotlinx.android.synthetic.main.movie_cut_fragment.*
import kotlinx.android.synthetic.main.movie_cut_fragment.view.*
import kotlinx.android.synthetic.main.time_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.jetbrains.anko.support.v4.toast
import org.lasque.tusdkeditoreasydemo.ApiActivity
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkpulse.core.TuSdk
import org.lasque.tusdkpulse.core.utils.StringHelper
import java.io.File
import java.util.concurrent.Semaphore

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/23  9:56
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 * 视频裁剪Api
 */
class MovieCutFragment : BaseFragment(FunctionType.MoiveCut) {

    private var mVideoItem: VideoItem? = null

    private var mVideoTrimConfig = Config()

    private var mAudioTrimConfig = Config()

    private var mVideoClip: Clip? = null

    private var mAudioClip: Clip? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mStartTime = 0L

    private var mEndTime = 0L

    private var mVideoTrimEffect: Effect? = null

    private var mAudioTrimEffect: Effect? = null

    private var mMaxDuration = 0L

    override fun getLayoutId(): Int {
        return R.layout.movie_cut_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool!!.execute {
            if (!restoreLayer()) {
                initLayer()
            }

            val videoPath = mVideoClip!!.config.getString(VideoFileClip.CONFIG_PATH)
            mMaxDuration = MediaInspector.shared().inspect(videoPath).streams[0].duration
            TLog.e("max percent max duration ${mMaxDuration}")



            runOnUiThread {
                view.lsq_start_bar.setCallBack(object : DoubleHeadedDragonBar.DhdBarCallBack() {
                    override fun getMaxString(value: Int): String {
                        mEndTime = value.toLong()
                        TLog.e("start : ${mStartTime} end : $mEndTime")
                        return super.getMaxString(value)
                    }

                    override fun getMinString(value: Int): String {
                        mStartTime = value.toLong()
                        TLog.e("start : ${mStartTime} end : $mEndTime")
                        return super.getMinString(value)
                    }

                    override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                        val startTime = (mMaxDuration * minPercentage / 100).toLong()
                        val endTime = (mMaxDuration * maxPercentage / 100).toLong()

                        if (endTime - startTime < 1000){
                            toast("裁剪区间不能小于1秒")
                            view.lsq_start_bar.minValue = (mStartTime / mMaxDuration.toFloat() * 100.0).toInt()
                            view.lsq_start_bar.maxValue = (mEndTime / mMaxDuration.toFloat() * 100.0).toInt()
                            view.lsq_start_bar.invalidate()
                            return
                        }

                        mStartTime = startTime
                        mEndTime = endTime
                        setCurrentState()
                        mThreadPool?.execute {
                            playerLock()
                            mVideoTrimConfig.setNumber(VideoTrimEffect.CONFIG_BEGIN, mStartTime)
                            mVideoTrimConfig.setNumber(VideoTrimEffect.CONFIG_END, mEndTime)
                            mAudioTrimConfig.setNumber(AudioTrimEffect.CONFIG_BEGIN, mStartTime)
                            mAudioTrimConfig.setNumber(AudioTrimEffect.CONFIG_END, mEndTime)
                            mVideoTrimEffect?.setConfig(mVideoTrimConfig)
                            mAudioTrimEffect?.setConfig(mAudioTrimConfig)
                            refreshEditor()
                            playerUnlock()
                            mOnPlayerStateUpdateListener?.onDurationUpdate()
                            mEditor!!.player.seekTo(0)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }
                    }

                    override fun getMinMaxString(value: Int, value1: Int): String {
                        return super.getMinMaxString(value, value1)
                    }

                })
                view.lsq_start_bar.post {
                    TLog.e("endPath ${mEndTime} max ${mMaxDuration}")
                    view.lsq_start_bar.minValue = ((mStartTime / mMaxDuration.toDouble()) * 100).toInt()
                    view.lsq_start_bar.maxValue = ((mEndTime / mMaxDuration.toDouble()) * 100).toInt()
                    view.lsq_start_bar.invalidate()
                }
            }
        }
    }

    private fun restoreLayer(): Boolean {
        if (mEditor!!.videoComposition().allLayers.isEmpty()) {
            return false
        } else {
            val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer
            val videoClip = videoLayer.getClip(100)
            val videoTrimEffect = videoClip.effects()[100]

            val audioLayer = mEditor!!.audioComposition().allLayers[11] as ClipLayer
            val audioClip = audioLayer.getClip(100)
            val audioTrimEffect = audioClip.effects()[100]

            val duration = videoClip.streamInfo.duration
            mStartTime = videoTrimEffect.config.getIntNumber(VideoTrimEffect.CONFIG_BEGIN)
            mEndTime = videoTrimEffect.config.getIntNumber(VideoTrimEffect.CONFIG_END)

            mVideoClip = videoClip
            mAudioClip = audioClip
            mVideoLayer = videoLayer
            mAudioLayer = audioLayer
            mVideoTrimEffect = videoTrimEffect
            mAudioTrimEffect = audioTrimEffect

            runOnUiThread {
                setCurrentState()
            }
        }
        return true
    }
    private val transcoderSemaphore: Semaphore = Semaphore(0)

    private fun videoTranscoder(inputPath: String): String {
        val outputPath = getOutputTempFilePath().path
        val transcoder = Transcoder()
        transcoder.setListener(object : Producer.Listener {
            override fun onEvent(state: Producer.State?, ts: Long) {
                if (state == Producer.State.kWRITING) {

                } else if (state == Producer.State.kEND) {
                    transcoderSemaphore.release()
                    mThreadPool?.execute {
                        transcoder.release()
                    }
                    runOnUiThread {
                        Toast.makeText(requireContext(), "视频转码结束", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })

        val transcoderConfig = Producer.OutputConfig()
        transcoderConfig.keyint = 0
        transcoder.setOutputConfig(transcoderConfig)

        if (!transcoder.init(outputPath, inputPath)) {
            TLog.e("Transcoder Error")
        }
        transcoder.start()
        runOnUiThread {
            toast("视频转码开始")
        }
        transcoderSemaphore.acquire()
        return outputPath
    }

    /** 获取临时文件路径  */
    protected fun getOutputTempFilePath(): File {
        return File(TuSdk.getAppTempPath(), String.format("lsq_%s.mp4", StringHelper.timeStampString()))
    }

    private fun initLayer() {
        val item = mVideoList!![0]
//        item.path = videoTranscoder(item.path)


        mVideoItem = VideoItem.createVideoItem(item.path, mEditor!!, true, item.type == AlbumItemType.Video)

        val videoClip = mVideoItem!!.mVideoClip
        val audioClip = mVideoItem!!.mAudioClip

        val duration = videoClip.streamInfo.duration
        mStartTime = 0
        mEndTime = duration

        var videoLayer = ClipLayer(mEditor!!.context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        videoLayer.setConfig(videoLayerConfig)

        if (!videoLayer.addClip(100, videoClip)) {
            return
        }

        var audioLayer = ClipLayer(mEditor!!.context, false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        audioLayer.setConfig(audioLayerConfig)
        if (!audioLayer.addClip(100, audioClip)) {
            return
        }

        mVideoTrimConfig.setNumber(VideoTrimEffect.CONFIG_BEGIN, 0)
        mVideoTrimConfig.setNumber(VideoTrimEffect.CONFIG_END, duration)
        mVideoTrimEffect = Effect(mEditor!!.context, VideoTrimEffect.TYPE_NAME)
        mVideoTrimEffect?.setConfig(mVideoTrimConfig)
        videoClip.effects().add(100, mVideoTrimEffect)

        mAudioTrimConfig.setNumber(AudioTrimEffect.CONFIG_BEGIN, 0)
        mAudioTrimConfig.setNumber(AudioTrimEffect.CONFIG_END, duration)
        mAudioTrimEffect = Effect(mEditor!!.context, AudioTrimEffect.TYPE_NAME)
        mAudioTrimEffect?.setConfig(mAudioTrimConfig)
        audioClip.effects().add(100, mAudioTrimEffect)

        mEditor!!.videoComposition().addLayer(11, videoLayer)
        mEditor!!.audioComposition().addLayer(11, audioLayer)

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        runOnUiThread {
            setCurrentState()
        }
    }

    private fun setCurrentState() {
        val currentVideoHour = mStartTime / 3600000
        val currentVideoMinute = (mStartTime % 3600000) / 60000

        val currentVideoSecond = (mStartTime % 60000 / 1000)

        val durationVideoHour = mEndTime / 3600000

        val durationVideoMinute = (mEndTime % 3600000) / 60000

        val durationVideoSecond = (mEndTime % 60000 / 1000)

        val currentDuration = mEndTime - mStartTime

        val currentDurationVideoHour = currentDuration / 3600000

        val currentDurationVideoMinute = (currentDuration % 3600000) / 60000

        val currentDurationVideoSecond = (currentDuration % 60000 / 1000)

        lsq_editor_current_state.setText("当前片段 开始时间 : ${currentVideoHour}:$currentVideoMinute:$currentVideoSecond 结束时间 : $durationVideoHour:$durationVideoMinute:$durationVideoSecond \n" +
                "当前输出时长 : ${currentDurationVideoHour}:$currentDurationVideoMinute:$currentDurationVideoSecond")
    }

}