/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/4$ 11:21$
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
import kotlinx.android.synthetic.main.mv_item_layout.view.*
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.modules.view.widget.sticker.StickerGroup
import org.lasque.tusdkpulse.modules.view.widget.sticker.StickerLocalPackage
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/4  11:21
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class MVItemAdapter(itemList: MutableList<MVFragment.MvItem>, context: Context) : BaseAdapter<MVFragment.MvItem, MVItemAdapter.MVViewHolder>(itemList, context) {

    public class MVViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.lsq_mv_icon
        val title = itemView.lsq_mv_title
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): MVViewHolder {
        return MVViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mv_item_layout, parent, false))
    }

    override fun onChildBindViewHolder(holder: MVViewHolder, position: Int, item: MVFragment.MvItem) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder, item)
        }
        StickerLocalPackage.shared().loadGroupThumb(item.stickerGroup, holder.icon)
        if (mCurrentPos == position) {
            holder.icon.setColorFilter(Color.parseColor("#99000000"))
        } else {
            holder.icon.clearColorFilter()
        }
        holder.title.setText(TuSdkContext.getString("lsq_mv_" + item.stickerGroup.name))
    }

    public fun findMV(groupId : Long){
        for (i in 0 until mItemList.size){
            if (mItemList[i].stickerGroup.groupId == groupId){
                setCurrentPosition(i)
                break
            }
        }
    }
}