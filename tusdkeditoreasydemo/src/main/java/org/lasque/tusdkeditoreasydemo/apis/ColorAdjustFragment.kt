/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/26$ 16:51$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.tusdk.pulse.Config
import com.tusdk.pulse.Property
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.effects.ColorAdjustEffect
import kotlinx.android.synthetic.main.color_adjust_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseFragment
import org.lasque.tusdkeditoreasydemo.base.ColorAdjustItem
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import java.util.ArrayList

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/26  16:51
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class ColorAdjustFragment : BaseFragment() {

    private var mVideoItem : VideoItem? = null

    private var mAdjustEffect : Effect? = null

    private var mAdjustPropertyBuilder : ColorAdjustEffect.PropertyBuilder = ColorAdjustEffect.PropertyBuilder()

    val mAdjustList = mutableListOf<ColorAdjustItem>(
            ColorAdjustItem.WhiteBalance,
            ColorAdjustItem.HighlightShadow,
            ColorAdjustItem.Sharpen,
            ColorAdjustItem.Brightness,
            ColorAdjustItem.Contrast,
            ColorAdjustItem.Saturation,
            ColorAdjustItem.Exposure
    )

    override fun getLayoutId(): Int {
        return R.layout.color_adjust_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            if (!restoreLayer()){
                initLayer()
            }

            runOnUiThread {
//                setCurrentState()
                val layoutManager = LinearLayoutManager(requireContext())
                layoutManager.orientation = LinearLayoutManager.VERTICAL
                val colorAdjustAdapter = ColorAdjustAdapter(mAdjustList,requireContext())
                colorAdjustAdapter.mOnParamsRefreshListener = object : ColorAdjustAdapter.OnParamsRefreshListener{
                    override fun onParamsRefresh() {
                        mThreadPool?.execute {
                            val property = mAdjustPropertyBuilder.makeProperty()
                            TLog.e("property ${property.toString()}")
                            mAdjustEffect?.setProperty(ColorAdjustEffect.PROP_PARAM,property)
                            mOnPlayerStateUpdateListener?.onRefreshFrame()
                        }
                    }
                }

                colorAdjustAdapter.mOnClipChangeListener = object : ColorAdjustAdapter.OnClipChangeListener{
                    override fun onSwap(item0: ColorAdjustItem, item1: ColorAdjustItem) {
                        mThreadPool?.execute {
                            mAdjustPropertyBuilder.holder.items = getPropertyItems()
                            mAdjustEffect?.setProperty(ColorAdjustEffect.PROP_PARAM,mAdjustPropertyBuilder.makeProperty())
                            mOnPlayerStateUpdateListener?.onRefreshFrame()
                        }
                    }
                }

                lsq_color_adjust_list.layoutManager = layoutManager
                lsq_color_adjust_list.adapter = colorAdjustAdapter

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
        val videoClip = videoLayer.getClip(videoClipMap.keys.first())

        val audioClipMap = audioLayer.allClips
        val audioClip = audioLayer.getClip(audioClipMap.keys.first())

        mAdjustEffect = videoClip.effects().get(10)
        val property : Property? = mAdjustEffect!!.getProperty(ColorAdjustEffect.PROP_PARAM)
        val holder = if (property == null)ColorAdjustEffect.PropertyHolder() else ColorAdjustEffect.PropertyHolder(property)

        if (holder.items.isNotEmpty()){
            mAdjustList.clear()
            for (i in 0 until holder.items.size){
                val pItem = holder.items[i]
                when(pItem.name){
                    ColorAdjustEffect.PROP_TYPE_WhiteBalance -> {mAdjustList.add(ColorAdjustItem.WhiteBalance)}
                    ColorAdjustEffect.PROP_TYPE_HighlightShadow -> {mAdjustList.add(ColorAdjustItem.HighlightShadow)}
                    ColorAdjustEffect.PROP_TYPE_Sharpen -> {mAdjustList.add(ColorAdjustItem.Sharpen)}
                    ColorAdjustEffect.PROP_TYPE_Brightness -> {mAdjustList.add(ColorAdjustItem.Brightness)}
                    ColorAdjustEffect.PROP_TYPE_Contrast -> {mAdjustList.add(ColorAdjustItem.Contrast)}
                    ColorAdjustEffect.PROP_TYPE_Saturation -> {mAdjustList.add(ColorAdjustItem.Saturation)}
                    ColorAdjustEffect.PROP_TYPE_Exposure -> {mAdjustList.add(ColorAdjustItem.Exposure)}
                }

                for (index in pItem.values.indices){
                    mAdjustList[i].propertyItem.values[index] = pItem.values[index]
                    mAdjustList[i].params[index].defaultValue = pItem.values[index]
                }

            }
        }


        mAdjustPropertyBuilder.holder.items = getPropertyItems()
        return true
    }

    private fun initLayer() {
        val item = mVideoList!![0]
        mVideoItem = VideoItem.createVideoItem(item.path,mEditor!!,true,item.type == AlbumItemType.Video,item.audioPath)

        val duration = mVideoItem!!.mVideoClip.streamInfo.duration

        val context = mEditor!!.context

        val videoLayer = ClipLayer(context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.setConfig(videoLayerConfig)

        val mainAudioLayer = ClipLayer(context,false)
        val mainAudioConfig = Config()
        mainAudioConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        mainAudioLayer.setConfig(mainAudioConfig)

        if (!videoLayer.addClip(100,mVideoItem!!.mVideoClip)){
            TLog.e("color adjust effect add video clip activate failed")
        }

        if (!mainAudioLayer.addClip(100,mVideoItem!!.mAudioClip)){
            TLog.e("color adjust effect add audio clip activate failed")
        }

        mEditor!!.videoComposition().addLayer(11,videoLayer)
        mEditor!!.audioComposition().addLayer(11,mainAudioLayer)

        val colorAdjustEffect = Effect(mEditor!!.context,ColorAdjustEffect.TYPE_NAME)
        mVideoItem!!.mVideoClip.effects().add(10,colorAdjustEffect)

        mAdjustEffect = colorAdjustEffect
        mAdjustPropertyBuilder.holder.items = getPropertyItems()
    }

    private fun getPropertyItems() : MutableList<ColorAdjustEffect.PropertyItem>{
        val list = ArrayList<ColorAdjustEffect.PropertyItem>()
        for (item in mAdjustList){
            list.add(item.propertyItem)
        }
        return list
    }

    private fun setCurrentState(){
        var stateInfo = "当前调整属性强度 \n"
        for (item in mAdjustList){
            stateInfo += "${resources.getString(item.titleIds)}  "
            for (p in 0 until item.params.size){
                stateInfo += "${resources.getString(item.params[p].titleId)} : ${String.format("%.2f",item.propertyItem.values[p])}  "
            }
            stateInfo +="\n"
        }

        lsq_editor_current_state.setText(stateInfo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        for (item in mAdjustList){
            item.resetValue()
        }
    }
}