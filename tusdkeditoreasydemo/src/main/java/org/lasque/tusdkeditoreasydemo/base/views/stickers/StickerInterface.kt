/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base.views.stickers$
 *  @author  H.ys
 *  @Date    2020/11/13$ 10:27$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views.stickers

import android.graphics.Bitmap
import android.graphics.Rect
import com.tusdk.pulse.editor.VideoEditor
import org.lasque.tusdkeditoreasydemo.base.EditorPlayerContext
import java.util.concurrent.ExecutorService

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views.stickers
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/13  10:27
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */

public interface OnStickerLayerItemViewListener{
    fun onItemClose(itemView : LayerItemViewBase)

    fun onItemSelected(itemView : LayerItemViewBase)

    fun onItemReleased(itemView : LayerItemViewBase)
}

public interface StickerLayerItemViewInterface{
    fun setCurrentLayerType(type : LayerType)

    fun getCurrentLayerType() : LayerType

    fun setStickerLayerType(type : LayerType)

    fun getStickerLayerType() : LayerType

    fun setSelected(isSelected : Boolean)

    fun setStroke(color : Int,width : Int)

    fun setParentFrame(frame : Rect)

    fun setParentRect(rect : Rect)

    fun setListener(listener: OnStickerLayerItemViewListener)

    fun setEditor(editor: VideoEditor)

    fun setPlayerContext(context : EditorPlayerContext)

    fun createLayer()

    fun setThreadPool(threadPool : ExecutorService)

    fun resize(frame : Rect)
}

public interface LayerViewDelegate{
    fun onItemViewSelected(type: LayerType,view : LayerItemViewBase)

    fun onItemViewReleased(type: LayerType,view: LayerItemViewBase)

    fun onItemUnselected(type: LayerType)
}

public enum class LayerType{
    Image,Text,Video,ImageAndVideo;
}