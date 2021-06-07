/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/10$ 15:23$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.tusdk.pulse.Config
import com.tusdk.pulse.VideoStreamInfo
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.VideoEditor
import com.tusdk.pulse.editor.effects.CanvasResizeEffect
import kotlinx.android.synthetic.main.ratio_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.OnItemClickListener
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import java.io.File

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/10  15:23
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class RatioFragment : BaseFragment(FunctionType.VideoRatio) {

    private var mClipList = ArrayList<VideoItem>()

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mLayerRatioEffect: Effect? = null

    private var mLayerRatioConfig = Config()

    private var isRestore = false

    private var restoreVideoRatio = VideoRatio.Ratio_1_1

    override fun getLayoutId(): Int {
        return R.layout.ratio_fragment
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

        val restoreWidth = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo).width
        val restoreHeight = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo).height

        val ratioList = VideoRatio.values().toMutableList()
        for (item in ratioList){
            val rWH = restoreWidth.toDouble() / restoreHeight
            val itemWH = item.width.toDouble() / item.height
            if (rWH == itemWH){
                restoreVideoRatio = item
                break
            }
        }

        return true
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            isRestore = restoreLayer()
            if (!isRestore){
                initLayer()
            }

            runOnUiThread {
                val ratioList = VideoRatio.values().toMutableList()
                val ratioAdapter = RatioAdapter(ratioList, requireContext())
                ratioAdapter.setOnItemClickListener(object : OnItemClickListener<VideoRatio, RatioAdapter.RatioViewHolder> {
                    override fun onItemClick(pos: Int, holder: RatioAdapter.RatioViewHolder, item: VideoRatio) {
                        mThreadPool?.execute {
                            val currentFrame = mPlayerContext!!.currentFrame
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            val height = 800 / item.width * item.height
                            val config = VideoEditor.OpenConfig()
                            config.width = 800
                            config.height = height
                            mEditor!!.update(config)
                            refreshEditor()
                            playerUnlock()
                            mEditor!!.player.previewFrame(currentFrame)
                        }
                        ratioAdapter.setCurrentPosition(pos)
                    }
                })
                val layoutManager = LinearLayoutManager(context)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                lsq_ratio_list.layoutManager = layoutManager
                lsq_ratio_list.adapter = ratioAdapter

                val index = ratioList.indexOf(restoreVideoRatio)
                ratioAdapter.setCurrentPosition(index)
            }
        }
    }

    private fun initLayer() {
        for (item in mVideoList!!) {
            mClipList.add(VideoItem.createVideoItem(item.path, mEditor!!,false,item.type == AlbumItemType.Video,item.audioPath))
        }

        val videoLayer = ClipLayer(mEditor!!.context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        videoLayer.setConfig(videoLayerConfig)

        val audioLayer = ClipLayer(mEditor!!.context, false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        audioLayer.setConfig(audioLayerConfig)

        for (item in mClipList) {
            if (!videoLayer.addClip(item.mId.toInt(), item.videoClip)) {
                TLog.e("Video clip add error ${item.mId}")
            }
            if (item.audioClip!= null){
                if (!audioLayer.addClip(item.mId.toInt(), item.audioClip)) {
                    TLog.e("Audio clip add error ${item.mId}")
                }
            }
        }

        if (!videoLayer.activate()) {
            return
        }
        mEditor!!.videoComposition().addLayer(11, videoLayer)
        if (audioLayer.getAllClips().size > 0){
            mEditor!!.audioComposition().addLayer(11, audioLayer)
        }

        mLayerRatioEffect = Effect(mEditor!!.context, CanvasResizeEffect.TYPE_NAME)
        mLayerRatioEffect?.setConfig(mLayerRatioConfig)
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
    }
}