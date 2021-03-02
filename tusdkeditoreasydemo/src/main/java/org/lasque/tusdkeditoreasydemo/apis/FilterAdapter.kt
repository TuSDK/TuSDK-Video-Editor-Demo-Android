/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/10/29$ 14:00$
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
import kotlinx.android.synthetic.main.filter_item_layout.view.*
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.seles.tusdk.FilterOption
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/29  14:00
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class FilterAdapter(itemList: MutableList<FilterOption>, context: Context) : BaseAdapter<FilterOption, FilterAdapter.FilterViewHolder>(itemList, context) {

    public class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.lsq_filter_icon
        val title = itemView.lsq_filter_title
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
       return FilterViewHolder(LayoutInflater.from(mContext).inflate(R.layout.filter_item_layout,parent,false))
    }

    override fun onChildBindViewHolder(holder: FilterViewHolder, position: Int, item: FilterOption) {
        val filterCode = item.code
        val imageCode = filterCode.toLowerCase().replace("_","")
        val filterImageName = getThumbPrefix() + imageCode
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder,item)
        }
        if (position == mCurrentPos){
            holder.icon.setColorFilter(Color.parseColor("#66CE4564"))
        } else {
            holder.icon.clearColorFilter()
        }
        Glide.with(mContext).load(TuSdkContext.getRawBitmap(filterImageName)).into(holder.icon)
        holder.title.setText(TuSdkContext.getString(getTextPrefix() + filterCode))
//        holder.title.setTextColor(Color.parseColor("#FFCE4564"))
    }

    /**
     * 缩略图前缀
     *
     * @return
     */
    protected fun getThumbPrefix(): String? {
        return "lsq_filter_thumb_"
    }

    /**
     * Item名称前缀
     *
     * @return
     */
    protected fun getTextPrefix(): String? {
        return "lsq_filter_"
    }

    public fun findFilter(code : String){
        for (i in 0 until mItemList.size){
            if (mItemList[i].code.equals(code)){
                setCurrentPosition(i)
                break;
            }
        }
    }
}