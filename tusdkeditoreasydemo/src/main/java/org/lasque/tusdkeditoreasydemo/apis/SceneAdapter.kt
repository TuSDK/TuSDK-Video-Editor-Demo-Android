/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/11/5$ 10:57$
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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.scene_item_layout.view.*
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/5  10:57
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class SceneAdapter(itemList: MutableList<String>, context: Context) : BaseAdapter<String, SceneAdapter.SceneViewHolder>(itemList, context) {

    public class SceneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.lsq_scene_icon
        val title = itemView.lsq_scene_title
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): SceneViewHolder {
        return SceneViewHolder(LayoutInflater.from(mContext).inflate(R.layout.scene_item_layout, parent, false))
    }

    override fun onChildBindViewHolder(holder: SceneViewHolder, position: Int, item: String) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder, item)
        }

        val sceneCode = item.toLowerCase()
        val screenImageName = getThumbPrefix() + sceneCode

        val screenId = TuSdkContext.getRawResId(screenImageName)

        //设置图片圆角角度
        val roundedCorners = RoundedCorners(TuSdkContext.dip2px(8f))
        val requestOption =RequestOptions.bitmapTransform(roundedCorners).override(holder.icon.width,holder.icon.height)
        Glide.with(mContext).asGif().load(screenId).apply(requestOption).into(holder.icon)
        holder.title.setText(TuSdkContext.getString(getTextPrefix() + sceneCode))
        if (position == mCurrentPos){
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

    public fun findScene(code : String){
        for (i in 0 until mItemList.size){
            if (mItemList[i].equals(code)){
                setCurrentPosition(i)
                break
            }
        }
    }
}