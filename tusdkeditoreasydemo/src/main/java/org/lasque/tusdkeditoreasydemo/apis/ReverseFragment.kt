/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/9$ 11:36$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.tusdk.pulse.*
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.clips.VideoReverseFileClip
import com.tusdk.pulse.editor.effects.CanvasResizeEffect
import kotlinx.android.synthetic.main.api_activity.*
import kotlinx.android.synthetic.main.time_fragment.*
import kotlinx.android.synthetic.main.time_fragment.lsq_editor_cut_load
import kotlinx.android.synthetic.main.time_fragment.lsq_editor_cut_load_parogress
import kotlinx.android.synthetic.main.time_fragment.view.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.jetbrains.anko.support.v4.toast
import org.lasque.tusdkpulse.core.TuSdk
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.utils.StringHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.ApiActivity
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkeditoreasydemo.utils.MD5Util
import java.io.File
import java.util.concurrent.Semaphore

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/9  11:36
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class ReverseFragment : BaseFragment(FunctionType.ReverseEffect) {

    private var mVideoItem: VideoItem? = null

    private var mStartTime = 0L

    private var mEndTime = 0L

    private var mVideoClip: Clip? = null

    private var mAudioClip: Clip? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mVideoReverseClip: Clip? = null

    private var isReverse = true

    private var mVideoDuration = 0

    private val transcoderSemaphore: Semaphore = Semaphore(0)

    override fun getLayoutId(): Int {
        return R.layout.time_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            if (!restoreLayer()){
                initLayer()
            }

            runOnUiThread {
                if (isReverse){
                    lsq_video_reverse.text = "开启倒播"
                } else {
                    lsq_video_reverse.text = "关闭倒播"
                }
                view.lsq_video_reverse.setOnClickListener {
                    mThreadPool?.execute {
                        mOnPlayerStateUpdateListener?.onPlayerPause()
                        playerLock()
                        isReverse = !isReverse
                        if (isReverse) {
                            runOnUiThread {
                                lsq_video_reverse.text = "开启倒播"
                            }
                            mVideoLayer!!.deleteClip(100)
                            mVideoLayer!!.addClip(100, mVideoClip)
                        } else {
                            val deleteClip = mVideoLayer!!.deleteClip(100)
                            if (mVideoReverseClip == null) {
                                checkVideoState()
                                initReverseClip()
                            }
                            runOnUiThread {
                                lsq_video_reverse.text = "关闭倒播"
                            }
                            val addClip = mVideoLayer!!.addClip(100, mVideoReverseClip)
                            TLog.e("add clip %s delete clip %s",addClip,deleteClip)
                        }
                        refreshEditor()
                        playerUnlock()
                        mEditor!!.player.seekTo(0)
                        mOnPlayerStateUpdateListener?.onPlayerPlay()
                        setCanBackPressed(true)
                    }
                }
            }

        }
    }

    private fun restoreLayer() : Boolean{
        if (mEditor!!.videoComposition().allLayers.isEmpty()){
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor!!.audioComposition().allLayers[11] as ClipLayer

        val videoClipMap = videoLayer.allClips
        val videoClip = videoLayer.getClip(100)
        val path = videoClip.config.getString(VideoReverseFileClip.CONFIG_PATH)
        if (videoClip.type == VideoReverseFileClip.TYPE_NAME){
            mVideoReverseClip = videoClip
            val originalVideoClip = Clip(mEditor!!.context,VideoFileClip.TYPE_NAME);
            val config = Config()
            config.setString(VideoFileClip.CONFIG_PATH,path)
            originalVideoClip.setConfig(config)
            mVideoClip = originalVideoClip
            isReverse = false
        } else {
            mVideoClip = videoClip
            isReverse = true
        }

        val audioClip = audioLayer.getClip(100)
        val item = AlbumInfo(path, AlbumItemType.Video, 0, 0, MD5Util.crypt(path))
        mVideoList!!.add(item)

        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        mVideoDuration = mVideoClip!!.streamInfo.duration.toInt()

        return true
    }

    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path, mEditor!!, true, item.type == AlbumItemType.Video,item.audioPath)
        val videoClip = mVideoItem!!.mVideoClip
        val audioClip = mVideoItem!!.mAudioClip

        val duration = videoClip.streamInfo.duration
        mStartTime = 0
        mEndTime = duration
        val videoLayer = ClipLayer(mEditor!!.context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        videoLayer.setConfig(videoLayerConfig)

        if (!videoLayer.addClip(100, videoClip)) {
            return
        }

        val audioLayer = ClipLayer(mEditor!!.context, false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        audioLayer.setConfig(audioLayerConfig)

        if (!audioLayer.addClip(100, audioClip)) {
            return
        }

        val resizeConfig = Config()
        val resizeEffect = Effect(mEditor!!.context, CanvasResizeEffect.TYPE_NAME)
        resizeEffect.setConfig(resizeConfig)
        videoClip.effects().add(1, resizeEffect)
        val pro = CanvasResizeEffect.PropertyBuilder()
        resizeEffect.setProperty(CanvasResizeEffect.PROP_PARAM, pro.makeProperty())

        mEditor!!.videoComposition().addLayer(11, videoLayer)
        mEditor!!.audioComposition().addLayer(11, audioLayer)

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        mVideoDuration = mVideoClip!!.streamInfo.duration.toInt()
    }

    private fun checkVideoState() {
        val path = mVideoList!![0].path
        val mediaInfo = MediaInspector.shared().inspect(path)
        for (info in mediaInfo.streams) {
            if (info is MediaInspector.MediaInfo.Video) {
                if (!info.directReverse) {
                    mVideoList!![0].path = videoTranscoder(path)
                }
            }
        }
    }

    private fun videoTranscoder(inputPath: String): String {
        val outputPath = getOutputTempFilePath().path
        val transcoder = VideoPreprocessor()
        transcoder.setListener(object : VideoPreprocessor.Listener {

            override fun onEvent(a: VideoPreprocessor.Action?, ts: Long) {
                when(a){
                    VideoPreprocessor.Action.OPEN->{

                    }

                    VideoPreprocessor.Action.CLOSE->{
//                        transcoderSemaphore.release()
                        runOnUiThread {
                            (activity as ApiActivity).setEnable(true)
                            lsq_video_reverse.visibility = View.VISIBLE
                            lsq_editor_cut_load.setVisibility(View.GONE)
                            lsq_editor_cut_load_parogress.setValue(0f)
                            Toast.makeText(requireContext(), "视频转码结束", Toast.LENGTH_SHORT).show()
                        }
                    }

                    VideoPreprocessor.Action.START->{

                    }
                    VideoPreprocessor.Action.WRITTING->{
                        val currentDuration = mVideoDuration
                        runOnUiThread {
                            lsq_video_reverse.visibility = View.GONE
                            lsq_editor_cut_load.setVisibility(View.VISIBLE)
                            lsq_editor_cut_load_parogress.setValue((ts / currentDuration.toFloat()) * 100f)
                        }
                    }
                    VideoPreprocessor.Action.CANCEL->{

                    }


                }
            }

        })

        val processerConfig = VideoPreprocessor.Config()
        processerConfig.inputPath = inputPath
        processerConfig.outputPath = outputPath
        processerConfig.keyint = 0
        if (!transcoder.open(processerConfig)) {
            TLog.e("Transcoder Error")
        }
        transcoder.start()
        runOnUiThread {
            (activity as ApiActivity).setEnable(false)
            toast("视频转码开始")
        }
        setCanBackPressed(false)
//        transcoderSemaphore.acquire()
        transcoder.close(true)
        return outputPath
    }

    /** 获取临时文件路径  */
    protected fun getOutputTempFilePath(): File {
        return File(TuSdk.getAppTempPath(), String.format("lsq_%s.mp4", StringHelper.timeStampString()))
    }

    private fun initReverseClip() {
        val videoReverseConfig = Config()
        videoReverseConfig.setString(VideoReverseFileClip.CONFIG_PATH, mVideoList!![0].path)
        val videoReverseClip = Clip(mEditor!!.context, VideoReverseFileClip.TYPE_NAME)
        videoReverseClip.setConfig(videoReverseConfig)

        if (!videoReverseClip.activate()) {
            return
        }

        val resizeConfig = Config()
        val resizeEffect = Effect(mEditor!!.context, CanvasResizeEffect.TYPE_NAME)
        resizeEffect.setConfig(resizeConfig)
        videoReverseClip.effects().add(1, resizeEffect)

        mVideoReverseClip = videoReverseClip

    }

    override fun onDestroyView() {
        super.onDestroyView()

    }

    override fun onDestroy() {
        super.onDestroy()

        mThreadPool?.execute {
            if (mVideoReverseClip != null){
                mVideoReverseClip!!.deactivate()
            }
            if (mVideoClip != null){
                mVideoClip!!.deactivate()
            }
        }
    }
}