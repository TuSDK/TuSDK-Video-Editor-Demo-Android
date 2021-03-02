/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/12/9$ 11:27$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import kotlinx.android.synthetic.main.include_video_segmentation_bar_layout.*
import kotlinx.android.synthetic.main.include_video_segmentation_list_layout.*
import kotlinx.android.synthetic.main.video_segmentation_fragment_layout.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.jetbrains.anko.support.v4.toast
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import java.util.concurrent.Callable

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/12/9  11:27
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class VideoSegmentationFragment : BaseFragment(FunctionType.VideoSegmentation) {

    private var mVideoItem: VideoItem? = null

    private var mSubVideoItem: VideoItem? = null

    private var mVideoTrimConfig = Config()

    private var mAudioTrimConfig = Config()

    private var mVideoClip: Clip? = null

    private var mAudioClip: Clip? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mVideoTrimEffect: Effect? = null

    private var mAudioTrimEffect: Effect? = null

    private var mSegmentationPos = 0L

    private var mMaxDuration = 0L

    private var isRestore = false

    override fun getLayoutId(): Int {
        return R.layout.video_segmentation_fragment_layout
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            isRestore = restoreLayer()
            if (!isRestore){
                initLayer()
            }
            runOnUiThread {
                if (isRestore){
                    initVideoItemList()
                    lsq_include_bar.visibility = View.INVISIBLE
                    lsq_include_list.visibility = View.VISIBLE
                    mOnPlayerStateUpdateListener?.onDurationUpdate()
                } else {
                    initSegmentationView()
                }
            }
        }
    }

    private fun initSegmentationView() {
        lsq_video_segmentation_bar.max = mMaxDuration.toInt()
        lsq_video_segmentation_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (!p2) return
                mSegmentationPos = p1.toLong()
                setCurrentState()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

        })

        lsq_video_segmentation.setOnClickListener {
            if (mSegmentationPos == 0L || mSegmentationPos == mMaxDuration){
                toast("视频分割位置不可选择视频起点或者视频终点")
                return@setOnClickListener
            }

            mThreadPool?.execute {
                mOnPlayerStateUpdateListener?.onPlayerPause()
                playerLock()
                val videoConfig = mVideoItem?.mVideoClip!!.config
                videoConfig.setNumber(VideoFileClip.CONFIG_TRIM_START, 0)
                videoConfig.setNumber(VideoFileClip.CONFIG_TRIM_DURATION, mSegmentationPos)
                mVideoItem?.mVideoClip?.setConfig(videoConfig)

                val audioConfig = mVideoItem?.mAudioClip!!.config
                audioConfig.setNumber(AudioFileClip.CONFIG_TRIM_START, 0)
                audioConfig.setNumber(AudioFileClip.CONFIG_TRIM_DURATION, mSegmentationPos)
                mVideoItem?.mAudioClip?.setConfig(audioConfig)
                initSubVideoItem()
                playerUnlock()
                mEditor!!.player.previewFrame(0)
                runOnUiThread {
                    initVideoItemList()
                    lsq_include_bar.visibility = View.INVISIBLE
                    lsq_include_list.visibility = View.VISIBLE
                    mOnPlayerStateUpdateListener?.onDurationUpdate()
                }
            }
        }
    }

    private fun initVideoItemList() {
        var videoItemList = ArrayList<VideoItem>()
        if (mVideoItem != null) videoItemList.add(mVideoItem!!)
        if (mSubVideoItem != null) videoItemList.add(mSubVideoItem!!)

        val itemAdapter = VideoItemAdapter(videoItemList, requireContext())
        itemAdapter.setOnClipChangeListener(object : VideoItemAdapter.OnClipChangeListener {
            override fun onSwap(item0: VideoItem, item1: VideoItem) {
                 val res = mThreadPool!!.submit(Callable<Boolean> {
                    mOnPlayerStateUpdateListener?.onPlayerPause()
                    playerLock()
                    mVideoLayer?.swapClips(item0.mId.toInt(), item1.mId.toInt())
                    mAudioLayer?.swapClips(item0.mId.toInt(), item1.mId.toInt())
                    val id = item0.mId
                    item0.mId = item1.mId
                    item1.mId = id
                    refreshEditor()
                    playerUnlock()
                    mEditor!!.player.previewFrame(0)
                })

                res.get()
            }

            override fun onRemove(item0: VideoItem, pos: Int) {
                val res = mThreadPool!!.submit(Callable<Boolean> {
                    mOnPlayerStateUpdateListener?.onPlayerPause()
                    playerLock()
                    val ret = mVideoLayer?.deleteClip(item0.mId.toInt())
                    mAudioLayer?.deleteClip(item0.mId.toInt())
                    refreshEditor()
                    playerUnlock()
                    mEditor!!.player.previewFrame(0)
                    mOnPlayerStateUpdateListener?.onDurationUpdate()
                    ret
                })
                res.get()
            }

        })
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        lsq_video_list.layoutManager = layoutManager
        lsq_video_list.adapter = itemAdapter
    }

    private fun restoreLayer(): Boolean {
        if (mEditor!!.videoComposition().allLayers.isEmpty()){
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor!!.audioComposition().allLayers[11] as ClipLayer

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        val videoClipMaps = videoLayer.allClips
        val audioClipMaps = audioLayer.allClips

        val mainClipId = videoClipMaps.keys.first()
        val mainVideoClip = videoLayer.getClip(mainClipId)
        val mainAudioClip = audioLayer.getClip(mainClipId)

        val videoPath = mainVideoClip.config.getString(VideoFileClip.CONFIG_PATH)
        val mainStart = mainVideoClip.config.getIntNumber(VideoFileClip.CONFIG_TRIM_START)
        val maindura = mainVideoClip.config.getIntNumber(VideoFileClip.CONFIG_TRIM_DURATION)


        mVideoItem = VideoItem(videoPath, mainClipId.toLong(),mainVideoClip,mainAudioClip)

        val subClipId = videoClipMaps.keys.last()
        if (mainClipId != subClipId){
            val subVideoClip = videoLayer.getClip(subClipId)
            val subAudioClip = audioLayer.getClip(subClipId)

            val subStart = subVideoClip.config.getIntNumber(VideoFileClip.CONFIG_TRIM_START)
            val subdura = subVideoClip.config.getIntNumber(VideoFileClip.CONFIG_TRIM_DURATION)

            TLog.e("id $subClipId start $subStart dura $subdura")


            mSubVideoItem = VideoItem(videoPath,subClipId.toLong(),subVideoClip,subAudioClip)
        }
        return true
    }


    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path, mEditor!!, true, item.type == AlbumItemType.Video)

        val videoClip = mVideoItem!!.mVideoClip
        val audioClip = mVideoItem!!.mAudioClip

        val duration = videoClip.streamInfo.duration
        mMaxDuration = duration

        var videoLayer = ClipLayer(mEditor!!.context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        videoLayer.setConfig(videoLayerConfig)

        if (!videoLayer.addClip(mVideoItem!!.mId.toInt(), videoClip)) {
            return
        }

        var audioLayer = ClipLayer(mEditor!!.context, false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        audioLayer.setConfig(audioLayerConfig)
        if (!audioLayer.addClip(mVideoItem!!.mId.toInt(), audioClip)) {
            return
        }

        mEditor!!.videoComposition().addLayer(11, videoLayer)
        mEditor!!.audioComposition().addLayer(11, audioLayer)

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

    }

    private fun initSubVideoItem() {
        mSubVideoItem = VideoItem.copy(mEditor!!, mVideoItem!!)
        val subVideoClip = mSubVideoItem!!.mVideoClip
        val duration = mMaxDuration
        val subAudioClip = mSubVideoItem!!.mAudioClip
        if (!mVideoLayer!!.addClip(mSubVideoItem!!.mId.toInt(), subVideoClip)) {
            return
        }

        val videoConfig = subVideoClip.config
        videoConfig.setNumber(VideoFileClip.CONFIG_TRIM_START, mSegmentationPos)
        videoConfig.setNumber(VideoFileClip.CONFIG_TRIM_DURATION, duration - mSegmentationPos)
        subVideoClip.setConfig(videoConfig)

        if (!mAudioLayer!!.addClip(mSubVideoItem!!.mId.toInt(), subAudioClip)) {

        }
        val audioConfig = subAudioClip.config
        audioConfig.setNumber(AudioFileClip.CONFIG_TRIM_START, mSegmentationPos)
        audioConfig.setNumber(AudioFileClip.CONFIG_TRIM_DURATION, duration - mSegmentationPos)
        subAudioClip.setConfig(audioConfig)
        refreshEditor()
    }

    private fun setCurrentState(){
        val currentVideoHour = mSegmentationPos / 3600000
        val currentVideoMinute = (mSegmentationPos % 3600000) / 60000

        val currentVideoSecond = (mSegmentationPos % 60000 / 1000)

        lsq_segmentation_state.setText("当前分割位置 : ${currentVideoHour}:${currentVideoMinute}:${currentVideoSecond}")
    }
}