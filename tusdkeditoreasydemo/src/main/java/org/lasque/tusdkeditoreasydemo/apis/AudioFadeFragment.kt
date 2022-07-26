/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2021/8/17$ 15:29$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import cn.bar.DoubleHeadedDragonBar
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.effects.AudioFadeEffect
import com.tusdk.pulse.editor.effects.AudioTrimEffect
import kotlinx.android.synthetic.main.audio_fade_fragment.view.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.AudioLayerItem
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkpulse.core.utils.TLog

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/8/17  15:29
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AudioFadeFragment : BaseFragment(FunctionType.AudioFade){

    private var Music_Path = "android_asset://audios/city_sunshine.mp3"

    private var mVideoItem: VideoItem? = null

    private var mVideoClip : Clip? = null

    private var mAudioClip : Clip? = null

    private var mVideoLayer : ClipLayer? = null

    private var mAudioLayer : ClipLayer? = null

    private var mMusicAudioLayer : ClipLayer? = null

    private var mAudioFadeEffectConfig = Config()

    private var mAudioFadeEffect : Effect? = null

    private var mFadeInDuration = 0L

    private var mFadeOutDuration = 0L

    private var mMaxDuration = 0L

    private var isRestore = false



    override fun getLayoutId(): Int {
        return R.layout.audio_fade_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            if (!restoreLayer()) {
                initLayer()
            }

            runOnUiThread {
                view.lsq_fade_start_bar.setCallBack(object :
                    DoubleHeadedDragonBar.DhdBarCallBack() {

                    override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                        super.onEndTouch(minPercentage, maxPercentage)

                        mFadeInDuration = (mMaxDuration * minPercentage / 100.0).toLong()
                        mFadeOutDuration = (mMaxDuration - (mMaxDuration * maxPercentage / 100.0)).toLong()

                        refreshFadeState()

                        mThreadPool?.execute {
                            mAudioFadeEffectConfig.setNumber(AudioFadeEffect.CONFIG_FADE_IN_DURATION,mFadeInDuration)
                            mAudioFadeEffectConfig.setNumber(AudioFadeEffect.CONFIG_FADE_OUT_DURATION,mFadeOutDuration)
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mAudioFadeEffect?.setConfig(mAudioFadeEffectConfig)
                            refreshEditor()
                            playerUnlock()
                            mEditor!!.player.seekTo(0)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }

                    }

                })

                refreshFadeState()

                if (isRestore){
                    val minPercent = mFadeInDuration.toFloat() / mMaxDuration
                    val maxPercent = (mMaxDuration - mFadeOutDuration).toFloat() / mMaxDuration
                    view.lsq_fade_start_bar.minValue = (minPercent* 100).toInt()
                    view.lsq_fade_start_bar.maxValue = (maxPercent * 100).toInt()
                    view.lsq_fade_start_bar.invalidate()
                }
            }


        }

    }

    private fun refreshFadeState(){
        val fadeInM = mFadeInDuration / 1000 / 60
        val fadeInS = mFadeInDuration / 1000 % 60

        val fadeOutM = (mMaxDuration - mFadeOutDuration) / 1000 / 60
        val fadeOutS = (mMaxDuration - mFadeOutDuration) / 1000 % 60

        val endM = mMaxDuration / 1000 / 60
        val endS = mMaxDuration / 1000 % 60

        view?.lsq_fade_state?.setText("淡入开始 : 00:00 结束 ${String.format("%02d",fadeInM)}:${String.format("%02d",fadeInS)} \n 淡出开始 : ${String.format("%02d",fadeOutM)}:${String.format("%02d",fadeOutS)} 结束 : ${String.format("%02d",endM)}:${String.format("%02d",endS)}")
    }

    private fun restoreLayer() : Boolean{
        if (mEditor!!.videoComposition().allLayers.isEmpty()){
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor!!.audioComposition().allLayers[11] as ClipLayer

        val videoClipMap = videoLayer.allClips
        val videoClip = videoLayer.getClip(videoClipMap.keys.first())

        val audioClipMap = audioLayer.allClips
        val audioClip = audioLayer.getClip(audioClipMap.keys.first())

        val musicAudioLayer = mEditor!!.audioComposition().allLayers[400]

        mMaxDuration = videoClip.originStreamInfo.duration

        mMusicAudioLayer = musicAudioLayer as ClipLayer

        mAudioFadeEffect = musicAudioLayer.effects().get(400)
        mAudioFadeEffectConfig = mAudioFadeEffect!!.config

        mFadeInDuration =
            mAudioFadeEffectConfig.getIntNumberOr(AudioFadeEffect.CONFIG_FADE_IN_DURATION,0).toLong()
        mFadeOutDuration = mAudioFadeEffectConfig.getIntNumberOr(AudioFadeEffect.CONFIG_FADE_OUT_DURATION,0).toLong()

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        return true
    }

    private fun initLayer(){
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video)
        val videoClip = mVideoItem!!.mVideoClip
        val audioClip = mVideoItem!!.mAudioClip

        mMaxDuration = videoClip.originStreamInfo.duration

        val context = mEditor!!.context

        val audioLayerItem = AudioLayerItem.createAudioLayerItem(Music_Path,mEditor!!)


        val fadeEffect = Effect(context,AudioFadeEffect.TYPE_NAME)
        val fadeConfig = Config()
        fadeConfig.setNumber(AudioFadeEffect.CONFIG_FADE_IN_DURATION,0)
        fadeConfig.setNumber(AudioFadeEffect.CONFIG_FADE_OUT_DURATION,0)
        fadeEffect.setConfig(fadeConfig)


        val videoLayer = ClipLayer(context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        val audioLayer = ClipLayer(context,false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        audioLayer.setConfig(audioLayerConfig)

        if (!videoLayer.addClip(mVideoItem!!.mId.toInt(), mVideoItem!!.videoClip)) {
            TLog.e("Video clip add error ${mVideoItem!!.mId}")
        }
        if (!audioLayer.addClip(mVideoItem!!.mId.toInt(), mVideoItem!!.audioClip)) {
            TLog.e("Audio clip add error ${mVideoItem!!.mId}")
        }

        mEditor!!.videoComposition().addLayer(1,videoLayer)
        val effect = Effect(mEditor!!.context, AudioTrimEffect.TYPE_NAME)
        val effectConfig = Config()
        effectConfig.setNumber(AudioTrimEffect.CONFIG_BEGIN, 0)
        effectConfig.setNumber(AudioTrimEffect.CONFIG_END, mEditor!!.videoComposition().streamInfo.duration)
        effect.setConfig(effectConfig)

        audioLayerItem.audioLayer.effects().add(10,effect)
        audioLayerItem.audioLayer.effects().add(20,fadeEffect)

        mEditor!!.audioComposition().addLayer(1, audioLayer)
        mEditor!!.audioComposition().addLayer(400,audioLayerItem.audioLayer)


        mVideoClip = videoClip
        mAudioClip = audioClip
        mMusicAudioLayer = audioLayerItem.audioLayer as ClipLayer
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        mAudioFadeEffect = fadeEffect
        mAudioFadeEffectConfig = fadeConfig
    }


}