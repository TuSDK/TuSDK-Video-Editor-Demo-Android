/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/12/22$ 16:29$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.video_item_layout.view.*
import org.lasque.tusdkeditoreasydemo.apis.VideoItemAdapter
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter
import org.lasque.tusdkeditoreasydemo.base.VideoItem

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/12/22  16:29
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class LayerListAdapter(itemList: MutableList<LayerItem>, context: Context) : BaseAdapter<LayerItem, LayerListAdapter.LayerViewHolder>(itemList, context) {

    public interface OnClipChangeListener{
        fun onSwap(item0 : LayerItem, item1 : LayerItem)
    }

    private var mClipChangeListener : OnClipChangeListener? = null


    class LayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mId = itemView.lsq_video_item_id
        val mIcon = itemView.lsq_video_item_icon
        val mMoveUp = itemView.lsq_video_item_move_up
        val mMoveDown = itemView.lsq_video_item_move_down
        val mRemove = itemView.lsq_video_item_remove
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        return LayerViewHolder(LayoutInflater.from(mContext).inflate(R.layout.video_item_layout, parent, false))
    }

    override fun onChildBindViewHolder(holder: LayerViewHolder, position: Int, item: LayerItem) {
        holder.mId.text = item.id.toString()
        Glide.with(mContext).load(item.path).into(holder.mIcon)
        if (position == 0){
            holder.mMoveUp.visibility = View.INVISIBLE
        } else {
            holder.mMoveUp.visibility = View.VISIBLE
        }
        if (position == mItemList.size - 1){
            holder.mMoveDown.visibility = View.INVISIBLE
        } else {
            holder.mMoveDown.visibility = View.VISIBLE
        }
        holder.mRemove.visibility = View.INVISIBLE

        holder.mMoveUp.setOnClickListener {
            mClipChangeListener?.onSwap(mItemList[position-1],mItemList[position])
            val up = mItemList[position - 1]
            mItemList[position - 1] = mItemList[position]
            mItemList[position] = up
            notifyDataSetChanged()
        }

        holder.mMoveDown.setOnClickListener {
            mClipChangeListener?.onSwap(mItemList[position],mItemList[position + 1])
            val down = mItemList[position + 1]
            mItemList[position + 1] = mItemList[position]
            mItemList[position] = down
            notifyDataSetChanged()
        }
    }

    public fun setOnClipChangeListener(listener: OnClipChangeListener){
        this.mClipChangeListener = listener
    }

    public fun refreshItems(itemList: MutableList<LayerItem>){
        mItemList = itemList
        notifyDataSetChanged()
    }
}