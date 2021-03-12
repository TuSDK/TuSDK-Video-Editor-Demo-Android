/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/12/14$ 18:34$
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
import kotlinx.android.synthetic.main.draft_item_layout.view.*
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter
import org.lasque.tusdkeditoreasydemo.base.DraftItem
import org.lasque.tusdkeditoreasydemo.base.OnItemDeleteClickListener

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/12/14  18:34
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class DraftAdapter(itemList: MutableList<DraftItem>, context: Context) : BaseAdapter<DraftItem, DraftAdapter.DraftViewHolder>(itemList, context) {

    private var mItemDeleteClickListener : OnItemDeleteClickListener<DraftItem,DraftViewHolder>? = null

    class DraftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val funcTitle = itemView.lsq_draft_func_type
        val path = itemView.lsq_draft_path
        val date = itemView.lsq_draft_date
        val delete = itemView.lsq_draft_delete
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): DraftViewHolder {
        return DraftViewHolder(LayoutInflater.from(mContext).inflate(R.layout.draft_item_layout, parent, false))
    }

    override fun onChildBindViewHolder(holder: DraftViewHolder, position: Int, item: DraftItem) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position, holder, item)
        }

        holder.delete.setOnClickListener {
            mItemDeleteClickListener?.onItemDelete(position,holder,item)
        }
        val func = FunctionType.values()[item.funcType]
        holder.funcTitle.setText(func.mTitleId)

        holder.path.setText(item.fileName)
        holder.date.setText(item.saveDate)
    }

    public fun updateDraftList(itemList: MutableList<DraftItem>){
        mItemList = itemList
        notifyDataSetChanged()
    }

    public fun setOnItemDeleteClickListener(itemDeleteClickListener: OnItemDeleteClickListener<DraftItem,DraftViewHolder>){
        mItemDeleteClickListener = itemDeleteClickListener
    }
}