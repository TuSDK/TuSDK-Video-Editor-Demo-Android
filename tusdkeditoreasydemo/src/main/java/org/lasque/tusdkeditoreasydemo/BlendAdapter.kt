/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/11/25$ 13:34$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.blend_item_layout.view.*
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/25  13:34
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class BlendAdapter(itemList: MutableList<String>, context: Context) : BaseAdapter<String, BlendAdapter.BlendViewHolder>(itemList, context) {

    class BlendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.lsq_blend_name
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): BlendViewHolder {
        return BlendViewHolder(LayoutInflater.from(mContext).inflate(R.layout.blend_item_layout,parent,false))
    }

    override fun onChildBindViewHolder(holder: BlendViewHolder, position: Int, item: String) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder, item)
        }

        val magicCode = item.toLowerCase()

        if (TextUtils.isEmpty(item)){
            holder.name.setText("无")

        } else {
            holder.name.setText(TuSdkContext.getString(getTextPrefix() + magicCode))

        }
        if (mCurrentPos == position){
            holder.name.setTextColor(Color.RED)
        } else {
            holder.name.setTextColor(Color.WHITE)
        }
    }

    /**
     * Item名称前缀
     *
     * @return
     */
    protected fun getTextPrefix(): String {
        return "lsq_filter_"
    }

    public fun findMode(mode : String){
        val index = mItemList.indexOf(mode)
        setCurrentPosition(index)
    }

}