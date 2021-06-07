/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.base.views$
 *  @author  H.ys
 *  @Date    2021/5/17$ 17:15$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.effects.MosaicEffect
import org.jetbrains.anko.runOnUiThread
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.utils.RectHelper
import org.lasque.tusdkpulse.core.view.TuSdkImageView
import java.util.concurrent.Callable
import java.util.concurrent.Future
import kotlin.math.max

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/5/17  17:15
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class MosaicItemView : EffectItemView {

    companion object{
        fun getLayoutId() : Int{
            return TuSdkContext.getLayoutResId("tusdk_impl_component_widget_effect_mosaic_item_view")
        }
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)


    protected var mMosaicProperty : MosaicEffect.PropertyBuilder = MosaicEffect.PropertyBuilder()

    private var mMosaicRect : MosaicEffect.MosaicRect = MosaicEffect.MosaicRect()



    /************************* view  */

    private var mImageView : TuSdkImageView? = null

    private var mCancelButton : TuSdkImageButton? = null

    private var mTurnButton : TuSdkImageButton? = null

    private var mStrokeWidth = 1

    private var mStrokeColor : Int = Color.TRANSPARENT

    private val mButtonClickListener : OnClickListener = object : OnClickListener{
        override fun onClick(v: View?) {
            if (equalViewIds(v,getCancelButton())){
                handleCancelButton()
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

    private fun handleCancelButton() {
        mListener?.onItemClose(this)

        mThreadPool?.execute {
            mEditor!!.player.lock()
            //todo 移除模糊rect
            mEditor!!.player.unlock()
            mPlayerContext?.refreshFrame()
        }
    }



    override fun updateRotate() {
        mMosaicRect.degree = mDegree
    }

    override fun updateZoom() {
//        mMosaicRect.scale = mScale
    }

    override fun updateRect(rect: RectF) {
        mMosaicRect.rectF.set(rect)
    }

    override fun updateProperty() {
        if (!mMosaicProperty.holder.mosaicRects.contains(mMosaicRect)){
            mMosaicProperty.holder.mosaicRects.add(mMosaicRect)
        }
        mCurrentEffect?.setProperty(MosaicEffect.RECT_PROP_PARAM,mMosaicProperty.makeRectProperty())
        mPlayerContext?.refreshFrame()
    }

    override fun setStroke(color: Int, width: Int) {
        mStrokeColor = color
        mStrokeWidth = max(width, 0)
        getImageView()?.setStroke(mStrokeColor, mStrokeWidth)
    }

    fun setEffectPos(start : Long,end : Long){
        mStartPos = start
        mEndPos = end
    }

    fun getStartPos() : Long{
        return mStartPos
    }

    fun getEndPos() : Long{
        return mEndPos
    }

    fun updateEffectPos(start: Long,end: Long){
        mStartPos = start
        mEndPos = end
        val res : Future<Boolean> = mThreadPool!!.submit(Callable<Boolean> {
            mEditor!!.player.lock()

            mCurrentConfig.setNumber(MosaicEffect.CONFIG_START_POS,start)
            mCurrentConfig.setNumber(MosaicEffect.CONFIG_END_POS,end)

            mCurrentEffect?.setConfig(mCurrentConfig)

            mEditor!!.build()

            mEditor!!.player.unlock()

            true
        })

        res.get()
    }



    override fun createEffect() {
        val mosaicEffect = Effect(mEditor!!.context,MosaicEffect.TYPE_NAME)
        mCurrentConfig.setNumber(MosaicEffect.CONFIG_START_POS,mStartPos)
        mCurrentConfig.setNumber(MosaicEffect.CONFIG_END_POS,mEndPos)
        mosaicEffect.setConfig(mCurrentConfig)
        mCurrentEffectIndex = EffectItemView.CURRENT_EFFECT_ID++
        mEditor!!.videoComposition().effects().add(mCurrentEffectIndex,mosaicEffect)
        mCurrentEffect = mosaicEffect

        mEditor!!.build()

        val view = this


        context.runOnUiThread {
            val centerPoint = PointF(0.5f,0.5f)
            val rWidth = 0.5
            val rHeight = 0.2

            mMosaicRect.rectF.set(0.25f,0.4f,0.75f,0.6f)

            val viewWidth = mParentFrame.width() * rWidth +  ContextUtils.dip2px(view.context,26f)
            val viewHeight = mParentFrame.height() * rHeight +  ContextUtils.dip2px(view.context,26f)

            val viewPx = centerPoint.x * mParentFrame.width()
            val viewPy = centerPoint.y * mParentFrame.height()

            x = (viewPx - (viewWidth / 2f)).toFloat()
            y = (viewPy - (viewHeight / 2f)).toFloat()

            view.layoutParams.width = viewWidth.toInt()
            view.layoutParams.height = viewHeight.toInt()

            mDefaultViewSize = TuSdkSize.create(viewWidth.toInt(), viewHeight.toInt())

            mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F,0f,viewWidth.toFloat(),viewHeight.toFloat())

            isLayout = true
            isSelected = true

            mTranslation = PointF(x,y)

            view.setStroke(Color.WHITE, ContextUtils.dip2px(context,2f))
            requestLayout()

            mThreadPool?.execute {
                updateProperty()
            }

        }

    }

    override fun restoreEffect(id: Int) {
        mThreadPool?.execute {
            mCurrentEffectIndex = id
            val effect = mEditor!!.videoComposition().effects()[id]
            val effectConfig = effect.config
            mStartPos = effectConfig.getIntNumber(MosaicEffect.CONFIG_START_POS)
            mEndPos = effectConfig.getIntNumber(MosaicEffect.CONFIG_END_POS)

            val property = effect.getProperty(MosaicEffect.RECT_PROP_PARAM)
            val mosaicHolder = MosaicEffect.PropertyHolder(property)

            mCurrentEffect = effect
            mCurrentConfig = effectConfig

            mMosaicRect = mosaicHolder.mosaicRects[0]

            val view = this
            context.runOnUiThread {
                val centerPoint = PointF(mMosaicRect.rectF.centerX(),mMosaicRect.rectF.centerY())
                val rWidth = mMosaicRect.rectF.width()
                val rHeight = mMosaicRect.rectF.height()

                mDegree = mMosaicRect.degree

                val viewWidth = mParentFrame.width() * rWidth +  ContextUtils.dip2px(view.context,26f)
                val viewHeight = mParentFrame.height() * rHeight +  ContextUtils.dip2px(view.context,26f)

                val viewPx = centerPoint.x * mParentFrame.width()
                val viewPy = centerPoint.y * mParentFrame.height()

                x = (viewPx - (viewWidth / 2f)).toFloat()
                y = (viewPy - (viewHeight / 2f)).toFloat()

                view.layoutParams.width = viewWidth.toInt()
                view.layoutParams.height = viewHeight.toInt()

                mDefaultViewSize = TuSdkSize.create(viewWidth.toInt(), viewHeight.toInt())

                mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F,0f,viewWidth.toFloat(),viewHeight.toFloat())

                isLayout = true
                isSelected = false
                rotation = mDegree

                mTranslation = PointF(x,y)
                requestLayout()

                mThreadPool?.execute {
                    updateProperty()
                }
            }

        }
    }

    override fun updateViewSize() {
        val view = this
        context.runOnUiThread {
            val centerPoint = PointF(mMosaicRect.rectF.centerX(),mMosaicRect.rectF.centerY())
            val rWidth = mMosaicRect.rectF.width()
            val rHeight = mMosaicRect.rectF.height()

            val viewWidth = mParentFrame.width() * rWidth +  ContextUtils.dip2px(view.context,26f)
            val viewHeight = mParentFrame.height() * rHeight +  ContextUtils.dip2px(view.context,26f)

            val viewPx = centerPoint.x * mParentFrame.width()
            val viewPy = centerPoint.y * mParentFrame.height()

            x = (viewPx - (viewWidth / 2f)).toFloat()
            y = (viewPy - (viewHeight / 2f)).toFloat()

            view.layoutParams.width = viewWidth.toInt()
            view.layoutParams.height = viewHeight.toInt()

            mDefaultViewSize = TuSdkSize.create(viewWidth.toInt(), viewHeight.toInt())

            mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F,0f,viewWidth.toFloat(),viewHeight.toFloat())

            mTranslation = PointF(x,y)
            requestLayout()

            mThreadPool?.execute {
                updateProperty()
            }
        }
    }


    override fun setSelected(selected: Boolean) {
        val color = if (selected) {
            mStrokeColor
        } else {
            Color.TRANSPARENT
        }
        getImageView()?.setStroke(color, mStrokeWidth)
        showViewIn(getCancelButton(), selected)
        showViewIn(getTurnButton(), selected)
    }
}