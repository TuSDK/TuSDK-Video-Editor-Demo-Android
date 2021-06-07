/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/12/3$ 10:35$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.effects.VideoTransformEffect
import kotlinx.android.synthetic.main.transform_fragment_layout.*
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
 * @Date        2020/12/3  10:35
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class TransformFragment : BaseFragment(FunctionType.Transform) {

    private var mVideoItem : VideoItem? = null

    private var mSpinEffect : Effect? = null

    private var mSpinConfig = Config()

    private var mCurrentSpinMode = VideoTransformEffect.MODE_None

    private var mCurrentVMode = VideoTransformEffect.MODE_None

    private var mCurrentHMode = VideoTransformEffect.MODE_None

    private var mTransfer = VideoTransformEffect.ModeTransfer(VideoTransformEffect.MODE_None)

    override fun getLayoutId(): Int {
        return R.layout.transform_fragment_layout
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            if (!restoreLayer()){
                initLayer()
            }

            runOnUiThread {

                lsq_up.setOnClickListener {
                    mThreadPool?.execute {
                        val currentFrame = mPlayerContext!!.currentFrame
                        mOnPlayerStateUpdateListener?.onPlayerPause()
                        nextSpin()
                        playerLock()
                        mSpinConfig.setString(VideoTransformEffect.CONFIG_MODE,mCurrentSpinMode)
                        mSpinEffect?.setConfig(mSpinConfig)
                        refreshEditor()
                        playerUnlock()
                        mEditor!!.player.previewFrame(currentFrame)
                    }
                }
                lsq_v_flip.setOnClickListener {
                    mThreadPool?.execute {
                        mCurrentVMode = when(mCurrentVMode){
                            VideoTransformEffect.MODE_None->{
                                VideoTransformEffect.MODE_VFlip
                            }
                            else -> VideoTransformEffect.MODE_None
                        }
                        val currentFrame = mPlayerContext!!.currentFrame
                        mOnPlayerStateUpdateListener?.onPlayerPause()
                        playerLock()
                        mCurrentSpinMode = mTransfer.ApplyFlip()
                        mSpinConfig.setString(VideoTransformEffect.CONFIG_MODE,mCurrentSpinMode)
                        mSpinEffect?.setConfig(mSpinConfig)
                        refreshEditor()
                        playerUnlock()
                        mEditor!!.player.previewFrame(currentFrame)
                    }
                }
                lsq_h_flip.setOnClickListener {
                    mThreadPool?.execute {
                        mCurrentHMode = when(mCurrentHMode){
                            VideoTransformEffect.MODE_None->{
                                VideoTransformEffect.MODE_HFlip
                            }
                            else -> VideoTransformEffect.MODE_None
                        }
                        val currentFrame = mPlayerContext!!.currentFrame
                        mOnPlayerStateUpdateListener?.onPlayerPause()
                        playerLock()
                        mCurrentSpinMode = mTransfer.ApplyMirror()
                        mSpinConfig.setString(VideoTransformEffect.CONFIG_MODE,mCurrentSpinMode)
                        mSpinEffect?.setConfig(mSpinConfig)
                        refreshEditor()
                        playerUnlock()
                        mEditor!!.player.previewFrame(currentFrame)
                    }

                }
            }
        }
    }

    private fun nextSpin(){
        mCurrentSpinMode = mTransfer.ApplyRotate(true)
    }

    private fun restoreLayer(): Boolean{
        if (mEditor!!.videoComposition().allLayers.isEmpty()) {
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor!!.audioComposition().allLayers[11] as ClipLayer

        val videoClipMap = videoLayer.allClips
        val videoClip = videoLayer.getClip(videoClipMap.keys.first())

        mSpinEffect = videoClip.effects().get(152)
        mSpinConfig = mSpinEffect!!.config

        mCurrentSpinMode = mSpinConfig.getString(VideoTransformEffect.CONFIG_MODE)

        mTransfer = VideoTransformEffect.ModeTransfer(mCurrentSpinMode)
        return true
    }

    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video,item.audioPath)

        val duration = mVideoItem!!.mVideoClip.streamInfo.duration

        val context = mEditor!!.context

        val videoLayer = ClipLayer(context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        val mainAudioLayer = ClipLayer(context,false)
        val mainAudioConfig = Config()
        mainAudioConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        mainAudioLayer.setConfig(mainAudioConfig)

        if (!videoLayer.addClip(100,mVideoItem!!.mVideoClip)){
            TLog.e("color adjust effect add video clip activate failed")
        }

        if (!mainAudioLayer.addClip(100,mVideoItem!!.mAudioClip)){
            TLog.e("color adjust effect add audio clip activate failed")
        }

        mEditor!!.videoComposition().addLayer(11,videoLayer)
        mEditor!!.audioComposition().addLayer(11,mainAudioLayer)

        val spinEffect = Effect(context, VideoTransformEffect.TYPE_NAME)
        mSpinConfig.setString(VideoTransformEffect.CONFIG_MODE, VideoTransformEffect.MODE_None)
        spinEffect.setConfig(mSpinConfig)
        mVideoItem!!.mVideoClip.effects().add(152,spinEffect)

        mSpinEffect = spinEffect
    }

}