/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/9$ 17:49$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.transitions_item_layout.view.*
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/9  17:49
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class TransitionsAdapter(itemList: MutableList<String>, context: Context) : BaseAdapter<String, TransitionsAdapter.TransitionsViewHolder>(itemList, context) {

    public class TransitionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.lsq_transitions_icon
        val title = itemView.lsq_transitions_title
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): TransitionsViewHolder {
        return TransitionsViewHolder(LayoutInflater.from(mContext).inflate(R.layout.transitions_item_layout, parent, false))
    }

    override fun onChildBindViewHolder(holder: TransitionsViewHolder, position: Int, item: String) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder,item)
        }
        if (TextUtils.isEmpty(item)){
            holder.title.setText("无")
        } else {
            val transitionCode = item.toLowerCase().replace("-","")
            val iconName = getThumbPrefix() + transitionCode
            val iconId = TuSdkContext.getRawResId(iconName)

            //设置图片圆角角度
            val roundedCorners = RoundedCorners(TuSdkContext.dip2px(8f))
            val requestOption = RequestOptions.bitmapTransform(roundedCorners).override(holder.icon.width,holder.icon.height)
            Glide.with(mContext).asGif().load(iconId).apply(requestOption).into(holder.icon)
            holder.title.setText(TuSdkContext.getString(getTextPrefix() + transitionCode))

        }
        if (position == mCurrentPos){
            holder.title.setTextColor(Color.RED)
            holder.icon.setColorFilter(Color.parseColor("#99000000"))
        } else {
            holder.title.setTextColor(Color.WHITE)
            holder.icon.clearColorFilter()
        }



    }

    /**
     * 缩略图前缀
     *
     * @return
     */
    fun getThumbPrefix(): String {
        return "lsq_filter_thumb_"
    }

    /**
     * Item名称前缀
     *
     * @return
     */
    fun getTextPrefix(): String {
        return "lsq_filter_"
    }

    fun findTransitions(code : String){
        for (i in 0 until mItemList.size){
            if (mItemList[i].equals(code)){
                setCurrentPosition(i)
                break
            }
        }
    }
}