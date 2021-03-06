/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base.views.stickers$
 *  @author  H.ys
 *  @Date    2020/11/16$ 17:15$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views.stickers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.tusdk.pulse.Config
import com.tusdk.pulse.VideoStreamInfo
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.ImageClip
import com.tusdk.pulse.editor.clips.Text2DClip
import org.jetbrains.anko.runOnUiThread
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.utils.RectHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.view.TuSdkTextView
import org.lasque.tusdkeditoreasydemo.base.views.TuSdkImageButton
import org.lasque.tusdkeditoreasydemo.utils.Constants
import kotlin.math.max

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views.stickers
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/16  17:15
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class TextLayerItemView : LayerItemViewBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /** -------------------------------------------------- View ------------------------------------------------------ */

    companion object {

        const val TEXT_CLIP_ID = 200

        fun getLayoutId(): Int {
            return TuSdkContext.getLayoutResId("tusdk_impl_component_widget_sticker_text_item_view")
        }
    }


    /** 文字视图  */
    private var mTextView: TuSdkTextView? = null

    /** 取消按钮  */
    private var mCancelButton: TuSdkImageButton? = null

    /** resize 按钮  */
    private var mResizeButton: TuSdkImageButton? = null

    /** 旋转缩放按钮  */
    private var mTurnButton: TuSdkImageButton? = null

    private var isLeft = true

    private val mButtonClickListener = View.OnClickListener { v ->
        if (equalViewIds(v, getCancelButton())) {
            handleCancelButton()
        }
    }

    private val mOnResizeButtonTouchListener = object : OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            if (event.pointerCount > 1) return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    handleTurnAndScaleActionStart(null, event.rawX, event.rawY)
                }
                MotionEvent.ACTION_MOVE -> {
                    handleTurnAndScaleActionMove(null, event.rawX, event.rawY)
                }
            }
            return true
        }

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

    /** 文字视图  */
    fun getTextView(): TuSdkTextView? {
        if (mTextView == null) {
            mTextView = this.getViewById("lsq_sticker_textView")
            mTextView!!.getPaint().isAntiAlias = true
            mTextView!!.getPaint().isDither = true
        }
        return mTextView
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

    /** resize 按钮  */
    fun getResizeButton(): TuSdkImageButton {
        if (mResizeButton == null) {
            mResizeButton = this.getViewById("lsq_sticker_resizeButton")
            if (mResizeButton != null) {
                mResizeButton!!.setOnTouchListener(mOnResizeButtonTouchListener)
            }
        }
        return mResizeButton!!
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

    override fun loadView() {
        super.loadView()
        getTextView()
        getCancelButton()
        getTurnButton()
    }

    /************************** Stroke  */
    /** 边框宽度  */
    private var mStrokeWidth = 0

    /** 边框颜色  */
    private var mStrokeColor = 0


    override fun setStroke(color: Int, width: Int) {
        mStrokeColor = color
        mStrokeWidth = max(width, 0)
        getTextView()?.setStroke(mStrokeColor, mStrokeWidth)
    }

    /** ----------------------------------- Clip -------------------------------------  */

    private var mCurrentClip: Clip? = null

    private var mTextClipConfig: Config = Config()

    private var mTextPropertyBuilder = Text2DClip.PropertyBuilder()

    private var mCurrentString: String = ""

    private val mDefaultString: String = "请输入文字"

    override fun createClip(layer: ClipLayer) {
        val textClip = Clip(mEditor!!.context, Text2DClip.TYPE_NAME)
        TLog.e("duration ${mClipDuration}")
        mTextClipConfig.setNumber(Text2DClip.CONFIG_DURATION, mClipDuration)
        textClip.setConfig(mTextClipConfig)
        if (!layer.addClip(TEXT_CLIP_ID, textClip)) {

        }
        mCurrentClip = textClip
    }

    override fun createLayer() {
        super.createLayer()
        //            mTextPropertyBuilder.font = "android_asset://SourceHanSansSC-Normal.ttf"
        mTextPropertyBuilder.holder.font = context.getSharedPreferences("TU-TTF",Context.MODE_PRIVATE).getString(Constants.TTF_KEY,"")
        mTextPropertyBuilder.holder.fillColor = Color.parseColor("#ffffff")
        mTextPropertyBuilder.holder.text = mDefaultString
        mTextPropertyBuilder.holder.strokeWidth = 0.0
        mTextPropertyBuilder.holder.bgColor = Color.parseColor("#00FFFFFF")
        mCurrentClip?.setProperty(Text2DClip.PROP_PARAM, mTextPropertyBuilder.makeProperty())
        val view = this
        val posProperty = Text2DClip.InteractionInfo(mCurrentClip!!.getProperty(Text2DClip.PROP_INTERACTION_INFO))
        val streamInfo = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo)
        val hp = streamInfo.width / mParentFrame.width().toFloat()
        context.runOnUiThread {
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
            requestLayout()
            mThreadPool?.execute {
                mPlayerContext?.refreshFrame()
            }
        }
    }

    override fun restoreLayer(id: Int) {
        super.restoreLayer(id)
        mThreadPool?.execute {
            val textHolder = Text2DClip.PropertyHolder(mCurrentClip!!.getProperty(Text2DClip.PROP_PARAM))
            mTextPropertyBuilder.holder = textHolder
            mCurrentString = mTextPropertyBuilder.holder.text
            val view = this
            val posProperty = Text2DClip.InteractionInfo(mCurrentClip!!.getProperty(Text2DClip.PROP_INTERACTION_INFO))
            val streamInfo = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo)
            val hp = streamInfo.width / mParentFrame.width().toFloat()
            context.runOnUiThread {
                val width = (posProperty.width / hp).toInt() + ContextUtils.dip2px(view.context, 24f)
                val height = (posProperty.height / hp).toInt() + ContextUtils.dip2px(view.context, 24f)
                mDefaultViewSize = TuSdkSize(width, height)
                mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F, 0f, posProperty.width.toFloat(), posProperty.height.toFloat())
                val pX = posProperty.posX * mParentFrame.width()
                val pY = posProperty.posY * mParentFrame.width()
                x = (pX - (width / 2f)).toFloat()
                y = (pY - (height / 2f)).toFloat()
                setViewSize(view, width, height)
                mStrokeWidth = ContextUtils.dip2px(context,2f)
                mStrokeColor = Color.WHITE
                mDegree = posProperty.rotation.toFloat()
                mScale = mTextPropertyBuilder.holder.fontScale.toFloat()
                TLog.e("current Zoom ${mScale}")
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

    override fun restoreClip(layer: ClipLayer) {
        val textClip = layer.getClip(TEXT_CLIP_ID)
        mTextClipConfig = textClip.config
        mCurrentClip = textClip
    }

    override fun setClipDuration(start: Long, end: Long) {
        mClipDuration = end - start
        mLayerStartPos = start
        mLayerEndPos = end
        mEditor!!.player.lock()
        mTextClipConfig.setNumber(Text2DClip.CONFIG_DURATION, mClipDuration)
        mCurrentClip?.setConfig(mTextClipConfig)

        mCurrentLayerConfig?.setNumber(Layer.CONFIG_START_POS, start)
        mCurrentLayer?.setConfig(mCurrentLayerConfig)
        if (!mEditor!!.build()) {
            TLog.e("Editor reBuild failed")
            throw Exception()
        }
        mEditor!!.player.unlock()

    }

    fun setString(string: String) {
        mCurrentString = string
    }

    fun getString(): String {
        return mCurrentString
    }

    override fun updatePan(cPointInLayer: PointF) {
        mTextPropertyBuilder.holder.posX = cPointInLayer.x.toDouble()
        mTextPropertyBuilder.holder.posY = cPointInLayer.y.toDouble()
    }

    override fun updateRotate() {
        mTextPropertyBuilder.holder.rotate = mDegree.toDouble()
    }

    override fun updateZoom() {
        mTextPropertyBuilder.holder.fontScale = mScale.toDouble()
    }

    fun updateFont(path: String) {
        mTextPropertyBuilder.holder.font = path
        updateProperty()
    }

    fun updateText(text: String) {
        mCurrentString = text
        mTextPropertyBuilder.holder.text = if (TextUtils.isEmpty(text)){mDefaultString} else {text}
        updateProperty()
        mThreadPool?.execute {
            val view = this
            val posProperty = Text2DClip.InteractionInfo(mCurrentClip!!.getProperty(Text2DClip.PROP_INTERACTION_INFO))
            val streamInfo = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo)
            val hp = streamInfo.width / mParentFrame.width().toFloat()
            context.runOnUiThread {
//                getTextView()?.setText(text)
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
                mTranslation = PointF(x, y)
                requestLayout()
            }

        }
    }

    fun updateLineSpacing(lineSpacing: Double) {
        mTextPropertyBuilder.holder.textScaleY = lineSpacing
        updateProperty()
    }

    fun updateWordSpacing(wordSpecing: Double) {
        mTextPropertyBuilder.holder.textScaleX = wordSpecing
        updateProperty()
    }

    fun updateStrokeWidth(width: Double) {
        mTextPropertyBuilder.holder.strokeWidth = width
        updateProperty()
    }

    fun updateStrokeColor(color: Int) {
        mTextPropertyBuilder.holder.strokeColor = color
        updateProperty()
    }

    fun updateFontColor(color: Int) {
        mTextPropertyBuilder.holder.fillColor = color
        updateProperty()
    }

    fun updateBackgroundColor(color: Int) {
        mTextPropertyBuilder.holder.bgColor = color
        updateProperty()
    }

    fun updateBackgroundAlpha(alpha: Int) {
        val color = mTextPropertyBuilder.holder.bgColor
        mTextPropertyBuilder.holder.bgColor = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        updateProperty()
    }

    fun updateTextAlpha(alpha: Double) {
        mResizeProperty.holder.opacity = alpha
        mCurrentLayer?.setProperty(Layer.PROP_OVERLAY, mResizeProperty.makeProperty())
        mPlayerContext?.refreshFrame()
    }

    fun getTextAlpha() : Double{
        return mResizeProperty.holder.opacity
    }

    fun getTextColor() : Int{
        return mTextPropertyBuilder.holder.fillColor
    }

    fun getBackgroundColor() : Int{
        return mTextPropertyBuilder.holder.bgColor
    }

    fun isUnderLine() : Boolean{
       return mTextPropertyBuilder.holder.style == Text2DClip.Style.UNDERLINE
    }

    fun updateTextAlign(align: Text2DClip.Alignment) {
        mTextPropertyBuilder.holder.alignment = align
        updateProperty()
    }

    fun textReverse(isLeft : Boolean) {
        if (isLeft == this.isLeft) return
        var text = mTextPropertyBuilder.holder.text
        text = reverseString(text)
        mTextPropertyBuilder.holder.text = text
        mCurrentString = text
        updateProperty()
        this.isLeft = isLeft
    }

    fun isTextReverse() : Boolean {
        return isLeft
    }

    /** 将字符串反转并返回  */
    private fun reverseString(text: String): String? {
        val stringBuilder = StringBuilder()
        val result = text.split("\n".toRegex()).toTypedArray()
        for (i in result.indices) {
            stringBuilder.append(StringBuilder(result[i]).reverse())
            if (i < result.size - 1) stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }

    fun updateTextStyle(style: Int) {
        mTextPropertyBuilder.holder.style = style
        updateProperty()
    }

    fun getCurrentText(): String {
        return mCurrentString
    }


    override fun updateProperty() {
        super.updateProperty()
        mCurrentClip?.setProperty(Text2DClip.PROP_PARAM, mTextPropertyBuilder.makeProperty())
        mPlayerContext?.refreshFrame()
    }

    override fun setSelected(selected: Boolean) {
        if (!isLayout) return
        val color = if (selected) {
            mStrokeColor
        } else {
            Color.TRANSPARENT
        }
        getTextView()?.setStroke(color, mStrokeWidth)
        showViewIn(getCancelButton(), selected)
        showViewIn(getTurnButton(), selected)
    }

    override fun updateViewSize(){
        val posProperty = Text2DClip.InteractionInfo(mCurrentClip!!.getProperty(Text2DClip.PROP_INTERACTION_INFO))
        val streamInfo = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo)
        val hp = streamInfo.width / mParentFrame.width().toFloat()
        val lastSize = mDefaultViewSize.copy()

        context.runOnUiThread {
            val width = (posProperty.width / hp).toInt() + ContextUtils.dip2px(context, 24f)
            val height = (posProperty.height / hp).toInt() + ContextUtils.dip2px(context, 24f)
            mDefaultViewSize = TuSdkSize(width,height)
            val scaleSize = mDefaultViewSize
            val pX = posProperty.posX * mParentFrame.width()
            val pY = posProperty.posY * mParentFrame.width()
            x = (pX - (width / 2f)).toFloat()
            y = (pY - (height / 2f)).toFloat()
            mTranslation.set(x,y)
            setViewSize(this@TextLayerItemView, width, height)
            x = mTranslation.x
            y = mTranslation.y
        }
    }


}