/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/10/22$ 16:51$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.api_item_layout.view.*
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper.OnSafeClickListener

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/22  16:51
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class ApiAdapter(itemList: MutableList<FunctionType>, context: Context) : BaseAdapter<FunctionType, ApiAdapter.ApiViewHolder>(itemList, context) {

    class ApiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): ApiViewHolder {
        return ApiViewHolder(LayoutInflater.from(mContext).inflate(R.layout.api_item_layout,parent,false))
    }

    override fun onChildBindViewHolder(holder: ApiViewHolder, position: Int, item: FunctionType) {
        holder.itemView.setOnClickListener(object : OnSafeClickListener(500) {
            override fun onSafeClick(v: View?) {
                mOnItemClickListener?.onItemClick(position, holder, item)
            }
        })
        if (item.mTitleId != -1){
            holder.itemView.lsq_api_item_title.setText(item.mTitleId)
        }
    }
}