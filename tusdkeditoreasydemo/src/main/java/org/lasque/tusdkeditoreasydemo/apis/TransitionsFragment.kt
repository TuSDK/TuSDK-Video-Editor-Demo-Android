/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/9$ 17:22$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.SeekBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.VideoFileClip
import kotlinx.android.synthetic.main.transitions_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.OnItemClickListener
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkeditoreasydemo.utils.Constants
import java.lang.Long.max

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/9  17:22
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class TransitionsFragment : BaseFragment() {

    private var mClipList = ArrayList<VideoItem>()

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var isRestore = false

    private var mCurrentTransitionsDuration = 1000L

    private var mCurrentVideoTransition : ClipLayer.Transition? = null

    private var mCurrentAudioTransition : ClipLayer.Transition? = null

    override fun getLayoutId(): Int {
        return R.layout.transitions_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            isRestore = restoreLayer()
            if (!isRestore){
                initLayer()
            }

            runOnUiThread {
                setCurrentState()
                val transitionsGroups = Constants.TRANSITIONS_CODES

                val transitionsAdapter = TransitionsAdapter(transitionsGroups.toMutableList(), requireContext())
                transitionsAdapter.setOnItemClickListener(object : OnItemClickListener<String, TransitionsAdapter.TransitionsViewHolder> {
                    override fun onItemClick(pos: Int, holder: TransitionsAdapter.TransitionsViewHolder, item: String) {
                        mThreadPool?.execute {
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            if (TextUtils.isEmpty(item)){
                                mVideoLayer!!.unsetTransition(mClipList[1].mId.toInt())
                                mAudioLayer!!.unsetTransition(mClipList[1].mId.toInt())
                            } else {
                                val transition = ClipLayer.Transition(mCurrentTransitionsDuration,item)
                                transition.duration = Math.max(mCurrentTransitionsDuration - 100 , 0 )
                                //转场加入到后一个clip上面
                                val boolean = mVideoLayer!!.setTransition(mClipList[1].mId.toInt(), transition)
                                val ttt = mVideoLayer!!.getTransition(mClipList[1].mId.toInt())
                                val audioTransitions = ClipLayer.Transition()
                                audioTransitions.duration = Math.max(mCurrentTransitionsDuration - 100 , 0 )
                                mAudioLayer!!.setTransition(mClipList[1].mId.toInt(), audioTransitions)
                                mCurrentVideoTransition = transition
                                mCurrentAudioTransition = audioTransitions
                            }
                            refreshEditor()
                            playerUnlock()
                            var targetPos = mClipList[0].videoClip.streamInfo.duration
                            mEditor!!.player.seekTo(max(targetPos - 3000,0))
                            mOnPlayerStateUpdateListener?.onPlayerPlay()

                        }
                        transitionsAdapter.setCurrentPosition(pos)
                    }
                })
                val layoutManager = LinearLayoutManager(context)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                lsq_transitions_list.layoutManager = layoutManager
                lsq_transitions_list.adapter = transitionsAdapter
                transitionsAdapter.setCurrentPosition(0)

                lsq_transitions_duration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        if (!p2) return
                        mCurrentTransitionsDuration = (p1 + 1) * 1000L
                        TLog.e("duration ${mCurrentTransitionsDuration}  p1 ${p1}")
                        setCurrentState()
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        if (mCurrentVideoTransition == null) return
                        mThreadPool?.execute {
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            val transition = mCurrentVideoTransition!!
                            transition.duration = Math.max(mCurrentTransitionsDuration - 100 , 0 )
                            //转场加入到后一个clip上面
                            val boolean = mVideoLayer!!.setTransition(mClipList[1].mId.toInt(), transition)

                            val audioTransitions = mCurrentAudioTransition!!
                            audioTransitions.duration = Math.max(0,mCurrentTransitionsDuration - 100)
                            mAudioLayer!!.setTransition(mClipList[1].mId.toInt(), audioTransitions)
                            refreshEditor()
                            playerUnlock()
                            var targetPos = mClipList[0].videoClip.streamInfo.duration
                            mEditor!!.player.seekTo(max(targetPos - 3000,0))
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }
                    }

                })

                if (isRestore){
                    val transition : ClipLayer.Transition? = mVideoLayer!!.getTransition(mClipList[1].mId.toInt())
                    if (transition != null){
                        mCurrentTransitionsDuration = transition.duration + 100
                        transitionsAdapter.findTransitions(transition.name)
                    }

                }
                lsq_transitions_duration.progress = (((mCurrentTransitionsDuration / 1000L)) - 1).toInt()

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

        for (key in videoClipMap.keys){
            val videoClip = videoLayer.getClip(key)
            val audioClip = audioLayer.getClip(key)
            val path = videoClip.config.getString(VideoFileClip.CONFIG_PATH)
            mClipList.add(VideoItem(path, key.toLong(),videoClip, audioClip))
        }

        val trans : ClipLayer.Transition? = videoLayer.getTransition(mClipList[1]!!.mId.toInt())
        if (trans != null)
            mCurrentTransitionsDuration = trans.duration + 100

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        return true
    }

    private fun initLayer() {
        for (item in mVideoList!!) {
            mClipList.add(VideoItem.createVideoItem(item.path, mEditor!!,true,item.type == AlbumItemType.Video,item.audioPath))
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
            if (!audioLayer.addClip(item.mId.toInt(), item.audioClip)) {
                TLog.e("Audio clip add error ${item.mId}")
            }
        }

        if (!videoLayer.activate() || !audioLayer.activate()) {
            return
        }

        mEditor!!.videoComposition().addLayer(11, videoLayer)
        mEditor!!.audioComposition().addLayer(11, audioLayer)

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
    }

    public fun setCurrentState(){
        val duration = Math.floor((mCurrentTransitionsDuration / 1000).toDouble())
        lsq_editor_current_state.setText("当前转场持续时长 : ${duration}秒")
    }

}