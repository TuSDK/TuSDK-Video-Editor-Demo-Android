/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.base.views$
 *  @author  H.ys
 *  @Date    2021/8/18$ 16:20$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views.matte

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.tusdk.pulse.Config
import com.tusdk.pulse.Property
import com.tusdk.pulse.VideoStreamInfo
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.Effect
import com.tusdk.pulse.editor.VideoEditor
import com.tusdk.pulse.editor.effects.VideoMatteEffect
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.sp
import org.lasque.tusdkeditoreasydemo.base.EditorPlayerContext
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.struct.TuSdkSizeF
import org.lasque.tusdkpulse.core.utils.RectHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.TuSdkGestureRecognizer
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
 * @Date        2021/8/18  16:20
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class MatteView : View, MatteInterface {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    companion object {
        private val MATTE_ID = 40
    }


    /** ---------------------------------------- View ------------------------------------------------------- */

    protected var mLastCenterPoint = PointF()

    protected var mLastPoint = PointF()

    protected var mTranslation = PointF()

    protected var mScale = 1f

    protected var mMaxScale = 0f

    protected var mDegree = 0.0f

    protected var mDefaultViewSize: TuSdkSize = TuSdkSize()

    protected var mHasExceededMaxSize = false

    private var mMattePaint: MattePaint? = null

    private var mCHypotenuse = 0f

    private var mParentRect = Rect()

    protected var mParentFrame : Rect = Rect()

    private var isInit = false

    private var isLayout = false

    private var mCurrentMatteSize = TuSdkSize()

    private var mRadius = 0.0

    private var mDrawRect = Rect()


    private val mOnTouchListener = object : TuSdkGestureRecognizer() {
        override fun onTouchBegin(
            gesture: TuSdkGestureRecognizer?,
            view: View?,
            event: MotionEvent?
        ) {
            handleTransActionStart(gesture!!,event!!)
        }

        override fun onTouchSingleMove(
            gesture: TuSdkGestureRecognizer,
            view: View?,
            event: MotionEvent,
            data: StepData?
        ) {
            this@MatteView.handleTransActionMove(gesture, event)
        }

        override fun onTouchEnd(
            gesture: TuSdkGestureRecognizer?,
            view: View?,
            event: MotionEvent,
            data: StepData?
        ) {
            this@MatteView.handleTransActionEnd(event)
        }

        override fun onTouchMultipleMoveForStablization(
            gesture: TuSdkGestureRecognizer,
            data: StepData
        ) {
            this@MatteView.handleDoubleActionMove(gesture,data)
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return mOnTouchListener.onTouch(this, event)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mMattePaint?.onDraw(canvas!!)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!isLayout){
            mOnTouchListener.isMultipleStablization = true
            isLayout = true
        }
    }

    private fun updatePaint(center: PointF, size: TuSdkSize, rotate: Double) {
        mMattePaint?.update(center, size, rotate)
    }

    protected fun handleTransActionEnd(event: MotionEvent) {
    }

    protected fun handleDoubleActionMove(
        gesture: TuSdkGestureRecognizer,
        data: TuSdkGestureRecognizer.StepData
    ) {
        // 旋转度数
        mDegree = (360 + mDegree + data.stepDegree) % 360

//        rotation = mDegree
//
        val rect = Rect()
        getGlobalVisibleRect(rect)
//
        val caPoint = PointF(rect.centerX().toFloat(), rect.centerY().toFloat())
        this.computerScale(data.stepSpace, caPoint)
//
//        val size = ViewSize.create(this)
//
//        var rectF = RectF()

//        val widthPercent =
//            (size.width.toFloat() - ContextUtils.dip2px(this.context, 26f)) / mParentRect.width()
//        val heightPercent =
//            (size.height.toFloat() - ContextUtils.dip2px(this.context, 26f)) / mParentRect.height()
//
//        val cPointInLayer = PointF()
//
//        cPointInLayer.set(
//            mLastCenterPoint.x / mParentRect.width().toFloat(),
//            mLastCenterPoint.y / mParentRect.height().toFloat()
//        )
//
//        rectF.left = cPointInLayer.x - (widthPercent / 2)
//        rectF.right = cPointInLayer.x + (widthPercent / 2)
//
//        rectF.top = cPointInLayer.y - (heightPercent / 2)
//        rectF.bottom = cPointInLayer.y + (heightPercent / 2)
//
        var res: Future<Unit>? = mThreadPool?.submit(Callable<Unit> {
            if (mMatteType == VideoMatteEffect.MatteType.LOVE || mMatteType == VideoMatteEffect.MatteType.STAR ||
            mMatteType == VideoMatteEffect.MatteType.RECT || mMatteType == VideoMatteEffect.MatteType.CIRCLE){
                val matteRect = mMatteRect
                val scaleSize = TuSdkSizeF.create(Math.abs(mCurrentMatteSize.width.toFloat() * mScale),Math.abs(mCurrentMatteSize.height.toFloat() * mScale))
                val centerX = matteRect.centerX()
                val centerY = matteRect.centerY()
                matteRect.set(centerX - scaleSize.width / 2,centerY - scaleSize.height / 2,centerX + scaleSize.width / 2,centerY + scaleSize.height / 2)

                updateSize(TuSdkSizeF.create(scaleSize.width, scaleSize.height),isImageMatte())
                TLog.e("current scale size ${scaleSize}")
            } else {
                updateScale()
            }
            updateRotate()
            updateProperty()
            updateViewSize()
        })
        res?.get()
        invalidate()
    }



    protected fun computerScale(distance: Float, cPoint: PointF) {
        if (distance == 0f) return

        val offsetScale = distance / mCHypotenuse * 2

        val lastSize = mDefaultViewSize.scale(mScale)

        if (!mHasExceededMaxSize || offsetScale < 0) mScale += offsetScale

        mScale = Math.max(mScale,0.005f)


//        mScale = min(max(mScale,mMinScale),mMaxScale)

        val size = mDefaultViewSize

        val scaleSize = mDefaultViewSize.scale(mScale)


    }

    open protected fun handleTransActionStart(gesture: TuSdkGestureRecognizer,event: MotionEvent) {
        val rect = TuSdkViewHelper.locationInWindow(this)

        val left = Math.min((mLastCenterPoint.x - mCurrentMatteSize.width / 2),(mLastCenterPoint.x + mCurrentMatteSize.width / 2))
        val right = Math.max((mLastCenterPoint.x - mCurrentMatteSize.width / 2),(mLastCenterPoint.x + mCurrentMatteSize.width / 2))
        val top = Math.min((mLastCenterPoint.y - mCurrentMatteSize.height / 2),(mLastCenterPoint.y + mCurrentMatteSize.height / 2))
        val bottom = Math.max((mLastCenterPoint.y - mCurrentMatteSize.height / 2),(mLastCenterPoint.y + mCurrentMatteSize.height / 2))

        val matteRect = RectF(left,top,right,bottom)

        val matteVerticalTop = RectF(
            matteRect.left,0f,matteRect.right,matteRect.top
        )

        val matteVerticalBottom = RectF(
            matteRect.left,matteRect.bottom,matteRect.right, rect.bottom.toFloat()
        )

        val matteHorLeft = RectF(
            0f,matteRect.top,matteRect.left,matteRect.bottom
        )

        val matteHorRight = RectF(
            matteRect.right,matteRect.top, rect.right.toFloat(),matteRect.bottom
        )

        val matteRadius = RectF(
            0f,matteRect.bottom,matteRect.left, rect.bottom.toFloat()
        )

        val matrix = Matrix()
        matrix.preRotate(mDegree,mLastCenterPoint.x,mLastCenterPoint.y)

        matrix.mapRect(matteVerticalTop)
        matrix.mapRect(matteVerticalBottom)
        matrix.mapRect(matteHorLeft)
        matrix.mapRect(matteHorRight)
        matrix.mapRect(matteRadius)

        if (matteRect.contains(event.x, event.y)){
            currentResizeMode = 1;
        } else {
            if (matteVerticalBottom.contains(event.x, event.y)){
                currentResizeMode = 2;
            } else if (matteVerticalTop.contains(event.x, event.y)){
                currentResizeMode = 3;
            }
            else if (matteHorRight.contains(event.x, event.y)){
                currentResizeMode = 4;
            } else if (matteHorLeft.contains(event.x, event.y)){
                currentResizeMode = 5;
            }
            else if (matteRadius.contains(event.x,event.y)){
                currentResizeMode = 6;
            }
        }

        TLog.e("current point ${event.x}:${event.y} " +
                "center point ${mLastCenterPoint}" +
                "matte rect ${matteRect} " +
                "vtop ${matteVerticalTop} " +
                "vbottom ${matteVerticalBottom} " +
                "hleft ${matteHorLeft} " +
                "hright ${matteHorRight}" +
                "current mode ${currentResizeMode}")
    }

    protected fun handleTransActionMove(gesture: TuSdkGestureRecognizer, event: MotionEvent) {

        when(mMatteType){
            VideoMatteEffect.MatteType.STAR,
            VideoMatteEffect.MatteType.LOVE,
            VideoMatteEffect.MatteType.LINEAR,
            VideoMatteEffect.MatteType.MIRROR->{
                scaleMatteActionMove(gesture)
            }
            VideoMatteEffect.MatteType.RECT,
            VideoMatteEffect.MatteType.CIRCLE -> {
                resizeMatteActionMove(gesture,event)
            }
        }


    }

    private var currentResizeMode = 1;//1 : center move 2 : vertical resize 3 : Horizontal resize 4 : radius

    private var lastTouchPoint = PointF()

    private var mMatteRect = RectF()

    private fun resizeMatteActionMove(gesture: TuSdkGestureRecognizer,event: MotionEvent){

        val rect = TuSdkViewHelper.locationInWindow(this)

        val matteRect = mMatteRect

        val matteVerticalTop = RectF(
            matteRect.left,0f,matteRect.right,matteRect.top
        )

        val matteVerticalBottom = RectF(
            matteRect.left,matteRect.bottom,matteRect.right, rect.bottom.toFloat()
        )

        val matteHorLeft = RectF(
            0f,matteRect.top,matteRect.left,matteRect.bottom
        )

        val matteHorRight = RectF(
            matteRect.right,matteRect.top, rect.right.toFloat(),matteRect.bottom
        )

        val matrix = Matrix()
        matrix.preRotate(mDegree,mLastCenterPoint.x,mLastCenterPoint.y)

        matrix.mapRect(matteVerticalTop)
        matrix.mapRect(matteVerticalBottom)
        matrix.mapRect(matteHorLeft)
        matrix.mapRect(matteHorRight)

        TLog.e("current action ${event.action} current mode ${currentResizeMode} step data ${gesture.stepPoint}")

        if (event.action == MotionEvent.ACTION_MOVE){
            if (currentResizeMode == 1){
                scaleMatteActionMove(gesture)
                return
            } else if (currentResizeMode == 2){
                val space = gesture.stepPoint.y
                matteRect.top -=space
                matteRect.bottom += space

            } else if (currentResizeMode == 3){
                val space = gesture.stepPoint.y
                matteRect.top +=space
                matteRect.bottom -= space
            }
            else if (currentResizeMode == 4){
                val space = gesture.stepPoint.x
                matteRect.left -=space
                matteRect.right += space
            } else if (currentResizeMode == 5){
                val space = gesture.stepPoint.x
                matteRect.left +=space
                matteRect.right -= space
            }
            else if (currentResizeMode == 6){
                val space = gesture.stepPoint.x / 100
                mRadius += space
                mRadius = Math.min(mRadius,0.999)
                mRadius = Math.max(mRadius,0.001)

                mRadius = Math.round(mRadius * 1000) / 1000.0
//                mRadius = 0.587000

                TLog.e("current radius ${mRadius}")

                updateRadius()

            }

            mThreadPool?.execute {
                updateSize(TuSdkSizeF.create(Math.abs(matteRect.width()),Math.abs(matteRect.height())),isImageMatte())
                updateProperty()
                updateViewSize()
                mCurrentMatteSize.set((mMatteRect.width() / mScale).toInt(),
                    (mMatteRect.height() / mScale).toInt()
                )
            }

            invalidate()
        }


    }

    private fun isImageMatte() : Boolean{
        return mMatteType == VideoMatteEffect.MatteType.LOVE || mMatteType == VideoMatteEffect.MatteType.STAR
    }



    private fun scaleMatteActionMove(gesture: TuSdkGestureRecognizer) {
        val xP = mLastCenterPoint.x + gesture.stepPoint.x
        val yP = mLastCenterPoint.y + gesture.stepPoint.y

        if (xP > width || xP < 0 || yP > height || yP < 0) return

        mLastCenterPoint.set(xP, yP)


        val rect = TuSdkViewHelper.locationInWindow(this)


        val viewWidth = rect.width()
        val viewHeight = rect.height()

        var xp = mLastCenterPoint.x / viewWidth
        var yp = mLastCenterPoint.y / viewHeight

        val mat = Matrix()
        mat.preTranslate(1f, -1f)
        mat.preScale(2f, -2f)
        mat.preTranslate(-1f, -1f)

        val dst = FloatArray(2)
        val src = FloatArray(2)
        src.set(0, xp)
        src.set(1, yp)

        mat.mapPoints(dst, src)

        xp = dst[0]
        yp = dst[1]


        val centerPercent = PointF(xp, yp)

        mThreadPool?.execute {
            updateCenterPoint(centerPercent)
            updateProperty()
            updateViewSize()
        }

        invalidate()
    }

    protected fun getCenterOpposite(trans: Rect): PointF {
        val globalOffset = Point()
        getGlobalVisibleRect(trans, globalOffset)
        val centerPoint = PointF(trans.centerX().toFloat(), trans.centerY().toFloat())
        centerPoint[trans.centerX().toFloat()] = trans.centerY().toFloat()
        return centerPoint
    }

    protected fun getCenterOpposite(trans: PointF): PointF {
        val oPoint = PointF()
        oPoint.x = trans.x + this.width * 0.5f
        oPoint.y = trans.y + this.height * 0.5f

        return oPoint
    }

    fun setViewSize(subView: View?, width: Int, height: Int) {
        TuSdkViewHelper.setViewWidth(subView, width)
        TuSdkViewHelper.setViewHeight(subView, height)
    }

    /** ---------------------------------------------- Effect ---------------------------------------------------- */

    protected var mThreadPool: ExecutorService? = null

    private var mEditor: VideoEditor? = null

    private var mMatteEffect: Effect? = null

    protected var mPlayerContext: EditorPlayerContext? = null

    protected var mMatteConfig = Config()

    protected var mMatteType = VideoMatteEffect.MatteType.LINEAR

    private var mMattePropertyHolder: VideoMatteEffect.PropertyHolder =
        VideoMatteEffect.PropertyHolder()

    private var mMattePropertyBuilder = VideoMatteEffect.PropertyBuilder(mMattePropertyHolder)

    override fun changeMatte(matteType: VideoMatteEffect.MatteType, parentClip: Clip) {
        isInit = false

        mEditor!!.player.lock()

        val matteEffect = Effect(mEditor!!.context, VideoMatteEffect.TYPE_NAME)
        val matteConfig = Config()
        matteConfig.setString(VideoMatteEffect.CONFIG_MATTE_TYPE, matteType.type)
        matteEffect.setConfig(matteConfig)

        if (parentClip.effects().get(MATTE_ID) != null){
            parentClip.effects().delete(MATTE_ID)
        }
        parentClip.effects().add(MATTE_ID, matteEffect)

        mMatteEffect = matteEffect
        mEditor!!.build()


        mEditor!!.player.unlock()

        val videoInfo = (parentClip.originStreamInfo as VideoStreamInfo)

        mMattePropertyHolder.scale = 0.35
        mMattePropertyHolder.size.width =
            when(matteType){
                VideoMatteEffect.MatteType.STAR,
                VideoMatteEffect.MatteType.LOVE->{
                    0.5f
                }
                VideoMatteEffect.MatteType.LINEAR,
                VideoMatteEffect.MatteType.MIRROR,
                VideoMatteEffect.MatteType.RECT,
                VideoMatteEffect.MatteType.CIRCLE->{
                    0.35f
                }
                else -> {0f}
            }
        mMattePropertyHolder.size.height =
            when(matteType){
                VideoMatteEffect.MatteType.STAR,
                VideoMatteEffect.MatteType.LOVE->{
                    0.5f
                }
                VideoMatteEffect.MatteType.LINEAR,
                VideoMatteEffect.MatteType.MIRROR,
                VideoMatteEffect.MatteType.RECT,
                VideoMatteEffect.MatteType.CIRCLE->{
                    videoInfo.width * 0.35f / videoInfo.height
//                    0.35f
                }
                else -> {0f}
            }

//        mMattePropertyHolder.radius = 0.587000
        updateProperty()


        mMatteType = matteType

        when (matteType) {
            VideoMatteEffect.MatteType.STAR -> {
                mMattePaint = MatteStarPaint()
            }
            VideoMatteEffect.MatteType.LOVE -> {
                mMattePaint = MatteLovePaint()
            }
            VideoMatteEffect.MatteType.LINEAR -> {
                mMattePaint = MatteLinearPaint()
            }
            VideoMatteEffect.MatteType.MIRROR -> {
                mMattePaint = MatteMirrorPaint()
            }
            VideoMatteEffect.MatteType.RECT -> {
                mMattePaint = MatteRectPaint()
            }
            VideoMatteEffect.MatteType.CIRCLE -> {
                mMattePaint = MatteCirclePaint()
            }
        }

        TLog.e("current rect ${mDrawRect}")

        mMattePaint?.updateRect(mDrawRect)

        updateViewSize()
    }

    override fun restoreMatte(parentClip: Clip) {
        if (parentClip.effects().get(MATTE_ID) == null) return;
        val matteEffect = parentClip.effects().get(MATTE_ID)
        val matteConfig = matteEffect.config

        val type = matteConfig.getString(VideoMatteEffect.CONFIG_MATTE_TYPE)

        mMatteEffect = matteEffect

        for (types in VideoMatteEffect.MatteType.values()){
            if (type == types.type){
                mMatteType = types
                break
            }
        }

        val holder = VideoMatteEffect.PropertyHolder(matteEffect.getProperty(VideoMatteEffect.PROP_PARAM))
        mMattePropertyHolder = holder
        mMattePropertyBuilder.holder = holder

        when (mMatteType) {
            VideoMatteEffect.MatteType.STAR -> {
                mMattePaint = MatteStarPaint()
            }
            VideoMatteEffect.MatteType.LOVE -> {
                mMattePaint = MatteLovePaint()
            }
            VideoMatteEffect.MatteType.LINEAR -> {
                mMattePaint = MatteLinearPaint()
            }
            VideoMatteEffect.MatteType.MIRROR -> {
                mMattePaint = MatteMirrorPaint()
            }
            VideoMatteEffect.MatteType.RECT -> {
                mMattePaint = MatteRectPaint()
            }
            VideoMatteEffect.MatteType.CIRCLE -> {
                mMattePaint = MatteCirclePaint()
            }
        }

        TLog.e("current rect ${mDrawRect}")

        mMattePaint?.updateRect(mDrawRect)

        updateViewSize()
    }

    override fun removeMatte(parentClip: Clip) {
        mEditor!!.player.lock()
        if (parentClip.effects().get(MATTE_ID) != null){
            parentClip.effects().delete(MATTE_ID)
        }
        mEditor!!.build()
        mEditor!!.player.unlock()
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

    private fun updateScale() {
        mMattePropertyHolder.scale = mScale.toDouble()
    }

    private fun updateRotate() {
        val rotate = Math.toRadians(mDegree.toDouble())
        mMattePropertyHolder.rotate = rotate.toDouble()
    }

    private fun updateZoom() {
        mMattePropertyHolder.scale = mScale.toDouble()
    }

    private fun updateRect(rect: RectF) {
        mMattePropertyHolder.size.width = rect.width()
        mMattePropertyHolder.size.height = rect.height()
    }

    private fun updateRadius() {
//        mMattePropertyHolder.radius = mRadius
    }

    private fun updateSize(size : TuSdkSizeF,isImage : Boolean){
        val rect = TuSdkViewHelper.locationInWindow(this)
        val rectSize = TuSdkSize.create(rect)

        if (isImage){
            val widthPercent = size.width.toFloat() / rectSize.minSide()
            val heightPercent = size.height.toFloat() / rectSize.minSide()

            mMattePropertyHolder.size.width = widthPercent
            mMattePropertyHolder.size.height = heightPercent
        } else {
            val widthPercent = size.width.toFloat() / rectSize.width
            val heightPercent = size.height.toFloat() / rectSize.height

            mMattePropertyHolder.size.width = widthPercent
            mMattePropertyHolder.size.height = heightPercent
        }

    }



    private fun updateCenterPoint(point : PointF){
        mMattePropertyHolder.center.set(point)
    }

    private fun updateProperty() {
        mMatteEffect?.setProperty(VideoMatteEffect.PROP_PARAM, mMattePropertyBuilder.makeProperty())
        mPlayerContext?.refreshFrame()
    }

    private fun updateViewSize() {

        val infoProperty: Property? =
            mMatteEffect!!.getProperty(VideoMatteEffect.PROP_INTERACTION_INFO)
        TLog.e("property info ${infoProperty.toString()}")
        val posProperty = VideoMatteEffect.InteractionInfo(infoProperty)
        context.runOnUiThread {
            when (mMatteType) {
                VideoMatteEffect.MatteType.STAR -> {
                    updateStarViewSize(posProperty)
                }
                VideoMatteEffect.MatteType.LOVE -> {
                    updateLoveViewSize(posProperty)
                }
                VideoMatteEffect.MatteType.LINEAR -> {
                    updateLinearViewSize(posProperty)
                }
                VideoMatteEffect.MatteType.MIRROR -> {
                    updateMirrorViewSize(posProperty)
                }
                VideoMatteEffect.MatteType.RECT -> {
                    updateRectViewSize(posProperty)
                }
                VideoMatteEffect.MatteType.CIRCLE -> {
                    updateCircleViewSize(posProperty)
                }
            }

            invalidate()
        }
    }

    private fun updateStarViewSize(posProperty: VideoMatteEffect.InteractionInfo) {
        val rect = TuSdkViewHelper.locationInWindow(this)

        val viewWidth = rect.width()
        val viewHeight = rect.height()

        val mat = Matrix()
        mat.preTranslate(0f,1f)
        mat.preScale(0.5f,-0.5f)
        mat.preTranslate(1f,1f)

        val dst = FloatArray(2)
        val src = FloatArray(2)
        src.set(0,posProperty.center.x)
        src.set(1,posProperty.center.y)

        mat.mapPoints(dst, src)

        val percentPoint = PointF()
        percentPoint.x = dst[0]
        percentPoint.y = dst[1]

        val viewSize = TuSdkSize.create(rect)
        val minSide = viewSize.minSide()
        val matteSize = TuSdkSize.create((posProperty.width * minSide).toInt(),
            (posProperty.height * minSide).toInt()
        )

        if (!isInit){
            mLastCenterPoint = PointF()
            mLastCenterPoint.x = percentPoint.x * viewWidth
            mLastCenterPoint.y = percentPoint.y * viewHeight

            mCurrentMatteSize = matteSize

            mMatteRect = RectF(
                (mLastCenterPoint.x - mCurrentMatteSize.width / 2),
                (mLastCenterPoint.y - mCurrentMatteSize.height / 2),
                (mLastCenterPoint.x + mCurrentMatteSize.width / 2),
                (mLastCenterPoint.y + mCurrentMatteSize.height / 2)
            )
            isInit = true

            mScale = 1.0f
        }

        mDegree = Math.toDegrees(posProperty.rotate).toFloat()

        mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F, 0f, viewWidth.toFloat(), viewHeight.toFloat()
        )

        mMattePaint?.updateCanvasSize(viewWidth,viewHeight)
        mMattePaint?.update(percentPoint,matteSize,mDegree.toDouble())
    }

    private fun updateLoveViewSize(posProperty: VideoMatteEffect.InteractionInfo) {
        val rect = TuSdkViewHelper.locationInWindow(this)

        val viewWidth = rect.width()
        val viewHeight = rect.height()

        val mat = Matrix()
        mat.preTranslate(0f,1f)
        mat.preScale(0.5f,-0.5f)
        mat.preTranslate(1f,1f)

        val dst = FloatArray(2)
        val src = FloatArray(2)
        src.set(0,posProperty.center.x)
        src.set(1,posProperty.center.y)

        mat.mapPoints(dst, src)

        val percentPoint = PointF()
        percentPoint.x = dst[0]
        percentPoint.y = dst[1]

        val viewSize = TuSdkSize.create(rect)
        val minSide = viewSize.minSide()
        val matteSize = TuSdkSize.create((posProperty.width * minSide).toInt(),
            (posProperty.height * minSide).toInt()
        )

        if (!isInit){
            mLastCenterPoint = PointF()
            mLastCenterPoint.x = percentPoint.x * viewWidth
            mLastCenterPoint.y = percentPoint.y * viewHeight

            mCurrentMatteSize = matteSize

            mMatteRect = RectF(
                (mLastCenterPoint.x - mCurrentMatteSize.width / 2),
                (mLastCenterPoint.y - mCurrentMatteSize.height / 2),
                (mLastCenterPoint.x + mCurrentMatteSize.width / 2),
                (mLastCenterPoint.y + mCurrentMatteSize.height / 2)
            )

            mScale = 1.0f

            isInit = true
        }

        mDegree = Math.toDegrees(posProperty.rotate).toFloat()

        mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F, 0f, viewWidth.toFloat(), viewHeight.toFloat()
        )

        mMattePaint?.updateCanvasSize(viewWidth,viewHeight)
        mMattePaint?.update(percentPoint,matteSize,mDegree.toDouble())
    }

    private fun updateRectViewSize(posProperty: VideoMatteEffect.InteractionInfo) {
        val rect = TuSdkViewHelper.locationInWindow(this)

        val viewWidth = rect.width()
        val viewHeight = rect.height()

        val mat = Matrix()
        mat.preTranslate(0f,1f)
        mat.preScale(0.5f,-0.5f)
        mat.preTranslate(1f,1f)

        val dst = FloatArray(2)
        val src = FloatArray(2)
        src.set(0,posProperty.center.x)
        src.set(1,posProperty.center.y)

        mat.mapPoints(dst, src)

        val percentPoint = PointF()
        percentPoint.x = dst[0]
        percentPoint.y = dst[1]

        val viewSize = TuSdkSize.create(rect)
        val minSide = viewSize.minSide()
        val matteSize = TuSdkSize.create((posProperty.width * viewSize.width).toInt(),
            (posProperty.height * viewSize.height).toInt()
        )

        if (!isInit){
            mLastCenterPoint = PointF()
            mLastCenterPoint.x = percentPoint.x * viewWidth
            mLastCenterPoint.y = percentPoint.y * viewHeight

            mCurrentMatteSize = matteSize

            mMatteRect = RectF(
                (mLastCenterPoint.x - mCurrentMatteSize.width / 2),
                (mLastCenterPoint.y - mCurrentMatteSize.height / 2),
                (mLastCenterPoint.x + mCurrentMatteSize.width / 2),
                (mLastCenterPoint.y + mCurrentMatteSize.height / 2)
            )

            mRadius = posProperty.radius

            mScale = 1f;
            isInit = true

        }

        mDegree = Math.toDegrees(posProperty.rotate).toFloat()

        TLog.e("current pos radius ${posProperty.radius}")


        mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F, 0f, viewWidth.toFloat(), viewHeight.toFloat()
        )


//        (mMattePaint as MatteRectPaint).updateRadius(posProperty.radius)
        mMattePaint?.updateCanvasSize(viewWidth,viewHeight)
        mMattePaint?.update(percentPoint,matteSize,mDegree.toDouble())
    }

    private fun updateCircleViewSize(posProperty: VideoMatteEffect.InteractionInfo) {
        val rect = TuSdkViewHelper.locationInWindow(this)

        val viewWidth = rect.width()
        val viewHeight = rect.height()

        val mat = Matrix()
        mat.preTranslate(0f,1f)
        mat.preScale(0.5f,-0.5f)
        mat.preTranslate(1f,1f)

        val dst = FloatArray(2)
        val src = FloatArray(2)
        src.set(0,posProperty.center.x)
        src.set(1,posProperty.center.y)

        mat.mapPoints(dst, src)

        val percentPoint = PointF()
        percentPoint.x = dst[0]
        percentPoint.y = dst[1]

        val viewSize = TuSdkSize.create(rect)
        val minSide = viewSize.minSide()
        val matteSize = TuSdkSize.create((posProperty.width * viewSize.width).toInt(),
            (posProperty.height * viewSize.height).toInt()
        )

        if (!isInit){
            mLastCenterPoint = PointF()
            mLastCenterPoint.x = percentPoint.x * viewWidth
            mLastCenterPoint.y = percentPoint.y * viewHeight

            mCurrentMatteSize = matteSize

            mMatteRect = RectF(
                (mLastCenterPoint.x - mCurrentMatteSize.width / 2),
                (mLastCenterPoint.y - mCurrentMatteSize.height / 2),
                (mLastCenterPoint.x + mCurrentMatteSize.width / 2),
                (mLastCenterPoint.y + mCurrentMatteSize.height / 2)
            )

            mScale = 1f;

            isInit = true

        }

        mDegree = Math.toDegrees(posProperty.rotate).toFloat()

        mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F, 0f, viewWidth.toFloat(), viewHeight.toFloat()
        )

        mMattePaint?.updateCanvasSize(viewWidth,viewHeight)
        mMattePaint?.update(percentPoint,matteSize,mDegree.toDouble())

    }

    private fun updateMirrorViewSize(posProperty: VideoMatteEffect.InteractionInfo) {
        val rect = TuSdkViewHelper.locationInWindow(this)

        val viewWidth = rect.width()
        val viewHeight = rect.height()

        val mat = Matrix()
        mat.preTranslate(0f,1f)
        mat.preScale(0.5f,-0.5f)
        mat.preTranslate(1f,1f)

        val dst = FloatArray(2)
        val src = FloatArray(2)
        src.set(0,posProperty.center.x)
        src.set(1,posProperty.center.y)

        mat.mapPoints(dst, src)

        val percentPoint = PointF()
        percentPoint.x = dst[0]
        percentPoint.y = dst[1]

        val viewSize = TuSdkSize.create(mDrawRect)
        val matteSize = TuSdkSize.create(viewSize.width,
            (viewSize.maxSide() * posProperty.scale).toInt()
        )

        TLog.e("mirrow scale ${posProperty.scale}")

        if (!isInit){
            mScale = 1f;

            mLastCenterPoint = PointF()
            mLastCenterPoint.x = percentPoint.x * viewWidth
            mLastCenterPoint.y = percentPoint.y * viewHeight


            isInit = true
        }

        mDegree = Math.toDegrees(posProperty.rotate).toFloat()

        mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F, 0f, viewWidth.toFloat(), viewHeight.toFloat()
        )

        mScale = posProperty.scale.toFloat()

        mMattePaint?.updateCanvasSize(viewWidth,viewHeight)
        mMattePaint?.update(percentPoint,matteSize,mDegree.toDouble())

        invalidate()

    }

    private fun updateLinearViewSize(posProperty: VideoMatteEffect.InteractionInfo) {

        val rect = TuSdkViewHelper.locationInWindow(this)

        val viewWidth = rect.width()
        val viewHeight = rect.height()

        val mat = Matrix()
        mat.preTranslate(0f,1f)
        mat.preScale(0.5f,-0.5f)
        mat.preTranslate(1f,1f)

        val dst = FloatArray(2)
        val src = FloatArray(2)
        src.set(0,posProperty.center.x)
        src.set(1,posProperty.center.y)

        mat.mapPoints(dst, src)


        val percentPoint = PointF()
        percentPoint.x = dst[0]
        percentPoint.y = dst[1]

        if (!isInit){
            mScale = 1f;

            mLastCenterPoint = PointF()
            mLastCenterPoint.x = percentPoint.x * viewWidth
            mLastCenterPoint.y = percentPoint.y * viewHeight


            isInit = true
        }

        mDegree = Math.toDegrees(posProperty.rotate).toFloat()


        TLog.e("current degree ${mDegree}")

        mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F, 0f, viewWidth.toFloat(), viewHeight.toFloat()
        )

        mMattePaint?.updateCanvasSize(viewWidth,viewHeight)
        mMattePaint?.update(percentPoint,TuSdkSize(viewWidth,viewHeight), mDegree.toDouble())

        invalidate()


    }

    public fun resize(frame: Rect){
        mMattePaint?.updateCanvasSize(frame.width(),frame.height())
        setViewSize(this,frame.width(),frame.height())

    }

    public fun updateRect(rect : Rect){
        mDrawRect = rect
        mMattePaint?.updateRect(rect)
    }

    public fun updateMixed(mixed : Double){
        mMattePropertyHolder.diff = mixed
        updateProperty()
    }

    public fun updateInv(){
        mMattePropertyHolder.invert = !mMattePropertyHolder.invert
        updateProperty()
    }


}