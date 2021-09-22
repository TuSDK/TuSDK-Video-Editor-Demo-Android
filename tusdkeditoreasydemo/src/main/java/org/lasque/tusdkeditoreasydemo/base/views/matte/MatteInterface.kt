package org.lasque.tusdkeditoreasydemo.base.views.matte

import android.graphics.Rect
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.VideoEditor
import com.tusdk.pulse.editor.effects.VideoMatteEffect
import org.lasque.tusdkeditoreasydemo.base.EditorPlayerContext
import java.util.concurrent.ExecutorService

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views.matte
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/8/24  11:21
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
interface MatteInterface {

    fun setEditor(editor : VideoEditor)

    fun setPlayerContext(context : EditorPlayerContext)

    fun changeMatte(matteType : VideoMatteEffect.MatteType, parentClip : Clip)

    fun restoreMatte(parentClip : Clip)

    fun removeMatte(parentClip: Clip)

    fun setThreadPool(threadPool : ExecutorService)
}

interface OnMatteItemViewListener{

    fun onItemSelected(itemView : MatteView)


}

interface MatteLayerViewDelegate{

}