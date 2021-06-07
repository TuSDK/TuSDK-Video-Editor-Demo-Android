/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2021/4/27$ 15:23$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.effects.AudioFreezeEffect
import com.tusdk.pulse.editor.effects.VideoFreezeEffect
import kotlinx.android.synthetic.main.freeze_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkpulse.core.utils.TLog
import java.util.concurrent.Callable
import kotlin.math.max

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/4/27  15:23
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class FreezeFragment : BaseFragment(FunctionType.Freeze) {

    private var mVideoItem: VideoItem? = null

    private var mFreezePos = 0L

    private var mVideoClip : Clip? = null

    private var mAudioClip : Clip? = null

    private var mVideoLayer : ClipLayer? = null

    private var mAudioLayer : ClipLayer? = null

    private var mVideoFreezeEffectConfig = Config()

    private var mVideoFreezeEffect : Effect? = null

    private var mAudioFreezeEffectConfig = Config()

    private var mAudioFreezeEffect : Effect? = null

    private var mCurrentFreezeDuration = 1000L

    private var mMaxDuration = 0L

    private var isRestore = false

    override fun getLayoutId(): Int {
        return R.layout.freeze_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {

        mThreadPool?.execute {
            isRestore = restoreLayer()
            if (!isRestore){
                initLayer()
            }

            runOnUiThread {

                lsq_start_bar.max = mMaxDuration.toInt()

                lsq_start_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (!fromUser) return

                        mFreezePos = progress.toLong()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        setFreeze(mFreezePos,mCurrentFreezeDuration)

                    }

                })

                lsq_freeze_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (!fromUser) return

                        mCurrentFreezeDuration = ((progress + 1) * 1000).toLong()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        setFreeze(mFreezePos,mCurrentFreezeDuration)
                    }

                })

                if (isRestore){
                    val durationProgress = ((mCurrentFreezeDuration / 1000) -1).toInt()
                    lsq_freeze_bar.setProgress(durationProgress)

                    val posProgress = mFreezePos.toInt()
                    lsq_start_bar.progress = posProgress

                }

                setFreezeState()

            }

        }

    }

    private fun restoreLayer() : Boolean{
        if (mEditor!!.videoComposition().allLayers.isEmpty()){
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor!!.audioComposition().allLayers[11] as ClipLayer

        val videoClipMap = videoLayer.allClips
        val videoClip = videoLayer.getClip(videoClipMap.keys.first())

        val audioClipMap = videoLayer.allClips
        val audioClip = audioLayer.getClip(audioClipMap.keys.first())

        mMaxDuration = videoClip.originStreamInfo.duration

        mVideoFreezeEffect = videoClip.effects().get(400)
        mVideoFreezeEffectConfig = mVideoFreezeEffect!!.config

        mAudioFreezeEffect = audioClip.effects().get(400)
        mAudioFreezeEffectConfig = mAudioFreezeEffect!!.config

        mFreezePos = mVideoFreezeEffectConfig.getNumberOr(VideoFreezeEffect.CONFIG_FREEZE_POS,-1.0).toLong()
        mCurrentFreezeDuration = mVideoFreezeEffectConfig.getNumberOr(VideoFreezeEffect.CONFIG_FREEZE_DURATION,0.0).toLong()

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        return true

    }


    private fun initLayer(){
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video,item.audioPath)
        val videoClip = mVideoItem!!.mVideoClip
        val audioClip = mVideoItem!!.mAudioClip

        val duration = videoClip.streamInfo.duration
        var audioDuration = audioClip.streamInfo.duration

        val videoLayer = ClipLayer(mEditor!!.context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        if (!videoLayer.addClip(100,videoClip)){
            return
        }

        TLog.e("video duration ${duration} audio duration ${audioDuration}")

        val audioLayer = ClipLayer(mEditor!!.context,false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        audioLayer.setConfig(audioLayerConfig)

        if (!audioLayer.addClip(100,audioClip)){

        }

        audioDuration = audioClip.streamInfo.duration
        mMaxDuration = videoClip.originStreamInfo.duration

        mEditor!!.videoComposition().addLayer(11,videoLayer)
        mEditor!!.audioComposition().addLayer(11,audioLayer)

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
    }

    private fun setFreeze(freezePos : Long,freezeDuration : Long){
        val res = mThreadPool!!.submit(Callable<Boolean>() {

            playerLock()

            if (mVideoFreezeEffect == null){

                val videoFreezeEffect = Effect(mEditor!!.context,VideoFreezeEffect.TYPE_NAME)
                val videoFreezeConfig = Config()
                videoFreezeConfig.setNumber(VideoFreezeEffect.CONFIG_FREEZE_POS,freezePos)
                videoFreezeConfig.setNumber(VideoFreezeEffect.CONFIG_FREEZE_DURATION,freezeDuration)
                videoFreezeEffect.setConfig(videoFreezeConfig)

                val audioFreezeEffect = Effect(mEditor!!.context,AudioFreezeEffect.TYPE_NAME)
                val audioFreezeConfig = Config()
                audioFreezeConfig.setNumber(AudioFreezeEffect.CONFIG_FREEZE_POS,freezePos)
                audioFreezeConfig.setNumber(AudioFreezeEffect.CONFIG_FREEZE_DURATION,freezeDuration)
                audioFreezeEffect.setConfig(videoFreezeConfig)

                mVideoClip!!.effects().add(400,videoFreezeEffect)
                mAudioClip!!.effects().add(400,audioFreezeEffect)

                mVideoFreezeEffect = videoFreezeEffect
                mVideoFreezeEffectConfig = videoFreezeConfig

                mAudioFreezeEffect = audioFreezeEffect
                mAudioFreezeEffectConfig = audioFreezeConfig
            } else {
                mVideoFreezeEffectConfig.setNumber(VideoFreezeEffect.CONFIG_FREEZE_POS,freezePos)
                mVideoFreezeEffectConfig.setNumber(VideoFreezeEffect.CONFIG_FREEZE_DURATION,freezeDuration)

                mAudioFreezeEffectConfig.setNumber(AudioFreezeEffect.CONFIG_FREEZE_POS,freezePos)
                mAudioFreezeEffectConfig.setNumber(AudioFreezeEffect.CONFIG_FREEZE_DURATION,freezeDuration)

                mVideoFreezeEffect!!.setConfig(mVideoFreezeEffectConfig)
                mAudioFreezeEffect!!.setConfig(mAudioFreezeEffectConfig)
            }
            refreshEditor()
            playerUnlock()
            mOnPlayerStateUpdateListener?.onDurationUpdate()

            mEditor!!.player.seekTo(max(freezePos - 400,0))
            mOnPlayerStateUpdateListener?.onPlayerPlay()


            true
        })

        res.get()

        setFreezeState()
    }

    private fun setFreezeState(){
        val m = mFreezePos / 1000 / 60
        val s = mFreezePos / 1000 % 60
        lsq_freeze_state.setText("定格位置 : ${String.format("%02d",m)}:${String.format("%02d",s)} 定格持续时间 : ${mCurrentFreezeDuration / 1000}s")
    }

}