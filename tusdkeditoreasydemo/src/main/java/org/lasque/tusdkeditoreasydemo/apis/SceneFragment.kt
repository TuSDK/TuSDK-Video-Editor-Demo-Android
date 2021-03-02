/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/5$ 10:39$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.recyclerview.widget.LinearLayoutManager
import cn.bar.DoubleHeadedDragonBar
import com.tusdk.pulse.Config
import com.tusdk.pulse.Player
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.AudioTrimEffect
import com.tusdk.pulse.editor.effects.CanvasResizeEffect
import com.tusdk.pulse.editor.effects.TusdkSceneEffect
import com.tusdk.pulse.editor.effects.VideoTrimEffect
import kotlinx.android.synthetic.main.movie_cut_fragment.*
import kotlinx.android.synthetic.main.movie_cut_fragment.view.*
import kotlinx.android.synthetic.main.scene_fragment.*
import kotlinx.android.synthetic.main.scene_fragment.lsq_editor_current_state
import kotlinx.android.synthetic.main.scene_fragment.view.*
import kotlinx.android.synthetic.main.scene_fragment.view.lsq_start_bar
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.OnItemClickListener
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkeditoreasydemo.utils.Constants
import java.util.concurrent.Callable
import java.util.concurrent.Future
import kotlin.math.max
import kotlin.math.min

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/5  10:39
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class SceneFragment : BaseFragment(FunctionType.SceneEffect) {

    private var mVideoItem: VideoItem? = null
    private var mStartTime = 0L

    private var mEndTime = 0L

    private var mVideoClip: Clip? = null

    private var mAudioClip: Clip? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mSceneEffectConfig : Config = Config()

    private var mSceneEffect : Effect? = null

    private var mScenePropertyBuilder : TusdkSceneEffect.PropertyBuilder = TusdkSceneEffect.PropertyBuilder()

    private var mMaxDuration = 0L

    private var isRestore = false


    override fun getLayoutId(): Int {
        return R.layout.scene_fragment
    }

    private fun setCurrentState() {
        val currentVideoHour = mStartTime / 3600000
        val currentVideoMinute = (mStartTime % 3600000) / 60000

        val currentVideoSecond = (mStartTime % 60000 / 1000)

        val durationVideoHour = mEndTime / 3600000

        val durationVideoMinute = (mEndTime % 3600000) / 60000

        val durationVideoSecond = (mEndTime % 60000 / 1000)
        lsq_editor_current_state.setText("场景特效作用范围 开始时间 : ${currentVideoHour}:$currentVideoMinute:$currentVideoSecond 结束时间 : $durationVideoHour:$durationVideoMinute:$durationVideoSecond")
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {

            isRestore = restoreLayer()
            if (!isRestore){
                initLayer()
            }

            var streamInfo = mVideoClip!!.streamInfo
            mMaxDuration = streamInfo.duration

            runOnUiThread {
                setCurrentState()

                val sceneGroups = Constants.SCENE_EFFECT_CODES

                val sceneAdapter = SceneAdapter(sceneGroups.toMutableList(),requireContext())
                sceneAdapter.setOnItemClickListener(object : OnItemClickListener<String,SceneAdapter.SceneViewHolder>{
                    override fun onItemClick(pos: Int, holder: SceneAdapter.SceneViewHolder, item: String) {
                        var res : Future<Boolean> = mThreadPool!!.submit(Callable<Boolean> {
                            val currentFrame = mPlayerContext!!.currentFrame
                            val currentState = mPlayerContext!!.state

                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mSceneEffectConfig.setString(TusdkSceneEffect.CONFIG_NAME,item)
                            val ret = mSceneEffect?.setConfig(mSceneEffectConfig)
                            if (mVideoClip!!.effects().get(110) == null){
                                mVideoClip!!.effects().add(110,mSceneEffect)
                            }
                            mSceneEffect?.setProperty(TusdkSceneEffect.PROP_PARAM,mScenePropertyBuilder.makeProperty())
                            refreshEditor()
                            playerUnlock()
                            if (currentState == Player.State.kPLAYING || currentState == Player.State.kDO_PLAY){
                                mEditor!!.player.seekTo(currentFrame)
                                mOnPlayerStateUpdateListener?.onPlayerPlay()
                            } else {
                                mEditor!!.player.previewFrame(currentFrame)
                            }

                            ret
                        })

                        res.get()
                        sceneAdapter.setCurrentPosition(pos)
                    }
                })

                val layoutManager = LinearLayoutManager(context)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                lsq_scene_list.layoutManager = layoutManager
                lsq_scene_list.adapter = sceneAdapter

                view.lsq_start_bar.setCallBack(object : DoubleHeadedDragonBar.DhdBarCallBack() {
                    override fun getMaxString(value: Int): String {
                        mEndTime = value.toLong()
                        TLog.e("start : ${mStartTime} end : $mEndTime")
                        return super.getMaxString(value)
                    }

                    override fun getMinString(value: Int): String {
                        mStartTime = value.toLong()
                        TLog.e("start : ${mStartTime} end : $mEndTime")
                        return super.getMinString(value)
                    }

                    override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                        mStartTime = (mMaxDuration * minPercentage / 100).toLong()
                        mEndTime = (mMaxDuration * maxPercentage / 100).toLong()
                        if (mEndTime - mStartTime < 100){
                            mEndTime = mStartTime + 100
                        }
                        setCurrentState()
                        mThreadPool?.execute {
                            mScenePropertyBuilder.holder.begin = mStartTime
                            mScenePropertyBuilder.holder.end = mEndTime
                            mSceneEffect?.setProperty(TusdkSceneEffect.PROP_PARAM,mScenePropertyBuilder.makeProperty())
                        }
                    }

                    override fun getMinMaxString(value: Int, value1: Int): String {
                        return super.getMinMaxString(value, value1)
                    }

                })

                view.lsq_start_bar.post {
                    view.lsq_start_bar.minValue = ((mStartTime / mMaxDuration.toDouble()) * 100).toInt()
                    view.lsq_start_bar.maxValue = ((mEndTime / mMaxDuration.toDouble()) * 100).toInt()
                    view.lsq_start_bar.invalidate()
                }

                if (isRestore){
                    sceneAdapter.findScene(mSceneEffect!!.config.getStringOr(TusdkSceneEffect.CONFIG_NAME,""))
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

        val videoClip = videoLayer.getClip(100)
        val audioClip = audioLayer.getClip(100)

        if ( videoClip.effects().get(110) != null){
            mSceneEffect = videoClip.effects().get(110)
            var sceneProperty = mSceneEffect!!.getProperty(TusdkSceneEffect.PROP_PARAM)
            if (sceneProperty != null)
            {
                mScenePropertyBuilder.holder = TusdkSceneEffect.PropertyHolder(sceneProperty)
                mStartTime = mScenePropertyBuilder.holder.begin
                mEndTime = mScenePropertyBuilder.holder.end
            } else {
                val duration = videoClip.streamInfo.duration
                mStartTime = 0
                mEndTime = duration
                mScenePropertyBuilder.holder.begin = mStartTime
                mScenePropertyBuilder.holder.end = mEndTime
            }

        } else {
            mSceneEffect = Effect(mEditor!!.context, TusdkSceneEffect.TYPE_NAME)
            val duration = videoClip.streamInfo.duration
            mStartTime = 0
            mEndTime = duration
            mScenePropertyBuilder.holder.begin = mStartTime
            mScenePropertyBuilder.holder.end = mEndTime
        }


        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
        mVideoClip = videoClip
        mAudioClip = audioClip
        return true

    }
    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path, mEditor!!, true, item.type == AlbumItemType.Video)

        val videoClip = mVideoItem!!.mVideoClip
        val audioClip = mVideoItem!!.mAudioClip



        val duration = videoClip.streamInfo.duration
        mStartTime = 0
        mEndTime = duration

        val videoLayer = ClipLayer(mEditor!!.context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        if (!videoLayer.addClip(100,videoClip)){
            return
        }

        val audioLayer = ClipLayer(mEditor!!.context,false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        audioLayer.setConfig(audioLayerConfig)

        if (!audioLayer.addClip(100, audioClip)){
            return
        }

        mEditor!!.videoComposition().addLayer(11,videoLayer)
        mEditor!!.audioComposition().addLayer(11,audioLayer)

        mSceneEffect = Effect(mEditor!!.context, TusdkSceneEffect.TYPE_NAME)
        mScenePropertyBuilder.holder.begin = mStartTime
        mScenePropertyBuilder.holder.end = mEndTime

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
    }
}