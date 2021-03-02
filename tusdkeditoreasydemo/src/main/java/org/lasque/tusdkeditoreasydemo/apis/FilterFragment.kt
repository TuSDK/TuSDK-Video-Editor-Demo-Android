/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/10/29$ 11:22$
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
import com.tusdk.pulse.Player
import com.tusdk.pulse.Property
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.TusdkFilterEffect
import kotlinx.android.synthetic.main.filter_fragment.*
import kotlinx.android.synthetic.main.filter_fragment.view.*
import kotlinx.android.synthetic.main.movie_cut_fragment.*
import kotlinx.android.synthetic.main.movie_cut_fragment.lsq_editor_current_state
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkpulse.core.seles.tusdk.FilterOption
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.ApiActivity
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.OnItemClickListener
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkeditoreasydemo.utils.Constants
import java.util.concurrent.Callable
import java.util.concurrent.Future

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/29  11:22
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class FilterFragment : BaseFragment(FunctionType.FilterEffect) {

    private var mVideoItem: VideoItem? = null

    private var mVideoClip: Clip? = null

    private var mAudioClip: Clip? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mFilterConfig: Config? = null

    private var mFilterEffect: Effect? = null

    private var mFilterProperty: TusdkFilterEffect.PropertyBuilder? = null

    private var mMixied = 0.75

    private var isRestore = false

    override fun getLayoutId(): Int {
        return R.layout.filter_fragment
    }

    private fun setCurrentState() {
        lsq_editor_current_state.setText("当前滤镜强度 : ${String.format("%.2f", mMixied)}")
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            isRestore = restoreLayer()
            if (!isRestore) {
                initLayer()
            }

            runOnUiThread {
                setCurrentState()

                val filterAdapter = FilterAdapter(Constants.getEditorFilters(), requireContext())
                filterAdapter.setOnItemClickListener(object : OnItemClickListener<FilterOption, FilterAdapter.FilterViewHolder> {
                    override fun onItemClick(pos: Int, holder: FilterAdapter.FilterViewHolder, item: FilterOption) {

                        var res: Future<Boolean>? = mThreadPool?.submit(Callable<Boolean> {
                            val currentFrame = mPlayerContext!!.currentFrame
                            val currentState = mPlayerContext!!.state
                            mOnPlayerStateUpdateListener?.onPlayerPause()
                            playerLock()
                            mFilterConfig?.setString(TusdkFilterEffect.CONFIG_CODE, item.code)
                            val ret = mFilterEffect?.setConfig(mFilterConfig)
                            if (mVideoClip!!.effects().get(120) == null) {
                                mVideoClip!!.effects().add(120, mFilterEffect)
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

                        res?.get()
                        filterAdapter.setCurrentPosition(pos)
                    }

                })
                val layoutManger = LinearLayoutManager(requireContext())
                layoutManger.orientation = LinearLayoutManager.HORIZONTAL
                view.lsq_filter_list.layoutManager = layoutManger
                view.lsq_filter_list.adapter = filterAdapter

                view.lsq_mixied_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        mMixied = (p1.toFloat() / 100f).toDouble()
                        setCurrentState()
                        mThreadPool?.execute {
                            mFilterProperty!!.holder.strength = (p1.toFloat() / 100f).toDouble()
                            mFilterEffect?.setProperty(TusdkFilterEffect.PROP_PARAM, mFilterProperty?.makeProperty())
                            if (mPlayerContext!!.state != Player.State.kPLAYING) {
                                mPlayerContext!!.refreshFrame()
                            }
                        }
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                    }

                })

                lsq_close_filter.setOnClickListener {
                    filterAdapter.setCurrentPosition(-1)
                    mThreadPool?.execute {
                        val currentState = mPlayerContext!!.state
                        val currentFrame = mPlayerContext!!.currentFrame
                        playerLock()
                        mVideoClip!!.effects().delete(120)
                        refreshEditor()
                        playerUnlock()

                        if (currentState == Player.State.kPLAYING){
                            mEditor!!.player.seekTo(currentFrame)
                            mOnPlayerStateUpdateListener?.onPlayerPlay()
                        } else {
                            mEditor!!.player.previewFrame(currentFrame)

                        }
                    }
                }

                if (isRestore) {
                    filterAdapter.findFilter(mFilterConfig!!.getStringOr(TusdkFilterEffect.CONFIG_CODE,""))
                    view.lsq_mixied_bar.progress = (mMixied * 100).toInt()
                }
            }
        }
    }

    private fun restoreLayer(): Boolean {
        if (mEditor!!.videoComposition().allLayers.isEmpty()) {
            return false
        }

        val videoLayer = mEditor!!.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor!!.audioComposition().allLayers[11] as ClipLayer

        val videoClip = videoLayer.getClip(100)
        val audioClip = audioLayer.getClip(100)

        if (videoClip.effects().get(120) != null){
            mFilterEffect = videoClip.effects().get(120)
            mFilterConfig = mFilterEffect!!.config
        } else {
            mFilterEffect = Effect(mEditor!!.context, TusdkFilterEffect.TYPE_NAME)
            mFilterConfig = Config()
        }

        var property: Property? = mFilterEffect!!.getProperty(TusdkFilterEffect.PROP_PARAM)
        val filterHolder = if(property == null){
            val holder = TusdkFilterEffect.PropertyHolder()
            holder.strength = 0.75
            holder
        }
        else
            TusdkFilterEffect.PropertyHolder(property)
        mFilterProperty = TusdkFilterEffect.PropertyBuilder()
        mFilterProperty!!.holder = filterHolder

        mMixied = mFilterProperty!!.holder.strength

        mVideoClip = videoClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer

        return true
    }

    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path, mEditor!!, true, item.type == AlbumItemType.Video)

        var videoLayer = ClipLayer(mEditor!!.context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        videoLayer.config = videoLayerConfig

        if (!videoLayer.addClip(100, mVideoItem!!.mVideoClip)) {
            TLog.e("filter effect add video clip activate failed")
            return
        }

        var audioLayer = ClipLayer(mEditor!!.context, false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        audioLayer.config = audioLayerConfig

        if (!audioLayer.addClip(100, mVideoItem!!.mAudioClip)) {
            TLog.e("filter effect add audio clip activate failed")
            return
        }

        mEditor!!.videoComposition().addLayer(11, videoLayer)
        mEditor!!.audioComposition().addLayer(11, audioLayer)

        mFilterConfig = Config()
        mFilterConfig!!.setString(TusdkFilterEffect.CONFIG_CODE, "Portrait_Bright_1")

        mFilterProperty = TusdkFilterEffect.PropertyBuilder()
        mFilterProperty!!.holder.strength = 0.75
        mFilterEffect = Effect(mEditor!!.context, TusdkFilterEffect.TYPE_NAME)
        mFilterEffect!!.setConfig(mFilterConfig)
        mFilterEffect!!.setProperty(TusdkFilterEffect.PROP_PARAM, mFilterProperty!!.makeProperty())


        mVideoClip = mVideoItem!!.mVideoClip
        mAudioClip = mVideoItem!!.mAudioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
    }
}