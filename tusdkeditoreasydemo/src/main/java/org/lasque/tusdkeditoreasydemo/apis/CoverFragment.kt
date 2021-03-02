/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/20$ 17:48$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.tusdk.pulse.Config
import com.tusdk.pulse.ThumbnailMaker
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Layer
import kotlinx.android.synthetic.main.cover_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
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
 * @Date        2020/11/20  17:48
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class CoverFragment : BaseFragment(FunctionType.Cover) {

    private var mVideoItem : VideoItem? = null

    private var mCurrentDuration = 0

    private var mThumbnailMaker : ThumbnailMaker? = null

    override fun getLayoutId(): Int {
        return R.layout.cover_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            initLayer()
        }

        lsq_start_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (!p2) return
                mCurrentDuration = p1
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                Glide.with(context!!).load(mThumbnailMaker!!.readImage(mCurrentDuration.toLong())).into(lsq_cover_image)
            }
        })
    }

    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video)

        mThumbnailMaker = ThumbnailMaker(mVideoItem!!.path, 400)

        val duration = mVideoItem!!.mVideoClip.streamInfo.duration

        runOnUiThread {
            lsq_start_bar.max = duration.toInt()
        }

        val videoLayer = ClipLayer(mEditor!!.context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        val audioLayer = ClipLayer(mEditor!!.context,false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        audioLayer.setConfig(audioLayerConfig)

        if (!videoLayer.addClip(mVideoItem!!.mId.toInt(),mVideoItem!!.videoClip)){

        }

        if (!audioLayer.addClip(mVideoItem!!.mId.toInt(),mVideoItem!!.audioClip)){

        }
        if (!videoLayer.activate() || !audioLayer.activate()) {
            return
        }

        mEditor!!.videoComposition().addLayer(11,videoLayer)
        mEditor!!.audioComposition().addLayer(11,audioLayer)
    }
}