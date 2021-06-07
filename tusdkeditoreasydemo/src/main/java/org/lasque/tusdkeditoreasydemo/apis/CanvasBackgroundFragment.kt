/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/27$ 18:58$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.CanvasResizeEffect
import kotlinx.android.synthetic.main.canvas_background_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumActivity
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseActivity
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkeditoreasydemo.base.views.ColorView
import org.lasque.tusdkpulse.core.utils.TLog

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/27  18:58
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class CanvasBackgroundFragment : BaseFragment(FunctionType.CanvasBackgroundType) {

    private var mVideoItem : VideoItem? = null

    private var mCanvasEffect : Effect? = null

    private var mCanvasProperty : CanvasResizeEffect.PropertyBuilder = CanvasResizeEffect.PropertyBuilder()

    private var isRestore = false

    private val IMAGE_BACKGROUND_REQUEST_CODE = 150

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_BACKGROUND_REQUEST_CODE && resultCode == BaseActivity.ALBUM_RESULT_CODE){
            var albumBundle = data!!.getBundleExtra("select")
            var albumList = albumBundle?.getSerializable("select") as ArrayList<AlbumInfo>
            mThreadPool?.execute {
                mCanvasProperty.holder.type = CanvasResizeEffect.BackgroundType.IMAGE
                mCanvasProperty.holder.image = albumList[0].path
                mCanvasEffect?.setProperty(CanvasResizeEffect.PROP_PARAM,mCanvasProperty.makeProperty())
                mOnPlayerStateUpdateListener?.onRefreshFrame()
            }
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.canvas_background_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            isRestore = restoreLayer()
            if (!isRestore){
                initLayer()
            }

            runOnUiThread {
                showOrHideColorView(false)
                showOrHideBlueView(false)
                lsq_color_background.setOnClickListener {
                    showOrHideBlueView(false)
                    showOrHideColorView(true)
                    mThreadPool?.execute {
                        mCanvasProperty.holder.type = CanvasResizeEffect.BackgroundType.COLOR
                        mCanvasEffect?.setProperty(CanvasResizeEffect.PROP_PARAM,mCanvasProperty.makeProperty())
                        mOnPlayerStateUpdateListener?.onRefreshFrame()
                    }

                    lsq_canvas_background_color_bar.findColorInt(mCanvasProperty.holder.color)
                }
                lsq_blue_background.setOnClickListener {
                    showOrHideColorView(false)
                    showOrHideBlueView(true)
                    mThreadPool?.execute {
                        mCanvasProperty.holder.type = CanvasResizeEffect.BackgroundType.BLUR
                        mCanvasEffect?.setProperty(CanvasResizeEffect.PROP_PARAM,mCanvasProperty.makeProperty())
                        mOnPlayerStateUpdateListener?.onRefreshFrame()
                    }
                }

                lsq_image_background.setOnClickListener {
                    showOrHideColorView(false)
                    showOrHideBlueView(true)

                    openAlbum(1,true,false,IMAGE_BACKGROUND_REQUEST_CODE)
                }

                lsq_canvas_background_color_bar.setOnColorChangeListener(object : ColorView.OnColorChangeListener{
                    override fun changeColor(colorId: Int) {
                        TLog.e("current select color ${colorId}")
                        mThreadPool?.execute {
                            mCanvasProperty.holder.color = colorId
                            mCanvasEffect?.setProperty(CanvasResizeEffect.PROP_PARAM,mCanvasProperty.makeProperty())
                            mOnPlayerStateUpdateListener?.onRefreshFrame()
                        }
                    }

                    override fun changePosition(percent: Float) {
                    }

                })

                lsq_blue_mix_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        if (!p2) return
                        mThreadPool?.execute {
                            val mix = p1 / 100.0
                            mCanvasProperty.holder.blurStrength = mix
                            mCanvasEffect?.setProperty(CanvasResizeEffect.PROP_PARAM,mCanvasProperty.makeProperty())
                            mOnPlayerStateUpdateListener?.onRefreshFrame()
                        }
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                    }

                })

                if (isRestore){
                    if (mCanvasProperty.holder.type == CanvasResizeEffect.BackgroundType.COLOR){
                        showOrHideBlueView(false)
                        showOrHideColorView(true)
                        TLog.e("current color ${mCanvasProperty.holder.color}")
                        lsq_canvas_background_color_bar.findColorInt(mCanvasProperty.holder.color)
                    } else if (mCanvasProperty.holder.type == CanvasResizeEffect.BackgroundType.BLUR || mCanvasProperty.holder.type == CanvasResizeEffect.BackgroundType.IMAGE){
                        showOrHideColorView(false)
                        showOrHideBlueView(true)
                        lsq_blue_mix_bar.progress = (mCanvasProperty.holder.blurStrength * 100).toInt()
                    }
                }
            }
        }
    }

    private fun showOrHideColorView(v : Boolean){
        var isV = if (v){View.VISIBLE} else {View.INVISIBLE}
        if (v){
            lsq_canvas_background_color_title.setText("颜色")
        }
        lsq_canvas_background_color_bar.visibility = isV
        lsq_canvas_background_color_title.visibility = isV
    }

    private fun showOrHideBlueView(v : Boolean){
        var isV = if (v){View.VISIBLE} else {View.INVISIBLE}
        if (v){
            lsq_canvas_background_color_title.setText("模糊")
        }
        lsq_blue_mix_bar.visibility = isV
        lsq_canvas_background_color_title.visibility = isV
    }



    private fun restoreLayer(): Boolean{
        if (mEditor!!.videoComposition().allLayers.isEmpty()) {
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor!!.audioComposition().allLayers[11] as ClipLayer

        val videoClip = videoLayer.getClip(100)

        mCanvasEffect = videoClip.effects().get(100)

        val canvasHolder = CanvasResizeEffect.PropertyHolder(mCanvasEffect!!.getProperty(CanvasResizeEffect.PROP_PARAM))
        mCanvasProperty.holder = canvasHolder

        return true

    }

    private fun initLayer() {
        val item = mVideoList!![0]

        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,false,item.type == AlbumItemType.Video,item.audioPath)

        var videoLayer = ClipLayer(mEditor!!.context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        videoLayer.setConfig(videoLayerConfig)

        if (!videoLayer.addClip(100, mVideoItem!!.mVideoClip)) {
            return
        }

        var audioLayer = ClipLayer(mEditor!!.context, false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)

        audioLayer.setConfig(audioLayerConfig)
        if (!audioLayer.addClip(100, mVideoItem!!.mAudioClip)) {
            return
        }

        val resizeConfig = Config()
        val resizeEffect = Effect(mEditor!!.context, CanvasResizeEffect.TYPE_NAME)
        resizeEffect.setConfig(resizeConfig)
        mVideoItem!!.mVideoClip.effects().add(100, resizeEffect)

        mEditor!!.videoComposition().addLayer(11, videoLayer)
        mEditor!!.audioComposition().addLayer(11, audioLayer)

        mCanvasEffect = resizeEffect

        mCanvasProperty.holder.blurStrength = 1.0
    }
}