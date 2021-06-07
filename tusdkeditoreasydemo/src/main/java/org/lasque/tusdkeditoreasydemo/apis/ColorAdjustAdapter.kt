/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/26$ 17:50$
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
import kotlinx.android.synthetic.main.color_adjust_item_layout_v1.view.*
import kotlinx.android.synthetic.main.color_adjust_item_layout_v1.view.lsq_adjust_item_move_down
import kotlinx.android.synthetic.main.color_adjust_item_layout_v1.view.lsq_adjust_item_move_up
import kotlinx.android.synthetic.main.color_adjust_item_layout_v1.view.lsq_adjust_item_title
import kotlinx.android.synthetic.main.color_adjust_item_layout_v1.view.lsq_adjust_params_title
import kotlinx.android.synthetic.main.color_adjust_item_layout_v1.view.lsq_color_adjust_params_bar
import kotlinx.android.synthetic.main.color_adjust_item_layout_v1.view.lsq_params_value
import kotlinx.android.synthetic.main.color_adjust_item_layout_v2.view.*
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter
import org.lasque.tusdkeditoreasydemo.base.ColorAdjustItem
import org.lasque.tusdkeditoreasydemo.base.VideoItem

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/26  17:50
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class ColorAdjustAdapter(itemList: MutableList<ColorAdjustItem>, context: Context) : BaseAdapter<ColorAdjustItem, ColorAdjustAdapter.ColorAdjustViewHolder>(itemList, context) {

    public interface OnParamsRefreshListener{
        fun onParamsRefresh()
    }

    public interface OnClipChangeListener{
        fun onSwap(item0 : ColorAdjustItem, item1 : ColorAdjustItem)
    }

    abstract class ColorAdjustViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.lsq_adjust_item_title
        val parmasTitle = itemView.lsq_adjust_params_title
        val parmasBar = itemView.lsq_color_adjust_params_bar
        val parmasValue = itemView.lsq_params_value

        val moveUp = itemView.lsq_adjust_item_move_up
        val moveDown = itemView.lsq_adjust_item_move_down
    }

    class ColorAdjustViewHolderV1(itemView: View) : ColorAdjustViewHolder(itemView) {

    }

    class ColorAdjustViewHolderV2(itemView: View) : ColorAdjustViewHolder(itemView) {
        val parmasTitleV2 = itemView.lsq_adjust_params_title_2
        val parmasBarV2 = itemView.lsq_color_adjust_params_bar_2
        val parmasValueV2 = itemView.lsq_params_value_2
    }

    public var mOnParamsRefreshListener : OnParamsRefreshListener? = null

    public var mOnClipChangeListener : OnClipChangeListener? = null

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): ColorAdjustViewHolder {
       return when(viewType){
            2->{
                return ColorAdjustViewHolderV2(LayoutInflater.from(mContext).inflate(R.layout.color_adjust_item_layout_v2,parent,false))
            }
           else -> {
               return ColorAdjustViewHolderV2(LayoutInflater.from(mContext).inflate(R.layout.color_adjust_item_layout_v1,parent,false))
           }
       }
    }

    override fun onChildBindViewHolder(holder: ColorAdjustViewHolder, position: Int, item: ColorAdjustItem) {
        holder.title.setText(item.titleIds)
        val params1 = item.params[0]
        holder.parmasTitle.setText(params1.titleId)
        if (position == 0){
            holder.moveUp.visibility = View.INVISIBLE
        } else {
            holder.moveUp.visibility = View.VISIBLE
        }
        if (position == mItemList.size - 1){
            holder.moveDown.visibility = View.INVISIBLE
        } else {
            holder.moveDown.visibility = View.VISIBLE
        }
        val maxValue = (params1.max - params1.min) * 100
        holder.parmasBar.max = maxValue.toInt()
        val currentValue = (params1.defaultValue - params1.min) * 100
        holder.parmasBar.progress = currentValue.toInt()
        holder.parmasValue.setText(String.format("%.2f",params1.defaultValue))
        holder.parmasBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (!p2) return
                var value = p1.toDouble() / 100
                value += params1.min
                item.propertyItem.values[0] = value
                params1.defaultValue = value
                holder.parmasValue.setText(String.format("%.2f",value))
                mOnParamsRefreshListener?.onParamsRefresh()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

        })
        holder.moveUp.setOnClickListener {
            val up = mItemList[position - 1]
            mItemList[position -1] = mItemList[position]
            mItemList[position] = up
            notifyDataSetChanged()
            mOnClipChangeListener?.onSwap(mItemList[position - 1],mItemList[position])
        }
        holder.moveDown.setOnClickListener {
            val down = mItemList[position + 1]
            mItemList[position + 1] = mItemList[position]
            mItemList[position] = down
            notifyDataSetChanged()
            mOnClipChangeListener?.onSwap(mItemList[position],mItemList[position + 1])
        }
        if (holder.itemViewType == 2){
            val params2 = item.params[1]
            val maxValue = (params2.max - params2.min) * 100
            val holderV2 = holder as ColorAdjustViewHolderV2
            val currentValue = (params2.defaultValue - params2.min) * 100
            holderV2.parmasBarV2.max = maxValue.toInt()
            holderV2.parmasBarV2.progress = currentValue.toInt()
            holderV2.parmasTitleV2.setText(params2.titleId)

            holderV2.parmasValueV2.setText(String.format("%.2f",params2.defaultValue))
            holderV2.parmasBarV2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    if (!p2) return
                    var value = p1.toDouble() / 100
                    value += params2.min
                    item.propertyItem.values[1] = value
                    params2.defaultValue = value
                    holder.parmasValueV2.setText(String.format("%.2f",value))
                    mOnParamsRefreshListener?.onParamsRefresh()
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {

                }

                override fun onStopTrackingTouch(p0: SeekBar?) {

                }

            })
        }
    }

    override fun getItemViewType(position: Int): Int {
        return mItemList[position].params.size
    }
}