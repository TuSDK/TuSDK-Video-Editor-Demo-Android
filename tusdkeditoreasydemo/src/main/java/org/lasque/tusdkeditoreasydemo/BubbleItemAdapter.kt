/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2021/4/13$ 10:04$
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
import kotlinx.android.synthetic.main.bubble_list_item_view.view.*
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter
import org.lasque.tusdkeditoreasydemo.base.BubbleItem
import org.lasque.tusdkpulse.core.TuSdkContext

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/4/13  10:04
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class BubbleItemAdapter(itemList: MutableList<BubbleItem>, context: Context) : BaseAdapter<BubbleItem, BubbleItemAdapter.BubbleItemViewHolder>(itemList, context) {

    class BubbleItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val icon = itemView.lsq_bubble_icon
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): BubbleItemViewHolder {
        return BubbleItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.bubble_list_item_view,parent,false))
    }

    override fun onChildBindViewHolder(holder: BubbleItemViewHolder, position: Int, item: BubbleItem) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder, item)
        }

        holder.icon.setImageBitmap(TuSdkContext.getRawBitmap(item.icon))
    }

}