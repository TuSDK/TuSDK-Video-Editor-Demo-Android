/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/11/19$ 15:37$
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
import kotlinx.android.synthetic.main.text_item_layout.view.*
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/19  15:37
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class TextFunctionAdapter(itemList: MutableList<TextStickerActivity.Companion.TextFunction>, context: Context) : BaseAdapter<TextStickerActivity.Companion.TextFunction, TextFunctionAdapter.TextFunctionViewHolder>(itemList, context) {

    class TextFunctionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.lsq_text_icon
        val title = itemView.lsq_text_title
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): TextFunctionViewHolder {
        return TextFunctionViewHolder(LayoutInflater.from(mContext).inflate(R.layout.text_item_layout,parent,false))
    }

    override fun onChildBindViewHolder(holder: TextFunctionViewHolder, position: Int, item: TextStickerActivity.Companion.TextFunction) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder, item)
        }
        Glide.with(mContext).load(item.iconId).into(holder.icon)
        holder.title.setText(item.textId)
    }
}