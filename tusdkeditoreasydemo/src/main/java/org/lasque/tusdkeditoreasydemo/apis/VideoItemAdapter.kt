/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2020/10/27$ 14:07$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.VideoEditor
import com.tusdk.pulse.editor.clips.VideoFileClip
import kotlinx.android.synthetic.main.video_item_layout.view.*
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.base.BaseAdapter
import org.lasque.tusdkeditoreasydemo.base.VideoItem

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/27  14:07
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class VideoItemAdapter(itemList: MutableList<VideoItem>, context: Context) : BaseAdapter<VideoItem, VideoItemAdapter.VideoViewHolder>(itemList, context) {

    public interface OnClipChangeListener{

        fun onSwap(item0 : VideoItem,item1 : VideoItem)

        fun onRemove(item0 : VideoItem,pos : Int)
    }

    private var mClipChangeListener : OnClipChangeListener? = null


    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val mId = itemView.lsq_video_item_id
        val mIcon = itemView.lsq_video_item_icon
        val mMoveUp = itemView.lsq_video_item_move_up
        val mMoveDown = itemView.lsq_video_item_move_down
        val mRemove = itemView.lsq_video_item_remove
    }

    override fun onChildCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(LayoutInflater.from(mContext).inflate(R.layout.video_item_layout,parent,false))
    }

    override fun onChildBindViewHolder(holder: VideoViewHolder, position: Int, item: VideoItem) {
        holder.mId.text = item.mId.toString()
        var startPos = item.mVideoClip.config.getIntNumberOr(VideoFileClip.CONFIG_TRIM_START,0)
        startPos *= 1000
        Glide.with(mContext)
                .setDefaultRequestOptions(RequestOptions().frame(startPos.toLong()))
                .load(item.mPath)
                .into(holder.mIcon)
        if (position == 0){
            holder.mMoveUp.visibility = View.INVISIBLE
        } else {
            holder.mMoveUp.visibility = View.VISIBLE
        }
        if (position == mItemList.size - 1){
            holder.mMoveDown.visibility = View.INVISIBLE
        } else {
            holder.mMoveDown.visibility = View.VISIBLE
        }
        if (mItemList.size == 1){
            holder.mRemove.visibility = View.INVISIBLE
        } else {
            holder.mRemove.visibility = View.VISIBLE
        }


        holder.mMoveUp.setOnClickListener {
            mClipChangeListener?.onSwap(mItemList[position-1],mItemList[position])
            val up = mItemList[position - 1]
            mItemList[position - 1] = mItemList[position]
            mItemList[position] = up
            notifyDataSetChanged()
        }

        holder.mMoveDown.setOnClickListener {
            mClipChangeListener?.onSwap(mItemList[position],mItemList[position + 1])
            val down = mItemList[position + 1]
            mItemList[position + 1] = mItemList[position]
            mItemList[position] = down
            notifyDataSetChanged()
        }

        holder.mRemove.setOnClickListener {
            mClipChangeListener?.onRemove(item,position)
            mItemList.remove(item)
            notifyDataSetChanged()
        }
    }

    public fun setOnClipChangeListener(listener: OnClipChangeListener){
        this.mClipChangeListener = listener
    }
}