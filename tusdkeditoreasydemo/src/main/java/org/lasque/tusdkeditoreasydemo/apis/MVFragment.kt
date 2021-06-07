/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/4$ 10:18$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import cn.bar.DoubleHeadedDragonBar
import com.tusdk.pulse.Config
import com.tusdk.pulse.MediaInspector
import com.tusdk.pulse.Player
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.*
import kotlinx.android.synthetic.main.mv_fragment.*
import kotlinx.android.synthetic.main.mv_fragment.lsq_editor_current_state
import kotlinx.android.synthetic.main.mv_fragment.view.lsq_start_bar
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.modules.view.widget.sticker.StickerGroup
import org.lasque.tusdkpulse.modules.view.widget.sticker.StickerLocalPackage
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.OnItemClickListener
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import java.util.concurrent.Callable
import java.util.concurrent.Future

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/4  10:18
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class MVFragment :BaseFragment(FunctionType.MVEffect) {

    private var mVideoItem: VideoItem? = null

    private var mStartTime = 0L

    private var mEndTime = 0L

    private var mVideoClip: Clip? = null

    private var mAudioClip: Clip? = null

    private var mMvAudioClip : Clip? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mMVAudioLayer : ClipLayer? = null

    private var mMVAudioLayerConfig : Config = Config()

    private var mMVEffectConfig : Config = Config()

    private var mMVEffect : Effect? = null

    private var mMvAudioConfig : Config = Config()

    private var mMvAudioEffect : Effect? = null

    private var mMvAudioEffectConfig = Config()

    private var mCurrentMVAudioPath = ""

    private var mMVPropertyBuilder : TusdkMVEffect.PropertyBuilder = TusdkMVEffect.PropertyBuilder()

    private var mMaxDuration = 0L

    private var isRestore = false


    override fun getLayoutId(): Int {
        return R.layout.mv_fragment
    }

    private fun setCurrentState() {
        val currentVideoHour = mStartTime / 3600000
        val currentVideoMinute = (mStartTime % 3600000) / 60000

        val currentVideoSecond = (mStartTime % 60000 / 1000)

        val durationVideoHour = mEndTime / 3600000

        val durationVideoMinute = (mEndTime % 3600000) / 60000

        val durationVideoSecond = (mEndTime % 60000 / 1000)
        lsq_editor_current_state.setText("MV特效 开始时间 :${currentVideoHour}:$currentVideoMinute:$currentVideoSecond 结束时间 : $durationVideoHour:$durationVideoMinute:$durationVideoSecond")
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {

            isRestore = restoreLayer()
            if (!isRestore){
                initLayer()
            }

            runOnUiThread {
                setCurrentState()

                mMaxDuration = mVideoClip!!.streamInfo.duration

                view.lsq_start_bar.setCallBack(object : DoubleHeadedDragonBar.DhdBarCallBack() {
                    override fun getMaxString(value: Int): String {
                        return super.getMaxString(value)
                    }

                    override fun getMinString(value: Int): String {
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
                            val currentFrame = mPlayerContext!!.currentFrame
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            mMVPropertyBuilder.holder.begin = mStartTime
                            mMVPropertyBuilder.holder.end = mEndTime
                            mMVEffect?.setProperty(TusdkMVEffect.PROP_PARAM,mMVPropertyBuilder.makeProperty())
                            mEditor?.player?.lock()
                            mMVAudioLayerConfig.setNumber(Layer.CONFIG_START_POS,mStartTime)
                            mMVAudioLayer?.setConfig(mMVAudioLayerConfig)
                            mMvAudioConfig.setNumber(AudioFileClip.CONFIG_TRIM_DURATION,mMVPropertyBuilder.holder.end-mMVPropertyBuilder.holder.begin)
                            mMvAudioClip?.setConfig(mMvAudioConfig)
                            mMvAudioEffectConfig.setNumber(AudioRepeatEffectV2.CONFIG_DURATION,mEndTime - mStartTime)
                            mMvAudioEffect?.setConfig(mMvAudioEffectConfig)
                            mEditor?.player?.unlock()
                            refreshEditor()
                            mEditor?.player?.seekTo(currentFrame)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        }
                    }

                    override fun getMinMaxString(value: Int, value1: Int): String {
                        return super.getMinMaxString(value, value1)
                    }
                })

                val mvGroups = mutableListOf<MvItem>()
                mvGroups.add(MvItem(StickerLocalPackage.shared().getStickerGroup(1420),"android_asset://audios/lsq_audio_cat.mp3"))
                mvGroups.add(MvItem(StickerLocalPackage.shared().getStickerGroup(1427),"android_asset://audios/lsq_audio_crow.mp3"))
                mvGroups.add(MvItem(StickerLocalPackage.shared().getStickerGroup(1432),"android_asset://audios/lsq_audio_tangyuan.mp3"))
                mvGroups.add(MvItem(StickerLocalPackage.shared().getStickerGroup(1470),"android_asset://audios/lsq_audio_oldmovie.mp3"))
                mvGroups.add(MvItem(StickerLocalPackage.shared().getStickerGroup(1469),"android_asset://audios/lsq_audio_relieve.mp3"))
                val mvAdapter = MVItemAdapter(mvGroups,requireContext())
                mvAdapter.setOnItemClickListener(object : OnItemClickListener<MVFragment.MvItem, MVItemAdapter.MVViewHolder> {
                    override fun onItemClick(pos: Int, holder: MVItemAdapter.MVViewHolder, item: MVFragment.MvItem) {
                        var res : Future<Boolean> = mThreadPool!!.submit(Callable<Boolean> {
                            val currentFrame = mPlayerContext!!.currentFrame
                            val currentState = mPlayerContext!!.state
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mMVEffectConfig.setNumber(TusdkMVEffect.CONFIG_ID,item.stickerGroup.groupId)
                            val ret = mMVEffect?.setConfig(mMVEffectConfig)
                            if (mVideoClip!!.effects().getAll().get(100) == null){
                                mVideoClip!!.effects().add(100,mMVEffect)
                            }
                            if (mEditor!!.audioComposition().getAllLayers()[12] == null){
                                mMvAudioConfig.setString(AudioFileClip.CONFIG_PATH,item.backgroundMusicPath)
                                mMvAudioClip!!.setConfig(mMvAudioConfig)
                                mMVAudioLayer!!.addClip(100,mMvAudioClip)
                                mMVAudioLayerConfig.setNumber(Layer.CONFIG_START_POS,mStartTime)
                                mMVAudioLayer!!.setConfig(mMVAudioLayerConfig)
                                mEditor!!.audioComposition().addLayer(12,mMVAudioLayer)
                                val effect = Effect(mEditor!!.context,AudioRepeatEffectV2.TYPE_NAME)
                                val effectConfig = Config()
                                effectConfig.setNumber(AudioRepeatEffectV2.CONFIG_DURATION,mEndTime - mStartTime)
                                TLog.e("start time ${mStartTime} end time ${mEndTime}")
                                effect.setConfig(effectConfig)
                                mMvAudioClip!!.effects().add(200,effect)
                                mMvAudioEffect = effect
                                mMvAudioEffectConfig = effectConfig
                            } else {
                                mMvAudioConfig.setString(AudioFileClip.CONFIG_PATH,item.backgroundMusicPath)
                                mMvAudioClip!!.setConfig(mMvAudioConfig)
                            }
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

                        mvAdapter.setCurrentPosition(pos)
                    }

                })

                val layoutManager = LinearLayoutManager(context)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                lsq_mv_list.layoutManager = layoutManager
                lsq_mv_list.adapter = mvAdapter
                if (isRestore){
                    mvAdapter.findMV(mMVEffectConfig.getNumberOr(TusdkMVEffect.CONFIG_ID,0.0).toLong())
                    TLog.e("mstartTime ${mStartTime} mendTime ${mEndTime}")
                    view.lsq_start_bar.post {
                        view.lsq_start_bar.minValue = ((mStartTime / mMaxDuration.toDouble()) * 100).toInt()
                        view.lsq_start_bar.maxValue = ((mEndTime / mMaxDuration.toDouble()) * 100).toInt()
                        view.lsq_start_bar.invalidate()
                    }
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

        val videoClip = videoLayer.getClip(100)
        val audioClip = audioLayer.getClip(100)

        val  layer =mEditor!!.audioComposition().allLayers[12]
        if (layer == null){
            mMVAudioLayer = ClipLayer(mEditor!!.context,false)
            mMvAudioClip = Clip(mEditor!!.context,AudioFileClip.TYPE_NAME)
        } else {
            mMVAudioLayer = layer as ClipLayer
            mMvAudioClip = mMVAudioLayer!!.getClip(100)
            mMvAudioConfig = mMvAudioClip!!.config
        }

        mMVEffect = videoClip.effects().get(100)
        if (mMVEffect == null){
            mMVEffect = Effect(mEditor!!.context,TusdkMVEffect.TYPE_NAME)
        } else {
            mMVEffectConfig = mMVEffect!!.config
        }

        var property = mMVEffect!!.getProperty(TusdkMVEffect.PROP_PARAM)
        if (property != null){
            val mvHolder = TusdkMVEffect.PropertyHolder(property)
            mMVPropertyBuilder.holder = mvHolder

            mStartTime = mvHolder.begin
            mEndTime = mvHolder.end
        } else {
            val duration = videoClip.streamInfo.duration
            mStartTime = 0
            mEndTime = duration
        }

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        return true
    }
    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video,item.audioPath)
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

        mMVAudioLayer = ClipLayer(mEditor!!.context,false)
        mMvAudioClip = Clip(mEditor!!.context,AudioFileClip.TYPE_NAME)

//        val resizeConfig = Config()
//        val resizeEffect = Effect(mEditor!!.context,CanvasResizeEffect.TYPE_NAME)
//        resizeEffect.setConfig(resizeConfig)
//        videoClip.effects().add(1,resizeEffect)

        mEditor!!.videoComposition().addLayer(11,videoLayer)
        mEditor!!.audioComposition().addLayer(11,audioLayer)

        mMVPropertyBuilder.holder.begin = mStartTime
        mMVPropertyBuilder.holder.end = mEndTime
//        mMvAudioConfig.setNumber(AudioFileClip.CONFIG_TRIM_START,mStartTime)
//        mMvAudioConfig.setNumber(AudioFileClip.CONFIG_TRIM_DURATION,mEndTime - mStartTime)

        mMVEffect = Effect(mEditor!!.context,TusdkMVEffect.TYPE_NAME)

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

    }

    data class MvItem(val stickerGroup : StickerGroup,val backgroundMusicPath : String)
}