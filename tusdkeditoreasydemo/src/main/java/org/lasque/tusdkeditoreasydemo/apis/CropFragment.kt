/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/27$ 17:57$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.graphics.RectF
import android.os.Bundle
import android.view.View
import cn.bar.DoubleHeadedDragonBar
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.effects.CropEffect
import com.tusdk.pulse.editor.effects.VideoTransformEffect
import kotlinx.android.synthetic.main.color_adjust_fragment.lsq_editor_current_state
import kotlinx.android.synthetic.main.crop_fragment_layout.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.jetbrains.anko.support.v4.toast
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
 * @Date        2020/11/27  17:57
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class CropFragment : BaseFragment(FunctionType.Crop) {

    private var mVideoItem: VideoItem? = null

    private var mCropEffect: Effect? = null

    private var mCropConfig: Config = Config()

    private var mCropRect : RectF = RectF(0.0f,0.0f,1.0f,1.0f)

    override fun getLayoutId(): Int {
        return R.layout.crop_fragment_layout
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            if (!restoreLayer()){
                initLayer()
            }

            runOnUiThread {
                setCurrentState()

                lsq_crop_top_bottom_bar.setCallBack(object : DoubleHeadedDragonBar.DhdBarCallBack() {
                    override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                        val top = minPercentage / 100.0
                        val bottom = maxPercentage / 100.0
                        if (top == bottom){
                            lsq_crop_top_bottom_bar.minValue = (mCropRect.top * 100).toInt()
                            lsq_crop_top_bottom_bar.maxValue = (mCropRect.bottom * 100).toInt()
                            lsq_crop_top_bottom_bar.invalidate()
                            toast("上下区间不可为0")
                            return
                        }
                        mCropRect.top = top.toFloat()
                        mCropRect.bottom = bottom.toFloat()
                        setCurrentState()
                        mThreadPool?.execute {
                            val currentFrame = mPlayerContext!!.currentFrame
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mCropConfig.setNumber(CropEffect.CONFIG_TOP, top)
                            mCropConfig.setNumber(CropEffect.CONFIG_BOTTOM, bottom)
                            mCropEffect?.setConfig(mCropConfig)
                            refreshEditor()
                            playerUnlock()
                            mEditor!!.player.seekTo(currentFrame)
                            mEditor!!.player.previewFrame(currentFrame)
                        }
                    }
                })

                lsq_crop_left_right_bar.setCallBack(object : DoubleHeadedDragonBar.DhdBarCallBack() {
                    override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                        val left = minPercentage / 100.0
                        val right = maxPercentage / 100.0
                        if (left == right){
                            lsq_crop_left_right_bar.minValue = (mCropRect.left * 100).toInt()
                            lsq_crop_left_right_bar.maxValue = (mCropRect.right * 100).toInt()
                            lsq_crop_left_right_bar.invalidate()
                            toast("左右区间不可为0")
                            return
                        }
                        mCropRect.left = left.toFloat()
                        mCropRect.right = right.toFloat()
                        setCurrentState()
                        mThreadPool?.execute {
                            val currentFrame = mPlayerContext!!.currentFrame
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mCropConfig.setNumber(CropEffect.CONFIG_LEFT, left)
                            mCropConfig.setNumber(CropEffect.CONFIG_RIGHT, right)
                            mCropEffect?.setConfig(mCropConfig)
                            refreshEditor()
                            playerUnlock()
                            mEditor!!.player.seekTo(currentFrame)
                            mEditor!!.player.previewFrame(currentFrame)
                        }
                    }
                })

                lsq_crop_top_bottom_bar.post {
                    lsq_crop_top_bottom_bar.minValue = (mCropRect.top * 100).toInt()
                    lsq_crop_top_bottom_bar.maxValue = (mCropRect.bottom * 100).toInt()
                    lsq_crop_top_bottom_bar.invalidate()
                }

                lsq_crop_left_right_bar.post {
                    lsq_crop_left_right_bar.minValue = (mCropRect.left * 100).toInt()
                    lsq_crop_left_right_bar.maxValue = (mCropRect.right * 100).toInt()
                    lsq_crop_left_right_bar.invalidate()
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

        mCropEffect = videoLayer.effects().get(50)
        mCropConfig = mCropEffect!!.config
        val top = mCropConfig.getNumber(CropEffect.CONFIG_TOP)
        val left = mCropConfig.getNumber(CropEffect.CONFIG_LEFT)
        val bottom = mCropConfig.getNumber(CropEffect.CONFIG_BOTTOM)
        val right = mCropConfig.getNumber(CropEffect.CONFIG_RIGHT)

        mCropRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

        return true
    }

    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video)


        val duration = mVideoItem!!.mVideoClip.streamInfo.duration

        val context = mEditor!!.context

        val videoLayer = ClipLayer(context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        videoLayer.setConfig(videoLayerConfig)

        val mainAudioLayer = ClipLayer(context, false)
        val mainAudioConfig = Config()
        mainAudioConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        mainAudioLayer.setConfig(mainAudioConfig)

        if (!videoLayer.addClip(100, mVideoItem!!.mVideoClip)) {
            TLog.e("color adjust effect add video clip activate failed")
        }

        if (!mainAudioLayer.addClip(100, mVideoItem!!.mAudioClip)) {
            TLog.e("color adjust effect add audio clip activate failed")
        }

        mEditor!!.videoComposition().addLayer(11, videoLayer)
        mEditor!!.audioComposition().addLayer(11, mainAudioLayer)

        val cropEffect = Effect(context, CropEffect.TYPE_NAME)
        mCropConfig.setNumber(CropEffect.CONFIG_TOP, 0.0)
        mCropConfig.setNumber(CropEffect.CONFIG_LEFT, 0.0)
        mCropConfig.setNumber(CropEffect.CONFIG_BOTTOM, 1.0)
        mCropConfig.setNumber(CropEffect.CONFIG_RIGHT, 1.0)
        cropEffect.setConfig(mCropConfig)
        videoLayer.effects().add(50, cropEffect)

        mCropEffect = cropEffect
    }

    private fun setCurrentState() {
        var stateInfo = "当前视频属性 \n" +
                "上 : ${mCropRect.top} 下 : ${mCropRect.bottom} 左 : ${mCropRect.left} 右 : ${mCropRect.right}"

        lsq_editor_current_state.setText(stateInfo)
    }
}