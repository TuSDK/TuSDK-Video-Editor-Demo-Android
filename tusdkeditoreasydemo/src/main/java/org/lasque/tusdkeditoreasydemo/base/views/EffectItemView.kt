/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.base.views$
 *  @author  H.ys
 *  @Date    2021/5/17$ 15:46$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.VideoEditor
import com.tusdk.pulse.editor.effects.MosaicEffect
import org.lasque.tusdkeditoreasydemo.base.EditorPlayerContext
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.struct.ViewSize
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.utils.RectHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.TuSdkGestureRecognizer
import org.lasque.tusdkpulse.core.view.TuSdkRelativeLayout
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/5/17  15:46
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
abstract class EffectItemView : TuSdkRelativeLayout,EffectInterface{
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    companion object{
        public var CURRENT_EFFECT_ID = 600
    }


    protected var mListener: OnEffectItemViewListener? = null

    protected var isMoveEvent = false

    protected var mParentFrame : Rect = Rect()

    protected var mLastCenterPoint = PointF()

    private var mParentRect = Rect()

    protected var mCHypotenuse = 0f

    protected var isLayout = false

    protected var mLastPoint = PointF()

    protected var mTranslation = PointF()

    protected var mScale = 1f

    protected var mMaxScale = 0f

    protected var mDegree = 0.0f

    protected var mDefaultViewSize : TuSdkSize = TuSdkSize()

    protected var mHasExceededMaxSize = false

    private var mOnDrawListener : ViewTreeObserver.OnPreDrawListener? = null

    protected var mStartPos : Long = 0L

    protected var mEndPos = 0L

    /** ----------------------------------------------- Effect -------------------------------------------------------------------- */

    protected var mThreadPool : ExecutorService? = null

    protected var mEditor : VideoEditor? = null

    protected var mCurrentEffect : Effect? = null

    protected var mCurrentConfig = Config()

    public var mCurrentEffectIndex = -1

    protected var mPlayerContext : EditorPlayerContext? = null

    protected var mItemViewType : EffectType = EffectType.Mosaic

    /** ----------------------------------------------- View -------------------------------------------------------------------- */



    private val mConfigTouchListener : TuSdkGestureRecognizer = object : TuSdkGestureRecognizer() {
        override fun onTouchSingleMove(gesture: TuSdkGestureRecognizer, view: View?, event: MotionEvent, data: StepData?) {
            handleTransActionMove(gesture,event)
        }

        override fun onTouchBegin(gesture: TuSdkGestureRecognizer?, view: View?, event: MotionEvent) {
            handleTransActionStart(event)
        }

        override fun onTouchMultipleMoveForStablization(gesture: TuSdkGestureRecognizer, data: StepData) {
            handleDoubleActionMove(gesture,data)
        }

        override fun onTouchEnd(gesture: TuSdkGestureRecognizer?, view: View?, event: MotionEvent, data: StepData?) {
            handleTransActionEnd(event)
        }

    }

    protected val mOnTouchListener = object : OnTouchListener{
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event!!.pointerCount > 1) return false

            val parentFrame = TuSdkViewHelper.locationInWindow(parent as View)

            var yOffset = 0;

            if (mParentFrame.top - parentFrame.top > 0)
                yOffset = mParentFrame.top - parentFrame.top

            when(event.action){
                MotionEvent.ACTION_DOWN->{
                    handleTurnAndScaleActionStart(null,event.rawX,event.rawY + yOffset)
                }

                MotionEvent.ACTION_MOVE->{
                    handleTurnAndScaleActionMove(null,event.rawX,event.rawY + yOffset)
                }
            }
            return true
        }

    }

    override fun initView() {
        val vto = this.viewTreeObserver
        val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (!isLayouted){
                isLayouted = true

                mConfigTouchListener.isMultipleStablization = true
                onLayouted()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            mOnDrawListener = ViewTreeObserver.OnPreDrawListener {
                mLastCenterPoint = PointF(x + (width / 2).toFloat(),y + (height / 2).toFloat())
                true
            }
            vto.addOnPreDrawListener(mOnDrawListener)
        }
        vto.addOnGlobalLayoutListener(globalLayoutListener)
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return mConfigTouchListener.onTouch(this,event)
    }

    protected fun handleTurnAndScaleActionStart(trueButton: TuSdkImageButton?,xCoordinate : Float,yCoordinate : Float){
        mLastPoint.set(xCoordinate,yCoordinate)

        mListener?.onItemSelected(this)
    }

    protected fun handleTurnAndScaleActionMove(turnButton: TuSdkImageButton?,xCoordinate : Float,yCoordinate : Float){
        mLastCenterPoint = PointF(x + (width / 2).toFloat(),y + (height / 2).toFloat())

        var point = PointF(xCoordinate,yCoordinate)

        var trans = Rect()

        //计算中心店
        var cPoint = getCenterOpposite(trans)

        //计算旋转角度
        computerAngle(point, cPoint)

        //计算缩放
        computerScale(point, cPoint)

        requestLayout()
        mLastPoint.set(point.x,point.y)

        val size = ViewSize.create(this)

        var rectF = RectF()

        val widthPercent = (size.width.toFloat() - ContextUtils.dip2px(this.context,26f)) / mParentRect.width()
        val heightPercent = (size.height.toFloat() - ContextUtils.dip2px(this.context,26f)) / mParentRect.height()

        val cPointInLayer = PointF()

        cPointInLayer.set(mLastCenterPoint.x / mParentRect.width().toFloat(),mLastCenterPoint.y / mParentRect.height().toFloat())

        rectF.left = cPointInLayer.x - (widthPercent / 2)
        rectF.right = cPointInLayer.x + (widthPercent / 2)

        rectF.top = cPointInLayer.y - (heightPercent / 2)
        rectF.bottom = cPointInLayer.y + (heightPercent / 2)

        var res : Future<Unit>? = mThreadPool?.submit(Callable<Unit> {
            updateRotate()
            updateRect(rectF)
            updateProperty()
        })

        res?.get()
    }

    protected fun computerScale(point: PointF,cPoint: PointF){
        val sDistance = RectHelper.getDistanceOfTwoPoints(cPoint,mLastPoint)

        val cDistance = RectHelper.getDistanceOfTwoPoints(cPoint,point)

        computerScale(cDistance - sDistance,cPoint)
    }

    protected fun computerAngle(point : PointF,cPoint : PointF){
        val sAngle = getCenterOppositeAngle(mLastPoint,cPoint)

        val eAngle = getCenterOppositeAngle(point, cPoint)

        mDegree = (360 + mDegree + (eAngle - sAngle)) % 360

        rotation = mDegree
    }

    protected fun getCenterOppositeAngle(point: PointF,cPoint: PointF) : Float{
        val sPoint = PointF(point.x - mParentFrame.left,point.y - mParentFrame.top)
        return RectHelper.computeAngle(point,cPoint)
    }


    protected fun handleTransActionEnd(event: MotionEvent){
        if (!isMoveEvent){
            mListener?.onItemUnselected(this)
        }
    }

    protected fun handleDoubleActionMove(gesture: TuSdkGestureRecognizer,data : TuSdkGestureRecognizer.StepData){
        // 旋转度数
        mDegree = (360 + mDegree + data.stepDegree) % 360

        rotation = mDegree

        val rect = Rect()
        getGlobalVisibleRect(rect)

        val caPoint = PointF(rect.centerX().toFloat(),rect.centerY().toFloat())
        this.computerScale(data.stepSpace,caPoint)

        this.requestLayout()

        val size = ViewSize.create(this)

        var rectF = RectF()

        val widthPercent = (size.width.toFloat() - ContextUtils.dip2px(this.context,26f)) / mParentRect.width()
        val heightPercent = (size.height.toFloat() - ContextUtils.dip2px(this.context,26f)) / mParentRect.height()

        val cPointInLayer = PointF()

        cPointInLayer.set(mLastCenterPoint.x / mParentRect.width().toFloat(),mLastCenterPoint.y / mParentRect.height().toFloat())

        rectF.left = cPointInLayer.x - (widthPercent / 2)
        rectF.right = cPointInLayer.x + (widthPercent / 2)

        rectF.top = cPointInLayer.y - (heightPercent / 2)
        rectF.bottom = cPointInLayer.y + (heightPercent / 2)

        var res : Future<Unit>? = mThreadPool?.submit(Callable<Unit> {
            updateRotate()
            updateRect(rectF)
            updateProperty()
        })

        res?.get()
    }

    protected fun computerScale(distance : Float,cPoint : PointF){
        if (distance == 0f) return

        val offsetScale = distance / mCHypotenuse * 2

        val lastSize = mDefaultViewSize.scale(mScale)

        if (!mHasExceededMaxSize || offsetScale < 0) mScale += offsetScale

//        mScale = min(max(mScale,mMinScale),mMaxScale)

        val size = mDefaultViewSize

        val scaleSize = mDefaultViewSize.scale(mScale)

        val outRect = RectHelper.minEnclosingRectangle(cPoint,scaleSize,mDegree)
        mTranslation.offset((lastSize.width - scaleSize.width) * 0.5f,(lastSize.height - scaleSize.height) * 0.5f)



        this.fixedMovePoint(mTranslation,outRect)
        TLog.e("(x , y) $mTranslation current scale $mScale lastSize $lastSize scaleSize $scaleSize maxscale ${mMaxScale}")

        setViewSize(this,scaleSize.width,scaleSize.height)
        x = mTranslation.x
        y = mTranslation.y


    }

    open protected fun handleTransActionStart(event : MotionEvent){
        isMoveEvent = false
        mListener?.onItemSelected(this)
    }

    protected fun handleTransActionMove(gesture : TuSdkGestureRecognizer,event : MotionEvent){
        if (!(Math.abs(gesture.stepPoint.x) < 2f && Math.abs(gesture.stepPoint.y) < 2f)) isMoveEvent = true

        mTranslation.offset(gesture.stepPoint.x,gesture.stepPoint.y)

        TLog.e(" current translation $mTranslation")

        val trans = Rect()

        val isLandscape = mParentFrame.width() > mParentFrame.height()

        var cPoint = if (isLandscape){getCenterOpposite(mTranslation)} else {getCenterOpposite(trans)}

        val size = ViewSize.create(this)

        val outRect = RectHelper.minEnclosingRectangle(cPoint,size,mDegree)

        cPoint.offset(gesture.stepPoint.x,gesture.stepPoint.y)

        this.fixedMovePoint(if (isLandscape){mTranslation} else {cPoint},outRect)

        x = mTranslation.x
        y = mTranslation.y
        requestLayout()

        mLastCenterPoint.set(x + width/2f,y + height/2f)

        var rectF = RectF()

        val widthPercent = (size.width.toFloat() - ContextUtils.dip2px(this.context,26f)) / mParentRect.width()
        val heightPercent = (size.height.toFloat() - ContextUtils.dip2px(this.context,26f)) / mParentRect.height()

        val cPointInLayer = PointF()

        cPointInLayer.set(mLastCenterPoint.x / mParentRect.width().toFloat(),mLastCenterPoint.y / mParentRect.height().toFloat())

        rectF.left = cPointInLayer.x - (widthPercent / 2)
        rectF.right = cPointInLayer.x + (widthPercent / 2)

        rectF.top = cPointInLayer.y - (heightPercent / 2)
        rectF.bottom = cPointInLayer.y + (heightPercent / 2)


        mThreadPool?.execute {
            updateRect(rectF)
            updateProperty()
        }

    }

    /**
     * 修复移动范围
     *
     * @param trans   视图相对父视图左上角坐标
     * @param outRect 旋转后的外接矩形坐标
     */
    protected fun fixedMovePoint(trans: PointF?, outRect: RectF?) {
        if (mParentFrame == null || trans == null || outRect == null) return
        // 父视图边缘区域
        val edge = RectF(-outRect.width() * 0.5f, -outRect.height() * 0.5f, mParentFrame.width() + outRect.width() * 0.5f, mParentFrame.height()
                + outRect.height() * 0.5f)

        // TLog.d("fixedMovePoint 1: outRect: %s | trans: %s | this:[%s|%s] | outRect:[%s|%s] | edge: %s",
        // outRect, trans,
        // this.getWidth(), this.getHeight(), outRect.width(),
        // outRect.height(), edge);
        if (outRect.left < edge.left) {
            trans.x = edge.left + (outRect.width() - this.width) * 0.5f
        }
        if (outRect.right > edge.right) {
            trans.x = edge.right - (outRect.width() + this.width) * 0.5f
        }
        if (outRect.top < edge.top) {
            trans.y = edge.top + (outRect.height() - this.height) * 0.5f
        }
        if (outRect.bottom > edge.bottom) {
            trans.y = edge.bottom - (outRect.height() + this.height) * 0.5f
        }

        // TLog.d("fixedMovePoint 2: %s", trans);
    }

    protected fun getCenterOpposite(trans : Rect) : PointF{
        val globalOffset = Point()
        getGlobalVisibleRect(trans, globalOffset)
        val centerPoint = PointF(trans.centerX().toFloat(), trans.centerY().toFloat())
        centerPoint[trans.centerX().toFloat()] = trans.centerY().toFloat()
        return centerPoint
    }

    protected fun getCenterOpposite(trans : PointF) : PointF{
        val oPoint = PointF()
        oPoint.x = trans.x + this.width * 0.5f
        oPoint.y = trans.y + this.height * 0.5f

        return oPoint
    }

    override fun setViewSize(subView: View?, width: Int, height: Int) {
        TuSdkViewHelper.setViewWidth(subView,width)
        TuSdkViewHelper.setViewHeight(subView,height)
    }



    override fun setParentFrame(frame: Rect) {
        this.mParentFrame = frame
    }

    override fun setParentRect(rect: Rect) {
        mParentRect.set(rect)
    }

    override fun setEditor(editor: VideoEditor) {
        mEditor = editor
    }

    override fun setPlayerContext(context: EditorPlayerContext) {
        mPlayerContext = context
    }

    override fun setThreadPool(threadPool: ExecutorService) {
        mThreadPool = threadPool
    }

    override fun resize(frame: Rect) {
        if (mParentFrame.equals(frame)) return
        this.mParentFrame = frame

        var res : Future<Unit>? = mThreadPool?.submit(Callable<Unit> {
            updateViewSize()
        } )

        res?.get()

    }

    override fun setListener(listener: OnEffectItemViewListener) {
        mListener = listener
    }

    fun getItemEffectType() : EffectType{
        return mItemViewType
    }

    override fun onSaveInstanceState(): Parcelable? {
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        mCurrentEffect = mEditor!!.videoComposition().effects()[mCurrentEffectIndex]
    }

    public fun requestShower(frame : Long){
        if (frame in mStartPos until mEndPos){
            visibility = View.VISIBLE
        } else {
            visibility = View.INVISIBLE
        }
    }

    abstract protected open fun updateRotate()

    abstract protected open fun updateZoom()


    abstract protected open fun updateRect(rect : RectF)

    abstract protected open fun updateProperty()

    abstract protected open fun updateViewSize()
}