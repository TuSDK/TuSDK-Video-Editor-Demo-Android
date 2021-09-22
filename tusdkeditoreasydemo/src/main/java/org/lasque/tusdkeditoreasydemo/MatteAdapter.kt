/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2021/8/26$ 16:03$
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
import kotlinx.android.synthetic.main.matte_item_layout.view.*
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter
import org.lasque.tusdkeditoreasydemo.base.views.matte.MatteItem

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/8/26  16:03
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class MatteAdapter(itemList: MutableList<MatteItem>, context: Context) :
    BaseAdapter<MatteItem, MatteAdapter.MatteViewHolder>(itemList, context) {

    class MatteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.lsq_matte_icon
        val title = itemView.lsq_matte_title
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): MatteViewHolder {
        return MatteViewHolder(LayoutInflater.from(mContext).inflate(R.layout.matte_item_layout,parent,false))
    }

    override fun onChildBindViewHolder(holder: MatteViewHolder, position: Int, item: MatteItem) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder,item)
        }

        holder.icon.setImageResource(item.iconId)
        holder.title.setText(item.titleId)

        if (getCurrentPosition() == position){
            holder.itemView.setBackgroundResource(R.drawable.lsq_matte_background)
        } else {
            holder.itemView.setBackgroundResource(0)
        }
    }
}