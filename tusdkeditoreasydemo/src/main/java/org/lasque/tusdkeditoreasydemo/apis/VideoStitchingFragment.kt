/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/10/26$ 17:30$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.*
import com.tusdk.pulse.editor.clips.VideoFileClip
import kotlinx.android.synthetic.main.video_stitching_fragment.*
import kotlinx.android.synthetic.main.video_stitching_fragment.view.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.jetbrains.anko.support.v4.startActivityForResult
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumActivity
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseActivity.Companion.ALBUM_RESULT_CODE
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkeditoreasydemo.base.VideoItem.Companion.createVideoItem
import kotlin.collections.ArrayList

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/26  17:30
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class VideoStitchingFragment(functionType: FunctionType) : BaseFragment(functionType) {

    private val ALBUM_ADD_REQUEST_CODE = 2

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mClipList = ArrayList<VideoItem>()

    private var maxSize = 9

    private var mVideoAdapter : VideoItemAdapter? = null

    private var mOnClipChangeListener: VideoItemAdapter.OnClipChangeListener = object : VideoItemAdapter.OnClipChangeListener {
        override fun onSwap(item0: VideoItem, item1: VideoItem) {
            mThreadPool!!.execute {
                mOnPlayerStateUpdateListener?.onPlayerPause()
                playerLock()
                mVideoLayer?.swapClips(item0.mId.toInt(), item1.mId.toInt())
                mAudioLayer?.swapClips(item0.mId.toInt(),item1.mId.toInt())
                val id = item0.mId
                item0.mId = item1.mId
                item1.mId = id
                refreshEditor()
                playerUnlock()
                mEditor!!.player.previewFrame(0)
            }
        }

        override fun onRemove(item0: VideoItem,pos : Int) {
            mThreadPool!!.execute {
                mOnPlayerStateUpdateListener?.onPlayerPause()
                playerLock()
                mVideoLayer?.deleteClip(item0.mId.toInt())
                mAudioLayer?.deleteClip(item0.mId.toInt())

                mOnPlayerStateUpdateListener?.onDurationUpdate()
                refreshEditor()
                playerUnlock()
                if (mClipList.size <= 1 || pos == 0){
                    mEditor!!.player.previewFrame(0)
                } else if (pos != mClipList.size - 1){
                    val targetFrame = getTargetFrame(mClipList[pos - 1]) + 10
                    mEditor!!.player.previewFrame(targetFrame)
                } else if (pos == mClipList.size - 1){
                    val targetFrame = getTargetFrame(mClipList[pos - 1])
                    mEditor!!.player.previewFrame(targetFrame)
                }
            }
            lsq_video_add.visibility = View.VISIBLE
        }

    }

    private fun getTargetFrame(item : VideoItem) : Long{
        var result = 0L
        for (i in mClipList){
            if (i == item) break
            result += i.mVideoClip.streamInfo.duration
        }
        return result
    }



    override fun getLayoutId(): Int {
        return R.layout.video_stitching_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {

            if (!restoreLayer()){
                initLayer()
            }

            runOnUiThread {
                mVideoAdapter = VideoItemAdapter(mClipList,requireContext())
                mVideoAdapter!!.setOnClipChangeListener(mOnClipChangeListener)
                val layoutManager = LinearLayoutManager(context)
                layoutManager.orientation = LinearLayoutManager.VERTICAL
                view.lsq_video_list.layoutManager = layoutManager
                view.lsq_video_list.adapter = mVideoAdapter

                lsq_video_add.setOnClickListener {
                    val isOnlyVideo = mType == FunctionType.VideoStitching
                    startActivityForResult<AlbumActivity>(ALBUM_ADD_REQUEST_CODE,"maxSize" to maxSize - mClipList.size,"onlyImage" to false,"onlyVideo" to isOnlyVideo)
                }

                if (mClipList.size >= maxSize){
                    lsq_video_add.visibility = View.GONE
                } else {
                    lsq_video_add.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ALBUM_ADD_REQUEST_CODE && resultCode == ALBUM_RESULT_CODE){
            var albumBundle = data!!.getBundleExtra("select")
            var albumList = albumBundle?.getSerializable("select") as ArrayList<AlbumInfo>
            mThreadPool?.execute {
                addVideolist(albumList!!)
                runOnUiThread {
                    if (mClipList.size >= maxSize){
                        lsq_video_add.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun restoreLayer(): Boolean{
        if (mEditor!!.videoComposition().allLayers.isEmpty()){
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor!!.audioComposition().allLayers[11] as ClipLayer

        val videoClipMaps = videoLayer.allClips

        for (itemId in videoClipMaps.keys){
            val videoClip = videoLayer.getClip(itemId)
            val audioClip = audioLayer.getClip(itemId)
            val path = videoClip.config.getString(VideoFileClip.CONFIG_PATH)
            mClipList.add(VideoItem(path, itemId.toLong(),videoClip, audioClip))
        }

        VideoItem.plusClipCount(videoClipMaps.size)

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        return true

    }

    private fun initLayer() {
        for (item in mVideoList!!){
             mClipList.add(createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video,item.audioPath))
        }

        val videoLayer = ClipLayer(mEditor!!.context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        val audioLayer = ClipLayer(mEditor!!.context,false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        audioLayer.setConfig(audioLayerConfig)

        for (item in mClipList){
            if (!videoLayer.addClip(item.mId.toInt(),item.videoClip)){
                TLog.e("Video clip add error ${item.mId}")
            }
            if (!audioLayer.addClip(item.mId.toInt(),item.audioClip)){
                TLog.e("Audio clip add error ${item.mId}")
            }
        }

        if (!videoLayer.activate() || !audioLayer.activate()) {
            return
        }

        mEditor!!.videoComposition().addLayer(11,videoLayer)
        mEditor!!.audioComposition().addLayer(11,audioLayer)

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer


    }



    public fun addVideolist(list: MutableList<AlbumInfo>){
        mOnPlayerStateUpdateListener?.onPlayerPause()
        playerLock()
        mVideoList!!.addAll(list)
        for (item in list){
            val clip = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video,item.audioPath)
            mVideoLayer!!.addClip(clip.mId.toInt(),clip.mVideoClip)
            mAudioLayer!!.addClip(clip.mId.toInt(),clip.mAudioClip)
            mClipList.add(clip)
        }
        refreshEditor()
        playerUnlock()
        mOnPlayerStateUpdateListener?.onDurationUpdate()
        runOnUiThread {
            mVideoAdapter?.notifyDataSetChanged()
        }
    }
}

