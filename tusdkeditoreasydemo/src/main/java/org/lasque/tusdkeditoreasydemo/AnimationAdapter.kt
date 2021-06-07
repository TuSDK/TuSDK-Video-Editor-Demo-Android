/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2021/5/10$ 11:01$
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
import kotlinx.android.synthetic.main.animation_item_layout.view.*
import org.lasque.tusdkeditoreasydemo.base.AnimationItem
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/5/10  11:01
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AnimationAdapter(itemList : MutableList<AnimationItem>,context : Context) : BaseAdapter<AnimationItem, AnimationAdapter.AnimationViewHolder>(itemList, context){

    class AnimationViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        val name = itemView.lsq_animation_name
        val thumb = itemView.lsq_animation_thumb

    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): AnimationViewHolder {
        return AnimationViewHolder(LayoutInflater.from(mContext).inflate(R.layout.animation_item_layout,parent,false))
    }

    override fun onChildBindViewHolder(holder: AnimationViewHolder, position: Int, item: AnimationItem) {
        holder.itemView.setOnClickListener {
            mOnItemClickListener?.onItemClick(position,holder,item)
        }

        holder.name.setText(item.getName())
        if (item.itemIndex == 0){
            Glide.with(mContext).asBitmap().load(item.getThumbId()).into(holder.thumb)
        } else {
            Glide.with(mContext).asGif().load(item.getThumbId()).into(holder.thumb)
        }


        if (mCurrentPos == position){
            holder.name.setTextColor(Color.RED)
        } else {
            holder.name.setTextColor(Color.WHITE)
        }
    }

    fun findAnimation(index : Int){
        for (i in 0 until itemCount){
            if (mItemList[i].itemIndex == index){
                setCurrentPosition(i)
                break
            }
        }
    }

    fun findAnimation(path : String){
        for (i in 0 until itemCount){
            if (mItemList[i].getAnimationFilePath().equals(path)){
                setCurrentPosition(i)
                break
            }
        }
    }
}