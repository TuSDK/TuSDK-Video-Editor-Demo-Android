package org.lasque.tusdkeditoreasydemo.base.views

import android.graphics.Rect
import com.tusdk.pulse.editor.VideoEditor
import org.lasque.tusdkeditoreasydemo.base.EditorPlayerContext
import java.util.concurrent.ExecutorService

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/5/17  15:10
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
interface EffectInterface {

    fun setSelected(isSelected : Boolean)

    fun setStroke(color : Int,width : Int)

    fun setParentFrame(frame : Rect)

    fun setParentRect(rect : Rect)

    fun setEditor(editor : VideoEditor)

    fun setPlayerContext(context : EditorPlayerContext)

    fun createEffect()

    fun restoreEffect(id : Int)

    fun setThreadPool(threadPool : ExecutorService)

    fun resize(frame : Rect)

    fun setListener(listener : OnEffectItemViewListener)
}

interface OnEffectItemViewListener{

    fun onItemSelected(itemView : EffectItemView)

    fun onItemUnselected(itemView: EffectItemView)

    fun onItemClose(itemView: EffectItemView)
}

interface EffectLayerViewDelegate{
    fun onItemViewSelected(type: EffectType,view : EffectItemView)
    fun onItemViewUnselected(type: EffectType,view: EffectItemView)
    fun onItemViewClose(type: EffectType,view: EffectItemView)
}

public enum class EffectType{
    Mosaic
}