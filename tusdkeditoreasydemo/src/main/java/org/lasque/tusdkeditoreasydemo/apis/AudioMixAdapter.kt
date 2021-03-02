/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/26$ 14:37$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import com.tusdk.pulse.editor.Layer
import kotlinx.android.synthetic.main.audio_mix_item_layout.view.*
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.base.AudioLayerItem
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter
import java.util.concurrent.ExecutorService

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/26  14:37
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AudioMixAdapter(itemList: MutableList<AudioLayerItem>, context: Context,threadPool : ExecutorService) : BaseAdapter<AudioLayerItem, AudioMixAdapter.AudioMixViewHolder>(itemList, context) {

    private val mThreadPool = threadPool

    class AudioMixViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.lsq_mix_item_title
        val mixBar = itemView.lsq_mix_weight_bar
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): AudioMixViewHolder {
        return AudioMixViewHolder(LayoutInflater.from(mContext).inflate(R.layout.audio_mix_item_layout,parent,false))
    }

    override fun onChildBindViewHolder(holder: AudioMixViewHolder, position: Int, item: AudioLayerItem) {
        holder.mixBar.setOnSeekBarChangeListener(object  : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (!p2) return
                mThreadPool.execute {
                    val mixied = p1 / 100.0
                    item.layerMixPropertyBuilder.holder.weight = mixied
                    item.audioLayer.setProperty(Layer.PROP_MIX,item.layerMixPropertyBuilder.makeProperty())
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

        })

        holder.title.setText("副音轨${position + 1}\n(0~1)")
        holder.mixBar.progress = (item.layerMixPropertyBuilder.holder.weight * 100).toInt()
    }
}