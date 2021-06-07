/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.base.views.stickers$
 *  @author  H.ys
 *  @Date    2021/4/12$ 16:52$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views.stickers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.tusdk.pulse.Config
import com.tusdk.pulse.Property
import com.tusdk.pulse.VideoStreamInfo
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.BubbleTextClip
import org.jetbrains.anko.runOnUiThread
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.base.views.TuSdkImageButton
import org.lasque.tusdkeditoreasydemo.utils.Constants
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.utils.RectHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.view.TuSdkImageView
import java.util.concurrent.ExecutionException
import kotlin.math.max

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views.stickers
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/4/12  16:52
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class BubbleLayerItemView : LayerItemViewBase{
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /** -------------------------------------------------- View ------------------------------------------------------ */

    companion object{
        const val BUBBLE_CLIP_ID = 300

        private const val Message_Update_Text = 100

        private const val Message_Double_Click = 200

        fun getLayoutId() : Int{
            return TuSdkContext.getLayoutResId("tusdk_impl_component_widget_bubble_text_item_view")
        }
    }

    private var mBubbleLayerViewDelegate : BubbleLayerViewDelegate? = null

    private var mImageView : TuSdkImageView? = null

    private var mCancelButton : TuSdkImageButton? = null

    private var mTurnButton : TuSdkImageButton? = null

    private var mStrokeWidth = 1

    private var mStrokeColor = Color.TRANSPARENT

    private var mHandler : Handler? = null

    private var mHandlerThread : HandlerThread? = null;

    private var willShowKeyBoard = false


    private val mButtonClickListener = object : OnClickListener {
        override fun onClick(v: View?) {
            if (equalViewIds(v, getCancelButton())) {
                handleCancelButton()
            }
        }

    }

    /** 图片视图  */
    fun getImageView(): TuSdkImageView? {
        if (mImageView == null) {
            mImageView = this.getViewById("lsq_sticker_imageView")
        }
        return mImageView
    }

    /** 取消按钮  */
    fun getCancelButton(): TuSdkImageButton {
        if (mCancelButton == null) {
            mCancelButton = this.getViewById("lsq_sticker_cancelButton")
            if (mCancelButton != null) {
                mCancelButton!!.setOnClickListener(mButtonClickListener)
            }
        }
        return mCancelButton!!
    }

    /** 旋转缩放按钮  */
    @SuppressLint("ClickableViewAccessibility")
    fun getTurnButton(): TuSdkImageButton {
        if (mTurnButton == null) {
            mTurnButton = this.getViewById("lsq_sticker_turnButton")
            if (mTurnButton != null) {
                mTurnButton!!.setOnTouchListener(mOnTouchListener)
            }
        }
        return mTurnButton!!
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (mCurrentType != LayerType.ImageAndVideo) {
            if (mCurrentLayerType != mCurrentType)
                false
            else {
                super.onTouchEvent(event)
            }
        } else super.onTouchEvent(event)
    }

    override fun setStroke(color: Int, width: Int) {
        mStrokeColor = color
        mStrokeWidth = max(width, 0)
        getImageView()?.setStroke(mStrokeColor, mStrokeWidth)
    }

    private fun handleCancelButton() {
        mListener?.onItemClose(this)

        mThreadPool?.execute {
            mEditor!!.player.lock()
            mEditor!!.videoComposition().deleteLayer(mCurrentLayerId)
            mEditor!!.player.unlock()
            mPlayerContext?.refreshFrame()
        }
    }

    private var mStartPointPercent: PointF? = null
    private var mStartPoint: PointF? = null

    override fun initView() {
        super.initView()
        mHandlerThread = HandlerThread(this.toString() + "LongClick")
        mHandlerThread?.start()
        mHandler = Handler(mHandlerThread!!.looper,object : android.os.Handler.Callback{
            override fun handleMessage(msg: Message): Boolean {
                if (msg.what == Message_Update_Text){
                    TLog.e("Message_Update_Text")
                    mBubbleLayerViewDelegate?.onBubbleUpdateText(this@BubbleLayerItemView,msg.arg1)
                    return true
                }
                if (msg.what == Message_Double_Click){
//                    willShowKeyBoard = false
                    return true
                }
                return false
            }

        })
    }

    override fun handleTransActionStart(event: MotionEvent) {
        super.handleTransActionStart(event)

        if (event.pointerCount == 1){
            val startX = event.x
            val startY = event.y
            mStartPoint = PointF(startX,startY)
            mStartPointPercent = PointF(startX / width,startY / height)
            val index = isHasText(mStartPointPercent!!)
            if (index > -1){
                if (willShowKeyBoard){
                    val msg = mHandler!!.obtainMessage()
                    msg.arg1 = index
                    msg.what = Message_Update_Text
                    mHandler!!.sendMessage(msg)
                } else {
                    willShowKeyBoard = true
//                    val msg = mHandler!!.obtainMessage()
//                    msg.arg1 = index
//                    msg.what = Message_Double_Click
//                    mHandler!!.sendMessageDelayed(msg,200)
                    TLog.e("Message_Update_Text send message")
                }
            }
        }
    }

    override fun loadView() {
        super.loadView()
        // 图片视图
        getImageView()
        // 取消按钮
        getCancelButton()
        // 旋转缩放按钮
        getTurnButton()
    }

    override fun setSelected(selected: Boolean) {

        val color = if (selected) {
            mStrokeColor
        } else {
            willShowKeyBoard = selected
            Color.TRANSPARENT
        }
        getImageView()?.setStroke(color, mStrokeWidth)
        showViewIn(getCancelButton(), selected)
        showViewIn(getTurnButton(), selected)

        for (view in mTextRectView){
            showViewIn(view,selected)
        }
    }

    fun setBubbleDelegate(delegate: BubbleLayerViewDelegate?){
        mBubbleLayerViewDelegate = delegate
    }

    /** ----------------------------- Clip ------------------------------ */

    private var mCurrentClip : Clip? = null

    private var mCurrentClipConfig : Config? = null

    private var mCurrentBubblePath = ""

    private var mBubblePropertyBuilder = BubbleTextClip.PropertyBuilder()

    private var mTextsPos = ArrayList<BubbleTextClip.InteractionInfo.BubbleTextItem>()

    private val mTextRectView = ArrayList<View>()

    override fun restoreClip(layer: ClipLayer) {
        var bubbleClip = layer.allClips[BUBBLE_CLIP_ID]
        val bubbleClipConfig = bubbleClip?.config
        mClipDuration = bubbleClipConfig?.getIntNumber(BubbleTextClip.CONFIG_DURATION)!!
        mCurrentClip = bubbleClip
        mCurrentClipConfig = bubbleClipConfig

        mClipStartPos = 0
        mClipEndPos = mClipDuration
    }

    override fun restoreLayer(id: Int) {
        super.restoreLayer(id)
        mStrokeWidth = ContextUtils.dip2px(context,2f)
        mStrokeColor = Color.WHITE

        mThreadPool?.execute {
            val targetView = this
            val posProperty = BubbleTextClip.InteractionInfo(mCurrentClip!!.getProperty(BubbleTextClip.PROP_INTERACTION_INFO))
            val streamInfo = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo)
            val hp = streamInfo.width / mParentFrame.width().toFloat()
            mTextsPos = posProperty.items

            var property : Property? = mCurrentClip!!.getProperty(BubbleTextClip.PROP_PARAM)

            val bubbleHolder = if (property == null) BubbleTextClip.PropertyHolder() else BubbleTextClip.PropertyHolder(property)
            mBubblePropertyBuilder.holder = bubbleHolder

            if (mBubblePropertyBuilder.holder.texts.isEmpty()){
                for (i in 0 until mTextsPos.size){
                    mBubblePropertyBuilder.holder.texts.add(mTextsPos[i].text)
                }
            }

            context.runOnUiThread {
                val oWidth = (posProperty.width / hp).toInt()
                val oHeight = (posProperty.height / hp ).toInt()
                val width = (posProperty.width / hp).toInt() + ContextUtils.dip2px(targetView.context, 24f)
                val height = (posProperty.height / hp).toInt() + ContextUtils.dip2px(targetView.context, 24f)

                mDefaultViewSize = TuSdkSize(width, height)
                mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F, 0f, posProperty.width.toFloat(), posProperty.height.toFloat())
                val parentSize = TuSdkSize.create(mParentRect)
                mMaxScale = parentSize.maxSide().toFloat() / mDefaultViewSize.minSide()
                val pX = posProperty.posX * mParentFrame.width()
                val pY = posProperty.posY * mParentFrame.width()
                x = (pX - (width / 2f)).toFloat()
                y = (pY - (height / 2f)).toFloat()
                setViewSize(targetView, width, height)
                setStroke(Color.WHITE, ContextUtils.dip2px(context, 2f))

                mDegree = mBubblePropertyBuilder.holder.rotate.toFloat()
                mScale = mBubblePropertyBuilder.holder.scale.toFloat()
                for (i in mTextsPos.indices) {
                    val vRect = mTextsPos[i].rect
                    val vWidth = (vRect.width() * oWidth).toInt()
                    val vHeight = (vRect.height() * oHeight).toInt()
                    val v = View(context)
                    v.background = context.getDrawable(R.drawable.bubble_dash_line)
                    val layoutParams = RelativeLayout.LayoutParams(vWidth,vHeight)
                    layoutParams.width = vWidth
                    layoutParams.height = vHeight
                    val marginLeft = (vRect.left * oWidth).toInt() + ContextUtils.dip2px(targetView.getContext(), 13f)
                    val marginTop = (vRect.top * oHeight).toInt() + ContextUtils.dip2px(targetView.getContext(), 13f)
                    TLog.e("rect index ${i} width = ${vWidth} height = ${vHeight} rect = ${vRect} mar left = ${marginLeft} mar top = ${marginTop}")
                    layoutParams.topMargin = marginTop
                    layoutParams.leftMargin = marginLeft
                    v.layoutParams = layoutParams
                    targetView.addView(v, i)
                    mTextRectView.add(v)
                }
                rotation = mDegree
                isLayout = true
                isSelected = false
                mTranslation = PointF(x, y)
                requestLayout()
                mThreadPool?.execute {
                    mPlayerContext?.refreshFrame()
                }
            }
        }




    }

    override fun createLayer() {
        super.createLayer()
//        updateProperty()
        val view = this
        val posProperty = BubbleTextClip.InteractionInfo(mCurrentClip!!.getProperty(BubbleTextClip.PROP_INTERACTION_INFO))
        val streamInfo = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo)
        val hp = streamInfo.width / mParentFrame.width().toFloat()
        mTextsPos = posProperty.items
        if (mBubblePropertyBuilder.holder.texts.isEmpty()){
            for (i in 0 until mTextsPos.size){
                mBubblePropertyBuilder.holder.texts.add("")
            }
        }
        val targetView: BubbleLayerItemView = this@BubbleLayerItemView
        context.runOnUiThread {
            val oWidth = (posProperty.width / hp).toInt()
            val oHeight = (posProperty.height / hp ).toInt()
            val width = (posProperty.width / hp).toInt() + ContextUtils.dip2px(view.context, 24f)
            val height = (posProperty.height / hp).toInt() + ContextUtils.dip2px(view.context, 24f)

            mDefaultViewSize = TuSdkSize(width, height)
            mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F, 0f, posProperty.width.toFloat(), posProperty.height.toFloat())
            val parentSize = TuSdkSize.create(mParentRect)
            mMaxScale = parentSize.maxSide().toFloat() / mDefaultViewSize.minSide()
            val pX = posProperty.posX * mParentFrame.width()
            val pY = posProperty.posY * mParentFrame.width()
            x = (pX - (width / 2f)).toFloat()
            y = (pY - (height / 2f)).toFloat()
            setViewSize(view, width, height)
            setStroke(Color.WHITE, ContextUtils.dip2px(context, 2f))
            isLayout = true
            isSelected = true
            mTranslation = PointF(x, y)
            for (i in mTextsPos.indices) {
                val vRect = mTextsPos[i].rect
                val vWidth = (vRect.width() * oWidth).toInt()
                val vHeight = (vRect.height() * oHeight).toInt()
                val v = View(context)
                v.background = context.getDrawable(R.drawable.bubble_dash_line)
                val layoutParams = RelativeLayout.LayoutParams(vWidth,vHeight)
                layoutParams.width = vWidth
                layoutParams.height = vHeight
                val marginLeft = (vRect.left * oWidth).toInt() + ContextUtils.dip2px(targetView.getContext(), 13f)
                val marginTop = (vRect.top * oHeight).toInt() + ContextUtils.dip2px(targetView.getContext(), 13f)
                TLog.e("rect index ${i} width = ${vWidth} height = ${vHeight} rect = ${vRect} mar left = ${marginLeft} mar top = ${marginTop}")
                layoutParams.topMargin = marginTop
                layoutParams.leftMargin = marginLeft
                v.layoutParams = layoutParams
                targetView.addView(v, i)
                mTextRectView.add(v)
            }

            requestLayout()
            mThreadPool?.execute {
                mPlayerContext?.refreshFrame()
            }
        }

    }

    override fun updateViewSize() {
        val view = this
        val posProperty = BubbleTextClip.InteractionInfo(mCurrentClip!!.getProperty(BubbleTextClip.PROP_INTERACTION_INFO))
        val streamInfo = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo)
        val hp = streamInfo.width / mParentFrame.width().toFloat()
        mTextsPos = posProperty.items
        val targetView: BubbleLayerItemView = this@BubbleLayerItemView
        context.runOnUiThread {
            val oWidth = (posProperty.width / hp ).toInt()
            val oHeight = (posProperty.height / hp ).toInt()
            val width = (posProperty.width / hp ).toInt() + ContextUtils.dip2px(view.context, 24f)
            val height = (posProperty.height / hp).toInt() + ContextUtils.dip2px(view.context, 24f)

            mDefaultViewSize = TuSdkSize(width, height)
            mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F, 0f, posProperty.width.toFloat(), posProperty.height.toFloat())
            val parentSize = TuSdkSize.create(mParentRect)
            mMaxScale = parentSize.maxSide().toFloat() / mDefaultViewSize.minSide()
            val pX = posProperty.posX * mParentFrame.width()
            val pY = posProperty.posY * mParentFrame.width()
            x = (pX - (width / 2f)).toFloat()
            y = (pY - (height / 2f)).toFloat()
            setViewSize(view, width, height)
            mTranslation = PointF(x, y)
            for (i in mTextsPos.indices) {
                val vRect = mTextsPos[i].rect
                val vWidth = (vRect.width() * oWidth).toInt()
                val vHeight = (vRect.height() * oHeight).toInt()
                val v = mTextRectView.get(i)
                val layoutParams = v.layoutParams as RelativeLayout.LayoutParams
                layoutParams.width = vWidth
                layoutParams.height = vHeight
                val marginLeft = (vRect.left * oWidth).toInt() + ContextUtils.dip2px(targetView.getContext(), 13f)
                val marginTop = (vRect.top * oHeight).toInt() + ContextUtils.dip2px(targetView.getContext(), 13f)
                layoutParams.topMargin = marginTop
                layoutParams.leftMargin = marginLeft
                v.layoutParams = layoutParams
            }
            requestLayout()
        }
    }

    override fun createClip(layer: ClipLayer) {
        val bubbleClip = Clip(mEditor!!.context,BubbleTextClip.TYPE_NAME);
        val bubbleClipConfig = Config()

        val ttfPath = context.getSharedPreferences("TU-TTF",Context.MODE_PRIVATE).getString(Constants.BUBBLE_TTF_KEY,"");
        bubbleClipConfig.setString(BubbleTextClip.CONFIG_MODEL,mCurrentBubblePath)
        bubbleClipConfig.setString(BubbleTextClip.CONFIG_FONT_DIR,ttfPath)
        bubbleClipConfig.setNumber(BubbleTextClip.CONFIG_DURATION,mClipDuration)
        bubbleClip.setConfig(bubbleClipConfig)
        if (!bubbleClip.activate()) return
        if (!layer.addClip(BUBBLE_CLIP_ID,bubbleClip)){

        }

        mCurrentClip = bubbleClip
        mCurrentClipConfig = bubbleClipConfig
        mClipStartPos = 0
        mClipEndPos = mClipDuration
        mClipMaxDuration = mClipDuration
    }

    override fun setClipDuration(start: Long, end: Long) {
        //        val end = max(1000,end)
        mClipDuration = end - start

        mLayerEndPos = end
        mLayerStartPos = start

        mLayerEndPos = mLayerStartPos + mClipDuration

        mEditor!!.player?.lock()
        if (mClipDuration != 0L) {
            mCurrentClipConfig?.setNumber(BubbleTextClip.CONFIG_DURATION, mClipDuration)
            mCurrentClip?.setConfig(mCurrentClipConfig)
        }

        mCurrentLayerConfig?.setNumber(Layer.CONFIG_START_POS,start)
        mCurrentLayer?.setConfig(mCurrentLayerConfig)

        if (!mEditor!!.build()) {
            TLog.e("Editor reBuild failed")
            throw Exception()
        }
        mEditor!!.player?.unlock()
    }

    fun setBubblePath(path : String){
        mCurrentBubblePath = path
    }

    private fun isHasText(p: PointF): Int {
        if (mTextsPos == null || mTextsPos.isEmpty()) return -1
        var res = false
        for (i in mTextsPos.indices) {
            val rf = mTextsPos[i].rect
            res = rf.contains(p.x, p.y)
            if (res) return i
        }
        return -1
    }

    fun getTextByIndex(index: Int): String? {
        return mBubblePropertyBuilder.holder.texts.get(index)
    }

    override fun updateProperty() {
        super.updateProperty()
        mCurrentClip?.setProperty(BubbleTextClip.PROP_PARAM,mBubblePropertyBuilder.makeProperty())
        mPlayerContext?.refreshFrame()
    }

    public fun updateText(textIndex: Int,text : String){
        val res = mThreadPool!!.submit<Boolean> {
            mBubblePropertyBuilder.holder.texts.set(textIndex, text)
            updateProperty()
            updateViewSize()
            true
        }

        try {
            res.get()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun updateRotate() {
        mBubblePropertyBuilder.holder.rotate = mDegree.toDouble()
    }

    override fun updatePan(cPointInLayer: PointF) {
        mBubblePropertyBuilder.holder.posX = cPointInLayer.x.toDouble()
        mBubblePropertyBuilder.holder.posY = cPointInLayer.y.toDouble()
    }


    override fun updateZoom() {
        mBubblePropertyBuilder.holder.scale = mScale.toDouble()
    }
}