/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.base.views$
 *  @author  H.ys
 *  @Date    2021/5/18$ 10:12$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.tusdk.pulse.editor.VideoEditor
import org.lasque.tusdkeditoreasydemo.base.EditorPlayerContext
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/5/18  10:12
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class EffectLayerView : FrameLayout,OnEffectItemViewListener{
    constructor(context: Context) : super(context){
        clipChildren = false
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs){
        clipChildren = false
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        clipChildren = false
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes){
        clipChildren = false
    }

    private val mMosaicLinkedList : LinkedList<MosaicItemView> = LinkedList()

    private var mParentRect : Rect = Rect()

    private var mCurrentSelectedView : EffectItemView? = null

    private var mDelegate : EffectLayerViewDelegate? = null

    fun setEffectLayerViewDelegate(delegate: EffectLayerViewDelegate){
        mDelegate = delegate
    }

    override fun onItemSelected(itemView: EffectItemView) {
        mCurrentSelectedView = itemView

        for (view in mMosaicLinkedList){
            view.isSelected = (view == itemView)
        }

        mDelegate?.onItemViewSelected(itemView.getItemEffectType(),itemView)
    }

    override fun onItemUnselected(itemView: EffectItemView) {
        mDelegate?.onItemViewUnselected(itemView.getItemEffectType(),itemView)
    }

    override fun onItemClose(itemView: EffectItemView) {
        removeView(itemView)
        mMosaicLinkedList.remove(itemView)
        val res = mThreadPool!!.submit {
            mEditor!!.player.lock()
            mEditor!!.videoComposition().effects().delete(itemView.mCurrentEffectIndex)
            mEditor!!.build()
            mEditor!!.player.unlock()
            mPlayerContext?.refreshFrame()
        }

        res.get()

        if (itemView == mCurrentSelectedView){
            mCurrentSelectedView = null
        }

        mThreadPool?.execute {
            mPlayerContext?.refreshFrame()
        }

        mDelegate?.onItemViewClose(itemView.getItemEffectType(),itemView)
    }

    fun cancelAllItemSelected(){
        for (view in mMosaicLinkedList){
            view.isSelected = false
        }

        if (mCurrentSelectedView != null){
            mDelegate?.onItemViewUnselected(mCurrentSelectedView!!.getItemEffectType(),mCurrentSelectedView!!)
        }
        mCurrentSelectedView = null
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            cancelAllItemSelected()
        }
        return super.onTouchEvent(event)
    }

    fun resize(videoSize : Rect){
        mParentRect = videoSize
        val size = TuSdkSize(videoSize.width(),videoSize.height())
        val layoutParams = layoutParams

        layoutParams.width = size.width
        layoutParams.height = size.height

        for (view in mMosaicLinkedList){
            view.setParentRect(videoSize)
            view.resize(TuSdkViewHelper.locationInWindow(this))
        }

    }

    fun hasEffect() : Boolean{
        return mMosaicLinkedList.size != 0
    }

    fun getLastEffectIndex() : Int{
        if (mMosaicLinkedList.isEmpty()) return -1
        else return mMosaicLinkedList.last.mCurrentEffectIndex
    }

    fun removeAllEffects(){
        removeAllMosaics()
    }

    fun removeEffect(index : Int){
        var targetView : EffectItemView? = null

        for (view in mMosaicLinkedList){
            if (view.mCurrentEffectIndex == index){
                targetView = view
                break
            }
        }

        if (targetView != null){
            removeView(targetView)
            mMosaicLinkedList.remove(targetView)
            val res = mThreadPool!!.submit {
                mEditor!!.player.lock()
                mEditor!!.videoComposition().effects().delete(targetView.mCurrentEffectIndex)
                mEditor!!.build()
                mEditor!!.player.unlock()
                mPlayerContext?.refreshFrame()
            }

            res.get()
        }
    }

    fun removeAllMosaics(){
        mEditor!!.player.lock()
        for (view in mMosaicLinkedList){
            removeView(view)
            mEditor!!.videoComposition().effects().delete(view.mCurrentEffectIndex)
        }

        mEditor!!.build()
        mEditor!!.player.unlock()

        mMosaicLinkedList.clear()
        mPlayerContext?.refreshFrame()
        mCurrentSelectedView = null
    }

    fun appendMosaic(start : Long,end : Long){
        val view = buildMosaicItemView(start, end)
        onItemSelected(view)
    }

    fun restoreMosaic(id:Int){
        val view = restoreMosaicItemView(id)
//        onItemSelected(view)
    }


    private fun restoreMosaicItemView(id : Int) : MosaicItemView{
        val view = TuSdkViewHelper.buildView<MosaicItemView>(context,getItemViewResId(EffectType.Mosaic))
        view.setEditor(mEditor!!)
        view.setThreadPool(mThreadPool!!)
        view.setPlayerContext(mPlayerContext!!)
        view.setListener(this)
        view.setStroke(Color.WHITE, ContextUtils.dip2px(context,2f))
        view.setParentRect(mParentRect)
        view.setParentFrame(TuSdkViewHelper.locationInWindow(this))
        val res = mThreadPool?.submit(Callable<Boolean> {
            view.restoreEffect(id)
            true
        })

        res?.get()
        view.isSelected = false
        addView(view)
        mMosaicLinkedList.add(view)
        return view
    }

    private fun buildMosaicItemView(start: Long,end: Long) : MosaicItemView{
        val view = TuSdkViewHelper.buildView<MosaicItemView>(context,getItemViewResId(EffectType.Mosaic))
        view.setEditor(mEditor!!)
        view.setThreadPool(mThreadPool!!)
        view.setPlayerContext(mPlayerContext!!)
        view.setListener(this)
        view.setParentRect(mParentRect)
        view.setParentFrame(TuSdkViewHelper.locationInWindow(this))
        val res = mThreadPool?.submit(Callable<Boolean>{
            mEditor!!.player.lock()
            view.setEffectPos(start, end)
            view.createEffect()
            mEditor!!.player.unlock()
            mPlayerContext?.refreshFrame()
            true
        })
        res?.get()
        addView(view)
        mMosaicLinkedList.add(view)
        return view
    }

    private fun getItemViewResId(type: EffectType) : Int{
        return when(type){
            EffectType.Mosaic -> {MosaicItemView.getLayoutId()}
        }
    }

    public fun requestShower(frame : Long){
        for (view in mMosaicLinkedList){
            view.requestShower(frame)
        }
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