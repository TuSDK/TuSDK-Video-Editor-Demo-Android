/**
 *  TuSDK
 *  VEDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2021/2/3$ 14:51$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import android.widget.Button
import com.tusdk.pulse.Config
import com.tusdk.pulse.Player
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.effects.AudioPitchEffect
import kotlinx.android.synthetic.main.audio_pitch_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkpulse.core.utils.TLog
import java.util.concurrent.Callable
import java.util.concurrent.Future

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * VEDemo
 *
 * @author        H.ys
 * @Date        2021/2/3  14:51
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AudioPitchFragment : BaseFragment(FunctionType.AudioPitch) {

    private var mVideoItem : VideoItem? = null

    private var mAudioLayer : ClipLayer? = null

    private var mAudioPitchEffect : Effect? = null

    private var mCurrentPitchType = AudioPitchEffect.TYPE_Normal

    private val mSoundPitchClickListener = object : View.OnClickListener{
        override fun onClick(p0: View?) {
            val pitch = getPitchType(p0!!.tag as String?)

            val res : Future<Boolean> = mThreadPool!!.submit(Callable<Boolean> {
                val currentFrame = mPlayerContext!!.currentFrame
                val currentState = mPlayerContext!!.state
                mOnPlayerStateUpdateListener?.onPlayerPause()
                playerLock()
                val pitchConfig = Config()
                pitchConfig.setString(AudioPitchEffect.CONFIG_TYPE,pitch)
                val ret = mAudioPitchEffect!!.setConfig(pitchConfig)
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

            val childCount = lsq_editor_audio_record_type_bar.childCount
            for (index in 0 until childCount){
                val view = lsq_editor_audio_record_type_bar.getChildAt(index) as Button
                if (view.tag == p0.tag){
                    view.setBackgroundResource(R.drawable.tusdk_edite_cut_speed_button_bg)
                    view.setTextColor(resources.getColor(R.color.lsq_editor_cut_select_font_color))
                } else {
                    view.setBackgroundResource(0)
                    view.setTextColor(resources.getColor(R.color.lsq_color_white))
                }
            }
        }

    }

    private fun getPitchType(p0: String?): String {
        val pitch = when (p0!!) {
            "0" -> {
                AudioPitchEffect.TYPE_Monster
            }
            "1" -> {
                AudioPitchEffect.TYPE_Uncle
            }
            "2" -> {
                AudioPitchEffect.TYPE_Normal
            }
            "3" -> {
                AudioPitchEffect.TYPE_Girl
            }
            "4" -> {
                AudioPitchEffect.TYPE_Lolita
            }

            else -> AudioPitchEffect.TYPE_Normal
        }
        return pitch
    }


    override fun getLayoutId(): Int {
        return R.layout.audio_pitch_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            if (!restoreLayer()){
                initLayer()
            }

            runOnUiThread {
                val childCount = lsq_editor_audio_record_type_bar.childCount
                for (index in 0 until childCount){
                    var view : Button = lsq_editor_audio_record_type_bar.getChildAt(index) as Button
                    view.setOnClickListener(mSoundPitchClickListener)
                    var pitchType = getPitchType(view.tag as String?)
                    if (pitchType == mCurrentPitchType){
                        view.setBackgroundResource(R.drawable.tusdk_edite_cut_speed_button_bg)
                        view.setTextColor(resources.getColor(R.color.lsq_editor_cut_select_font_color))
                    } else {
                        view.setBackgroundResource(0)
                        view.setTextColor(resources.getColor(R.color.lsq_color_white))
                    }
                }

            }
        }
    }

    private fun initLayer(){
        val item  = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video)

        val context = mEditor!!.context
        val videoLayer = ClipLayer(context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        val audioLayer = ClipLayer(context,false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        audioLayer.setConfig(audioLayerConfig)

        if (!videoLayer.addClip(1, mVideoItem!!.videoClip)) {
            TLog.e("Video clip add error ${mVideoItem!!.mId}")
        }

        if (!audioLayer.addClip(1, mVideoItem!!.audioClip)) {
            TLog.e("Audio clip add error ${mVideoItem!!.mId}")
        }

        mEditor!!.videoComposition().addLayer(1,videoLayer)
        mEditor!!.audioComposition().addLayer(1,audioLayer)

        val effect = Effect(mEditor!!.context,AudioPitchEffect.TYPE_NAME)
        val pitchConfig = Config()
        pitchConfig.setString(AudioPitchEffect.CONFIG_TYPE,AudioPitchEffect.TYPE_Normal)
        effect.setConfig(pitchConfig)
        mVideoItem!!.mAudioClip.effects().add(32,effect)

        mAudioLayer = audioLayer
        mAudioPitchEffect = effect

    }

    private fun restoreLayer() : Boolean{
        if (mEditor!!.videoComposition().allLayers.isEmpty()) {
            return false
        }

        val audioLayer = mEditor!!.audioComposition().allLayers[1] as ClipLayer
        val audioClip = audioLayer.allClips[1]!!
        val effect = audioClip.effects()[32]
        val config = effect.config
        val pitchType = config.getString(AudioPitchEffect.CONFIG_TYPE)
        mCurrentPitchType = pitchType


        mAudioLayer = audioLayer
        mAudioPitchEffect = effect

        return true
    }
}