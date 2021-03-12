/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base.views$
 *  @author  H.ys
 *  @Date    2020/11/13$ 10:24$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views.stickers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.tusdk.pulse.editor.VideoEditor
import org.jetbrains.anko.centerInParent
import org.jetbrains.anko.runOnUiThread
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.view.TuSdkLinearLayout
import org.lasque.tusdkpulse.core.view.TuSdkRelativeLayout
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import org.lasque.tusdkeditoreasydemo.base.EditorPlayerContext
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.ThreadHelper
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import kotlin.collections.HashMap

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/13  10:24
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class StickerLayerView : FrameLayout, OnStickerLayerItemViewListener {
    constructor(context: Context?) : super(context!!) {
        clipChildren = false
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        clipChildren = false
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr) {
        clipChildren = false
    }

    private val mTextLinkedList: LinkedList<TextLayerItemView> = LinkedList()

    private val mImageLinkedList: LinkedList<ImageLayerItemView> = LinkedList()

    private val mVideoLinkedList: LinkedList<VideoLayerItemView> = LinkedList()

    private val mLayerMap = HashMap<Int,LayerItemViewBase>()

    private var mCurrentLayerType = LayerType.Image

    private var mParentRect: Rect = Rect()

    private var mCurrentSelectedView: LayerItemViewBase? = null

    private var mDelegate: LayerViewDelegate? = null

    public var isSelectedTwice = false

    public fun setLayerViewDelegate(delegate: LayerViewDelegate) {
        mDelegate = delegate;
    }

    public fun setCurrentLayerType(type: LayerType) {
        mCurrentLayerType = type
    }

    override fun onItemClose(itemView: LayerItemViewBase) {
        removeView(itemView)
        if (itemView == mCurrentSelectedView) {
            mCurrentSelectedView = null
        }
        mThreadPool?.execute {
            mPlayerContext?.refreshFrame()
        }
        mDelegate?.onItemUnselected(itemView.getCurrentLayerType())
    }

    override fun onItemSelected(itemView: LayerItemViewBase) {
        if (mCurrentLayerType == LayerType.ImageAndVideo) {
            if (itemView.getCurrentLayerType() != LayerType.Image && itemView.getCurrentLayerType() != LayerType.Video)
                return
        } else {
            if (itemView.getCurrentLayerType() != mCurrentLayerType) return
        }

        mCurrentSelectedView = itemView

        isSelectedTwice = false

        if (itemView.equals(mCurrentSelectedView)) {
            isSelectedTwice = true
        }

        for (view in mTextLinkedList) {
            view.isSelected = (view == itemView)
        }
        for (view in mImageLinkedList) {
            view.isSelected = (view == itemView)
        }
        for (view in mVideoLinkedList) {
            view.isSelected = (view == itemView)
        }

        mDelegate?.onItemViewSelected(itemView.getCurrentLayerType(), itemView)
    }

    override fun onItemReleased(itemView: LayerItemViewBase) {
        if (!isSelectedTwice) return

        mDelegate?.onItemViewReleased(itemView.getCurrentLayerType(), itemView)
    }

    public fun appendText(text: String, start: Long, end: Long) {
        val view = buildTextItemView(text, start, end)
        onItemSelected(view)

    }

    public fun appendImage(image: Bitmap, start: Long, end: Long) {
        val view = buildImageItemView(null, image, ImageLayerItemView.ImageType.Image, start, end)
        onItemSelected(view)
    }

    public fun appendImage(path: String, start: Long, end: Long) {
        val view = buildImageItemView(path, null, ImageLayerItemView.ImageType.Path, start, end)
        onItemSelected(view)
    }

    public fun appendVideo(path: String, start: Long, end: Long, needAudio: Boolean = true) {
        val view = buildVideoItemView(path, start, end, needAudio)
        onItemSelected(view)
    }

    public fun restoreImage(id : Long){
        restoreImageItemView(id)
    }

    public fun restoreVideo(id: Long){
        restoreVideoItemView(id)
    }

    public fun restoreText(id : Long){
        restoreTextItemView(id)
    }

    private fun restoreVideoItemView(id : Long){
        val view = TuSdkViewHelper.buildView<VideoLayerItemView>(context, getItemViewResId(LayerType.Video))
        view.setEditor(mEditor!!)
        view.setThreadPool(mThreadPool!!)
        view.setPlayerContext(mPlayerContext!!)
        view.setCurrentLayerType(LayerType.Video)
        view.setStickerLayerType(mCurrentLayerType)
        view.setListener(this)
        val arrays: IntArray = IntArray(2)
        getLocationInWindow(arrays)
        TLog.e("size ${arrays} location ${TuSdkViewHelper.locationInWindow(this)}")
        view.setParentFrame(TuSdkViewHelper.locationInWindow(this))
        view.setParentRect(mParentRect)
        view.setLayerMaps(mLayerMap)
        addView(view)
        view.restoreLayer(id.toInt())
        mVideoLinkedList.add(view)
    }

    private fun restoreImageItemView(id : Long){
        val view = TuSdkViewHelper.buildView<ImageLayerItemView>(context, getItemViewResId(LayerType.Image))
        view.setEditor(mEditor!!)
        view.setThreadPool(mThreadPool!!)
        view.setPlayerContext(mPlayerContext!!)
        view.setCurrentLayerType(LayerType.Image)
        view.setStickerLayerType(mCurrentLayerType)
        view.setListener(this)
        view.setParentFrame(TuSdkViewHelper.locationInWindow(this))
        view.setParentRect(mParentRect)
        view.setLayerMaps(mLayerMap)
        addView(view)
        view.restoreLayer(id.toInt())
        mImageLinkedList.add(view)
    }

    private fun restoreTextItemView(id : Long){
        val view = TuSdkViewHelper.buildView<TextLayerItemView>(context, getItemViewResId(LayerType.Text))
        view.setEditor(mEditor!!)
        view.setThreadPool(mThreadPool!!)
        view.setPlayerContext(mPlayerContext!!)
        view.setCurrentLayerType(LayerType.Text)
        view.setStickerLayerType(mCurrentLayerType)
        view.setListener(this)
        view.setParentFrame(TuSdkViewHelper.locationInWindow(this))
        view.setParentRect(mParentRect)
        view.setLayerMaps(mLayerMap)
        view.restoreLayer(id.toInt())
        addView(view)
        mTextLinkedList.add(view)
    }

    private fun buildVideoItemView(path: String, start: Long, end: Long, needAudio: Boolean): VideoLayerItemView {
        val view = TuSdkViewHelper.buildView<VideoLayerItemView>(context, getItemViewResId(LayerType.Video))
        view.setEditor(mEditor!!)
        view.setThreadPool(mThreadPool!!)
        view.setPlayerContext(mPlayerContext!!)
        view.setVideoPath(path)
        view.setCurrentLayerType(LayerType.Video)
        view.setStickerLayerType(mCurrentLayerType)
        view.setListener(this)
        view.setStroke(Color.WHITE, ContextUtils.dip2px(context, 2f))
        TLog.e("size ${TuSdkViewHelper.locationInWindow(this)}")
        view.setParentFrame(TuSdkViewHelper.locationInWindow(this))
        view.setParentRect(mParentRect)
        view.needAudio(needAudio)
        view.setLayerMaps(mLayerMap)
        val res = mThreadPool?.submit(Callable<Boolean> {
            view.setClipDuration(start, end)
            view.createLayer()
            mPlayerContext?.refreshFrame()
            true
        })
        res?.get()
        addView(view)

        mVideoLinkedList.add(view)
        return view
    }


    private fun buildTextItemView(text: String, start: Long, end: Long): TextLayerItemView {
        val view = TuSdkViewHelper.buildView<TextLayerItemView>(context, getItemViewResId(LayerType.Text))
        view.setEditor(mEditor!!)
        view.setThreadPool(mThreadPool!!)
        view.setPlayerContext(mPlayerContext!!)
        view.setString(text)
        view.setCurrentLayerType(LayerType.Text)
        view.setStickerLayerType(mCurrentLayerType)
        view.setListener(this)
        view.setParentFrame(TuSdkViewHelper.locationInWindow(this))
        view.setParentRect(mParentRect)
        view.setLayerMaps(mLayerMap)
        val res = mThreadPool?.submit(Callable<Boolean> {
            view.setClipDuration(start, end)
            view.createLayer()
            mPlayerContext?.refreshFrame()
            true
         })
        res?.get()
        addView(view)
        mTextLinkedList.add(view)
        return view
    }

    private fun buildImageItemView(path: String?, image: Bitmap?, imageType: ImageLayerItemView.ImageType, start: Long, end: Long): ImageLayerItemView {
        val view = TuSdkViewHelper.buildView<ImageLayerItemView>(context, getItemViewResId(LayerType.Image))
        view.setEditor(mEditor!!)
        view.setThreadPool(mThreadPool!!)
        view.setPlayerContext(mPlayerContext!!)
        when (imageType) {
            ImageLayerItemView.ImageType.Image -> {
                view.setImage(image!!)
            }
            ImageLayerItemView.ImageType.Path -> {
                view.setImage(path!!)
            }
        }
        view.setCurrentLayerType(LayerType.Image)
        view.setStickerLayerType(mCurrentLayerType)
        view.setListener(this)
        view.setStroke(Color.WHITE, ContextUtils.dip2px(context, 2f))
        view.setParentFrame(TuSdkViewHelper.locationInWindow(this))
        view.setParentRect(mParentRect)
        view.setLayerMaps(mLayerMap)
        val res = mThreadPool?.submit(Callable<Boolean> {
            view.setClipDuration(start, end)
            view.createLayer()
            mPlayerContext?.refreshFrame()
            true
        })
        res?.get()
        addView(view)
        mImageLinkedList.add(view)
        return view
    }

    private fun getItemViewResId(type: LayerType): Int {
        return when (type) {
            LayerType.Image -> {
                ImageLayerItemView.getLayoutId()
            }
            LayerType.Text -> {
                TextLayerItemView.getLayoutId()
            }
            LayerType.Video -> {
                VideoLayerItemView.getLayoutId()
            }
            else -> 0
        }
    }

    public fun cancelAllItemSelected() {
        for (view in mTextLinkedList) {
            view.isSelected = false
        }
        for (view in mImageLinkedList) {
            view.isSelected = false
        }
        for (view in mVideoLinkedList) {
            view.isSelected = false
        }
        mCurrentSelectedView = null
        mDelegate?.onItemUnselected(mCurrentLayerType)

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            cancelAllItemSelected()
        }
        return super.onTouchEvent(event)
    }

    public fun resize(videoSize: Rect) {
        mParentRect = videoSize
        val size = TuSdkSize(videoSize.width(), videoSize.height())
        val layoutParams = layoutParams

        layoutParams.width = size.width
        layoutParams.height = size.height

        for (view in mTextLinkedList) {
            view.setParentRect(videoSize)
            view.resize(TuSdkViewHelper.locationInWindow(this))
        }

        for (view in mImageLinkedList) {
            view.setParentRect(videoSize)
            view.resize(TuSdkViewHelper.locationInWindow(this))
        }
        for (view in mVideoLinkedList) {
            view.setParentRect(videoSize)
            view.resize(TuSdkViewHelper.locationInWindow(this))
        }
    }

    public fun removeAllSticker() {
        removeAllTexts()
        removeAllImages()
        removeAllVideos()
    }

    public fun removeAllTexts() {
        for (view in mTextLinkedList) {
            removeView(view)
            mEditor!!.videoComposition().deleteLayer(view.mCurrentLayerId)
        }
        mTextLinkedList.clear()
        mPlayerContext?.refreshFrame()
        mCurrentSelectedView = null
    }

    public fun removeAllImages() {
        for (view in mImageLinkedList) {
            removeView(view)
            mEditor!!.videoComposition().deleteLayer(view.mCurrentLayerId)
        }
        mImageLinkedList.clear()
        mPlayerContext?.refreshFrame()
        mCurrentSelectedView = null

    }

    public fun removeAllVideos() {
        for (view in mVideoLinkedList) {
            removeView(view)
            mEditor!!.videoComposition().deleteLayer(view.mCurrentLayerId)
            mEditor!!.audioComposition().deleteLayer(view.mCurrentLayerId)
        }
        mVideoLinkedList.clear()
        mPlayerContext?.refreshFrame()
        mCurrentSelectedView = null
    }

    public fun requestShower(frame: Long) {
        TLog.e("current frame ${frame}")
        for (view in mTextLinkedList) {
            view.requestShower(frame)
        }
        for (view in mImageLinkedList) {
            view.requestShower(frame)
        }
        for (view in mVideoLinkedList) {
            view.requestShower(frame)
        }
    }

    public fun swapLayer(idx0 : Int,idx1 : Int){
        val layer0 = mLayerMap[idx0]
        val layer1 = mLayerMap[idx1]

        if (layer0 == null || layer1 == null) return

        val index0 = indexOfChild(layer0)
        val index1 = indexOfChild(layer1)

        ThreadHelper.post {
            removeView(layer0)
            removeView(layer1)

            if (index0 < index1){
                addView(layer1,index0)
                addView(layer0,index1)
            } else {
                addView(layer0,index1)
                addView(layer1,index0)
            }

        }
        val id = layer0!!.mCurrentLayerId
        layer0!!.mCurrentLayerId = layer1!!.mCurrentLayerId
        layer1!!.mCurrentLayerId = id

        mLayerMap.put(idx0,layer1)
        mLayerMap.put(idx1,layer0)
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