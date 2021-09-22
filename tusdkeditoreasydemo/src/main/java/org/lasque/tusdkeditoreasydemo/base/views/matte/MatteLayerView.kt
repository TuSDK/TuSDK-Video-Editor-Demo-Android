/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.base.views.matte$
 *  @author  H.ys
 *  @Date    2021/8/24$ 11:02$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views.matte

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.VideoEditor
import com.tusdk.pulse.editor.effects.VideoMatteEffect
import org.lasque.tusdkeditoreasydemo.base.EditorPlayerContext
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views.matte
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/8/24  11:02
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class MatteLayerView : FrameLayout,OnMatteItemViewListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    private val mMatteLinkedList : LinkedList<MatteView> = LinkedList()

    private var mParentRect : Rect = Rect()

    private var mCurrentMatte : MatteView? = null

    private var mDelegate : MatteLayerViewDelegate? = null

    fun setDelegate(delegate: MatteLayerViewDelegate){
        mDelegate = delegate
    }

    fun resize(videoSize : Rect){
//        mParentRect = videoSize
//        val size = TuSdkSize.create(videoSize)
//        val layoutParams = layoutParams
//
//        layoutParams.width = size.width
//        layoutParams.height = size.height
//
//        for (view in mMatteLinkedList){
//            view.setParentRect(videoSize)
//            view.resize(TuSdkViewHelper.locationInWindow(this))
//        }
    }

    override fun onItemSelected(itemView: MatteView) {
        mCurrentMatte = itemView
    }

    fun removeMatte(view : MatteView){
        val res = mThreadPool?.submit(Callable<Boolean> {
//            view.removeMatte()
            mPlayerContext?.refreshFrame()
            true
        })
        res?.get()
        removeView(view)
        mMatteLinkedList.remove(view)
    }

    fun addMatte(type : VideoMatteEffect.MatteType,parentClip : Clip){
        val view = buildMatteView(type, parentClip)
    }

    fun restoreMatte(parentClip : Clip){
        val view = restoreMatteView(parentClip)
    }

    private fun buildMatteView(type : VideoMatteEffect.MatteType,parentClip : Clip) : MatteView{
        val view = MatteView(context)

        val rect = TuSdkViewHelper.locationInWindow(this)


        view.setEditor(mEditor!!)
        view.setThreadPool(mThreadPool!!)
        view.setPlayerContext(mPlayerContext!!)
        val layoutParams = FrameLayout.LayoutParams(rect.width(),rect.height())
        view.layoutParams = layoutParams
        addView(view)
        val res = mThreadPool?.submit(Callable<Boolean> {
            view.changeMatte(type,parentClip)
            mPlayerContext?.refreshFrame()
            true
        })

        res?.get()
        mMatteLinkedList.add(view)
        return view
    }

    private fun restoreMatteView(parentClip: Clip){
        val view = MatteView(context)
        view.setEditor(mEditor!!)
        view.setThreadPool(mThreadPool!!)
        view.setPlayerContext(mPlayerContext!!)
        val res = mThreadPool?.submit(Callable<Boolean> {
            view.restoreMatte(parentClip)
            mPlayerContext?.refreshFrame()
            true
        })

        res?.get()
        addView(view)
        mMatteLinkedList.add(view)
    }



    /** ------------------------------------------- Editor ------------------------------------------------------ */

    private var mEditor: VideoEditor? = null

    private var mThreadPool: ExecutorService? = null

    private var mPlayerContext: EditorPlayerContext? = null

    public fun setEditor(editor: VideoEditor) {
        mEditor = editor
    }

    public fun setThreadPool(threadPool: ExecutorService) {
        mThreadPool = threadPool
    }

    public fun setPlayerContext(context: EditorPlayerContext) {
        mPlayerContext = context
    }




}