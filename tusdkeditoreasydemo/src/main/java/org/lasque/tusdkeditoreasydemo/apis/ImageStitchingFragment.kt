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
import com.tusdk.pulse.editor.clips.SilenceClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.AudioTrimEffect
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
import java.util.*
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
public class ImageStitchingFragment : BaseFragment(FunctionType.ImageStitching) {

    private val ALBUM_ADD_REQUEST_CODE = 2

    private var mVideoLayer: ClipLayer? = null

    private var mClipList = ArrayList<VideoItem>()

    private var mVideoAdapter : VideoItemAdapter? = null

    private var mAudioEffect : Effect? = null

    private var mAudioEffectConfig : Config? = null

    private var mOnClipChangeListener: VideoItemAdapter.OnClipChangeListener = object : VideoItemAdapter.OnClipChangeListener {
        override fun onSwap(item0: VideoItem, item1: VideoItem) {
            mThreadPool!!.execute {
                mOnPlayerStateUpdateListener?.onPlayerPause()
                playerLock()
                mVideoLayer?.swapClips(item0.mId.toInt(), item1.mId.toInt())
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
                mOnPlayerStateUpdateListener?.onDurationUpdate()
                refreshEditor()
                mAudioEffectConfig?.setNumber(AudioTrimEffect.CONFIG_END,mEditor!!.videoComposition().streamInfo.duration)
                mAudioEffect?.setConfig(mAudioEffectConfig)
                refreshEditor()
                playerUnlock()
                mEditor!!.player.previewFrame(0)
            }
            lsq_video_add.visibility = View.VISIBLE
        }

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
                    startActivityForResult<AlbumActivity>(ALBUM_ADD_REQUEST_CODE,"maxSize" to -1,"onlyImage" to true,"onlyVideo" to false)
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
            }
        }
    }

    private fun restoreLayer() : Boolean{
        if (mEditor!!.videoComposition().allLayers.isEmpty()){
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer

        val videoClipMaps = videoLayer.allClips

        for (itemId in videoClipMaps.keys){
            val videoClip = videoLayer.getClip(itemId)
            val audioClip = Clip(mEditor!!.context, SilenceClip.TYPE_NAME)
            val audioConfig = Config()
            audioConfig.setNumber(SilenceClip.CONFIG_DURATION,3000)
            audioClip.setConfig(audioConfig)
            val path = videoClip.config.getString(VideoFileClip.CONFIG_PATH)
            mClipList.add(VideoItem(path, itemId.toLong(),videoClip, audioClip))
        }

        mVideoLayer = videoLayer

        VideoItem.plusClipCount(videoClipMaps.size)


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

        for (item in mClipList){
            if (!videoLayer.addClip(item.mId.toInt(),item.videoClip)){
                TLog.e("Video clip add error ${item.mId}")
            }
        }

        if (!videoLayer.activate()) {
            return
        }

        mEditor!!.videoComposition().addLayer(11,videoLayer)

        TLog.e("duration ${mEditor!!.videoComposition().streamInfo.duration}")

        val effect = Effect(mEditor!!.context,AudioTrimEffect.TYPE_NAME)
        val effectConfig = Config()
        effectConfig.setNumber(AudioTrimEffect.CONFIG_BEGIN,0)
        effectConfig.setNumber(AudioTrimEffect.CONFIG_END,mEditor!!.videoComposition().streamInfo.duration)
        effect.setConfig(effectConfig)
        mEditor!!.audioComposition().effects().add(10,effect)

        mAudioEffect = effect
        mAudioEffectConfig = effectConfig

        mVideoLayer = videoLayer

    }



    public fun addVideolist(list: MutableList<AlbumInfo>){
        mVideoList!!.addAll(list)
        playerLock()
        for (item in list){
            val clip = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video,item.audioPath)
            mVideoLayer!!.addClip(clip.mId.toInt(),clip.mVideoClip)
            mClipList.add(clip)
        }
        mAudioEffectConfig?.setNumber(AudioTrimEffect.CONFIG_END,mEditor!!.videoComposition().streamInfo.duration)
        mAudioEffect?.setConfig(mAudioEffectConfig)
        refreshEditor()
        playerUnlock()
        mEditor!!.player.previewFrame(0)
        mOnPlayerStateUpdateListener?.onDurationUpdate()
        runOnUiThread {
            mVideoAdapter?.notifyDataSetChanged()
        }
    }
}

