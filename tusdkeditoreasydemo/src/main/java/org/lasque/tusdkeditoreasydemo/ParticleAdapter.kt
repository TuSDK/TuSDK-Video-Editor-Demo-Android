/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/11/11$ 14:54$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.particle_item_layout.view.*
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/11  14:54
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class ParticleAdapter(itemList: MutableList<String>, context: Context) : BaseAdapter<String, ParticleAdapter.ParticleViewHolder>(itemList, context) {

    public class ParticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.lsq_particle_icon
        val title = itemView.lsq_particle_title
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): ParticleViewHolder {
        return ParticleViewHolder(LayoutInflater.from(mContext).inflate(R.layout.particle_item_layout,parent,false))
    }

    override fun onChildBindViewHolder(holder: ParticleViewHolder, position: Int, item: String) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder, item)
        }
        val magicCode = item.toLowerCase()
        val magicImageCode = getThumbPrefix() + magicCode
        //设置图片圆角角度
        val roundedCorners = RoundedCorners(TuSdkContext.dip2px(8f))
        val requestOption = RequestOptions.bitmapTransform(roundedCorners).override(holder.icon.width,holder.icon.height)
        Glide.with(mContext).load(TuSdkContext.getRawBitmap(magicImageCode))
                .apply(requestOption)
                .into(holder.icon)
        holder.title.setText(TuSdkContext.getString(getTextPrefix() + magicCode))
        if (mCurrentPos == position){
            holder.icon.setColorFilter(Color.parseColor("#99000000"))
        } else {
            holder.icon.clearColorFilter()
        }
    }

    /**
     * 缩略图前缀
     *
     * @return
     */
    protected fun getThumbPrefix(): String {
        return "lsq_filter_thumb_"
    }

    /**
     * Item名称前缀
     *
     * @return
     */
    protected fun getTextPrefix(): String {
        return "lsq_filter_"
    }
}