/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/20$ 18:14$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.tusdk.pulse.Config
import com.tusdk.pulse.Producer
import com.tusdk.pulse.Transcoder
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.effects.AudioStretchEffect
import com.tusdk.pulse.editor.effects.CanvasResizeEffect
import com.tusdk.pulse.editor.effects.VideoStretchEffect
import kotlinx.android.synthetic.main.speed_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.jetbrains.anko.support.v4.toast
import org.lasque.tusdkpulse.core.TuSdk
import org.lasque.tusdkpulse.core.utils.StringHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import java.io.File
import java.util.concurrent.Semaphore

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/20  18:14
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class SpeedFragment : BaseFragment(FunctionType.Speed) {

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

    private var mCurrentStretch = 1.0

    private var isRestore = false


    override fun getLayoutId(): Int {
        return R.layout.speed_fragment
    }

    private fun setCurrentState() {
        lsq_editor_current_state.setText("当前播放速率 : ${String.format("%.2f",1.0/mCurrentStretch)}")
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
        mCurrentStretch = mVideoSlowEffectConfig.getNumber(VideoStretchEffect.CONFIG_STRETCH)

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
        mVideoClip = videoClip
        mAudioClip = audioClip

        return true
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            //todo 未来预计增加转码逻辑,待底层加速播放逻辑调整结束
//            val path = mVideoList!![0]
//            videoTranscoder(path)

            isRestore = restoreLayer()
            if (!isRestore){
                initLayer()
            }

            runOnUiThread {
                setCurrentState()
                lsq_slow_multiple_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    private var currentFrame = 0L
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        if (!p2) return
                        if (p1 == 3){
                            mCurrentStretch = 1.0
                        } else if (p1 < 3){
                            mCurrentStretch =(3 - p1 + 1).toDouble()
                        } else if (p1 > 3){
                            mCurrentStretch = 1.0 /  (p1 - 3 + 1)
                        }
                        setCurrentState()
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        mThreadPool?.execute {
                            currentFrame = mPlayerContext!!.currentFrame
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                        }
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        mThreadPool?.execute {
                            playerLock()
                            mVideoSlowEffectConfig.setNumber(VideoStretchEffect.CONFIG_STRETCH,mCurrentStretch)
                            mAudioSlowEffectConfig.setNumber(AudioStretchEffect.CONFIG_STRETCH,mCurrentStretch)
                            mVideoSlowEffect?.setConfig(mVideoSlowEffectConfig)
                            mAudioSlowEffect?.setConfig(mAudioSlowEffectConfig)
                            refreshEditor()
                            playerUnlock()
                            mEditor!!.player.seekTo(0)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }
                    }

                })

                if (isRestore){
                    var p = 3
                    if (mCurrentStretch > 1.0){
                        if (mCurrentStretch == 2.0) p = 2
                        else p = 1
                    } else if (mCurrentStretch < 1.0){
                        if (mCurrentStretch == 0.5) p = 4
                        else p = 5


                    }
                    lsq_slow_multiple_bar.progress = p
                }
            }
        }
    }

    private val transcoderSemaphore : Semaphore = Semaphore(0)


    private fun videoTranscoder(inputPath : String) : String{
        val outputPath = getOutputTempFilePath().path
        val transcoder = Transcoder()
        transcoder.setListener { state, ts ->
            if (state == Producer.State.kWRITING){
//                runOnUiThread {
//                    lsq_editor_cut_load.setVisibility(View.VISIBLE)
//                    lsq_editor_cut_load_parogress.setValue((ts / transcoder.duration.toFloat()) * 100f)
//                }
            } else if (state == Producer.State.kEND){
                transcoderSemaphore.release()
//                runOnUiThread {
//                    lsq_editor_cut_load.setVisibility(View.GONE)
//                    lsq_editor_cut_load_parogress.setValue(0f)
//                    Toast.makeText(requireContext(), "视频转码结束", Toast.LENGTH_SHORT).show()
//                }
            }
        }
        val transcoderConfig = Producer.OutputConfig()
        transcoderConfig.keyint = 0
        transcoder.setOutputConfig(transcoderConfig)

        if (!transcoder.init(outputPath,inputPath)){
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

        val resizeConfig = Config()
        val resizeEffect = Effect(mEditor!!.context, CanvasResizeEffect.TYPE_NAME)
        resizeEffect.setConfig(resizeConfig)
        videoClip.effects().add(1,resizeEffect)

        mVideoSlowEffectConfig.setNumber(VideoStretchEffect.CONFIG_BEGIN,0)
        mVideoSlowEffectConfig.setNumber(VideoStretchEffect.CONFIG_END,duration)
        mVideoSlowEffect = Effect(mEditor!!.context, VideoStretchEffect.TYPE_NAME)
        mVideoSlowEffect?.setConfig(mVideoSlowEffectConfig)
        videoClip.effects().add(100,mVideoSlowEffect)

        mAudioSlowEffectConfig.setNumber(AudioStretchEffect.CONFIG_BEGIN,0)
        mAudioSlowEffectConfig.setNumber(AudioStretchEffect.CONFIG_END,duration)
        mAudioSlowEffect = Effect(mEditor!!.context, AudioStretchEffect.TYPE_NAME)
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