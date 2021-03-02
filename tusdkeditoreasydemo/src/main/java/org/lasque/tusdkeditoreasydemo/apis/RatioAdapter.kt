/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/10$ 15:36$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.ratio_item_layout.view.*
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/10  15:36
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class RatioAdapter(itemList: MutableList<VideoRatio>, context: Context) : BaseAdapter<VideoRatio, RatioAdapter.RatioViewHolder>(itemList, context) {

    public class RatioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.lsq_ratio_icon
        val title = itemView.lsq_ratio_title
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): RatioViewHolder {
        return RatioViewHolder(LayoutInflater.from(mContext).inflate(R.layout.ratio_item_layout, parent, false))
    }

    override fun onChildBindViewHolder(holder: RatioViewHolder, position: Int, item: VideoRatio) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder, item)
        }
        holder.title.setText(item.titleId)
        if (mCurrentPos == position){
            Glide.with(mContext).load(item.selIconId).into(holder.icon)
            holder.title.setTextColor(Color.parseColor("#ffcc00"))
        } else {
            Glide.with(mContext).load(item.iconId).into(holder.icon)
            holder.title.setTextColor(Color.WHITE)
        }
    }
}