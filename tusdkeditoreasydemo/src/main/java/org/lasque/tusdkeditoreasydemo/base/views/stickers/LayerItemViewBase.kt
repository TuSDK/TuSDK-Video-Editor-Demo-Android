/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base.views.stickers$
 *  @author  H.ys
 *  @Date    2020/11/13$ 11:07$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views.stickers

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import com.tusdk.pulse.Config
import com.tusdk.pulse.Property
import com.tusdk.pulse.VideoStreamInfo
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.Layer.BLEND_MODE_Default
import com.tusdk.pulse.editor.VideoEditor
import org.jetbrains.anko.runOnUiThread
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.struct.ViewSize
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.utils.RectHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.TuSdkGestureRecognizer
import org.lasque.tusdkpulse.core.view.TuSdkRelativeLayout
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import org.lasque.tusdkeditoreasydemo.base.views.TuSdkImageButton
import org.lasque.tusdkeditoreasydemo.base.EditorPlayerContext
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views.stickers
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/13  11:07
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
abstract class LayerItemViewBase : TuSdkRelativeLayout,StickerLayerItemViewInterface {

    companion object{
        public var CURRENT_LAYER_ID = 30
    }

    /** --------------- View ------------------------------------------------------**/

    /** 是否能扩张  */
    protected var mEnableExpand = true

    protected var mListener : OnStickerLayerItemViewListener? = null

    protected var isMoveEvent = false
    protected var mParentFrame: Rect = Rect()

    /** 当前中心点坐标  */
    protected var mLastCenterPoint = PointF()

    protected var mParentRect = Rect()

    /** 内容视图斜边长度  */
    protected var mCHypotenuse = 0f

    protected var isLayout = false

    /**************************** Trans  */
    /** 触摸点信息  */
    protected var mLastPoint = PointF()

    /** 位置信息  */
    protected var mTranslation = PointF()

    /** 缩放比例  */
    protected var mScale = 1f

    /** 最小缩小比例(默认: 0.5f <= mMinScale <= 1)  */
    protected var mMinScale = 0.5f

    /** 最大缩放比例  */
    protected var mMaxScale = 0f

    /** 旋转度数  */
    protected var mDegree = 0.0f

    /** 默认视图长宽  */
    protected var mDefaultViewSize: TuSdkSize = TuSdkSize()

    /** 贴纸宽高是否已经超过了最大允许的尺寸  */
    protected var mHasExceededMaxSize = false

    /** ----------------------------------------------- Layer -------------------------------------------------------------------- */

    protected var mThreadPool : ExecutorService? = null

    protected var mEditor : VideoEditor? = null

    protected var mCurrentType : LayerType = LayerType.Image

    protected var mCurrentLayerType = LayerType.Image

    protected var mCurrentLayer : Layer? = null

    protected var mCurrentLayerConfig : Config? = null

    protected var mResizeProperty : Layer.OverlayPropertyBuilder = Layer.OverlayPropertyBuilder()

    protected var mClipDuration : Long = 0

    protected var mClipStartPos : Long = 0

    protected var mClipEndPos : Long = 0

    protected var mLayerStartPos : Long = 0

    protected var mLayerEndPos : Long = 0

    protected var mMaxLayerDuration : Long = 0

    protected var mClipMaxDuration : Long = 0

    protected var mPlayerContext : EditorPlayerContext? = null

    public var mCurrentLayerId = 0

    public var isMainLayer = false

    private var mLayerMap : HashMap<Int,LayerItemViewBase>? = null

    private val mConfigTouchListner = object : TuSdkGestureRecognizer() {
        override fun onTouchSingleMove(gesture: TuSdkGestureRecognizer, view: View?, event: MotionEvent, data: StepData?) {
            handleTransActionMove(gesture,event)
        }

        override fun onTouchBegin(gesture: TuSdkGestureRecognizer, view: View?, event: MotionEvent) {
            handleTransActionStart(event)
        }

        override fun onTouchMultipleMoveForStablization(gesture: TuSdkGestureRecognizer, data: StepData) {
            handleDoubleActionMove(gesture,data)
        }

        override fun onTouchEnd(gesture: TuSdkGestureRecognizer?, view: View?, event: MotionEvent, data: StepData?) {
            handleTransActionEnd(event)
        }
    }

    private var mOnDrawListener : ViewTreeObserver.OnPreDrawListener? = null

    constructor(context: Context?) : super(context){
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle){
    }

    override fun initView() {
        val vto = this.viewTreeObserver
        val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (!isLayouted){
                isLayouted = true

                mConfigTouchListner.isMultipleStablization = true
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

    override fun setCurrentLayerType(type: LayerType) {
        this.mCurrentLayerType = type
    }

    override fun getCurrentLayerType(): LayerType {
        return mCurrentLayerType
    }

    override fun setStickerLayerType(type: LayerType) {
        this.mCurrentType = type
    }

    override fun getStickerLayerType(): LayerType {
        return mCurrentType
    }

    override fun setParentFrame(frame: Rect) {
        this.mParentFrame = frame
    }

    override fun resize(frame: Rect) {
        if (mParentFrame.equals(frame)) return
        this.mParentFrame = frame
        var res : Future<Unit>? = mThreadPool?.submit(Callable<Unit> {
            updateViewSize()
        } )

        res?.get()
    }

    override fun setListener(listener: OnStickerLayerItemViewListener) {
        this.mListener = listener
    }

    override fun setEditor(editor: VideoEditor) {
        mEditor = editor
    }

    override fun setPlayerContext(context: EditorPlayerContext) {
        mPlayerContext = context
    }

    override fun setParentRect(rect: Rect) {
        mParentRect.set(rect)
    }

    override fun setThreadPool(threadPool: ExecutorService) {
        mThreadPool = threadPool
    }

    fun setLayerMaps(maps : HashMap<Int,LayerItemViewBase>){
        this.mLayerMap = maps
    }

    fun getLayerMaxDuration() : Long{
        return mMaxLayerDuration
    }

    fun getClipMaxDuration() : Long{
        return mClipMaxDuration
    }

    fun getClipStart() : Long{
        return mClipStartPos
    }

    fun getClipEnd() : Long{
        return mClipEndPos
    }

    override open fun createLayer() {
        mEditor!!.player?.lock()
        val layer = ClipLayer(mEditor!!.context,true)
        val layerConfig = Config()
        layerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,0)
        layerConfig.setNumber(Layer.CONFIG_START_POS,mLayerStartPos)
        layerConfig.setString(Layer.CONFIG_BLEND_MODE,Layer.BLEND_MODE_Default)
        layer.setConfig(layerConfig)
        val layerId = ++CURRENT_LAYER_ID
        mCurrentLayerId = layerId
        createClip(layer)
        mEditor!!.videoComposition().addLayer(layerId,layer)
        if (!mEditor!!.build()){

        }
        mEditor!!.player?.unlock()
        mResizeProperty.holder.blend_strength = 1.0

        mCurrentLayer = layer
        mCurrentLayerConfig = layerConfig
        if (mClipDuration == 0L){
            mClipDuration = layer!!.streamInfo.duration
        }
        mLayerEndPos = mLayerStartPos + layer!!.streamInfo.duration

        if (mMaxLayerDuration == 0L) mMaxLayerDuration = mLayerStartPos + layer!!.streamInfo.duration

        if (mCurrentLayerType == LayerType.Text || mCurrentLayerType == LayerType.Bubble) return
        if (mEditor!!.videoComposition().allLayers.size > 1){
            mResizeProperty.holder.pzr_zoom = 0.5
            layer.setProperty(Layer.PROP_OVERLAY,mResizeProperty.makeProperty())
            mScale = 0.5F
        }

        val view = this
        val posProperty =  Layer.InteractionInfo(layer.getProperty(Layer.PROP_INTERACTION_INFO))
        val streamInfo = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo)
        val hp = streamInfo.width / mParentFrame.width().toFloat()
        TLog.e("stream info ${streamInfo} hp ${hp} parent frame ${mParentFrame}")
        mLayerMap?.put(mCurrentLayerId,view)
        context.runOnUiThread {
            val width = (posProperty.width / hp).toInt() + ContextUtils.dip2px(view.context,26f)
            val height = (posProperty.height / hp).toInt() + ContextUtils.dip2px(view.context,26f)
            TLog.e("width ${posProperty.width} height ${posProperty.height} width ${width} height ${height}  width ${(posProperty.width / hp).toInt()} height ${(posProperty.height / hp).toInt()}")
            mDefaultViewSize = TuSdkSize(width,height)
            mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F,0f,posProperty.width.toFloat(),posProperty.height.toFloat())
            val pX = posProperty.posX * mParentFrame.width()
            val pY = posProperty.posY * mParentFrame.width()
            x = (pX - (width / 2f)).toFloat()
            y = (pY - (height / 2f)).toFloat()
            setViewSize(view, width,height)
            isLayout = true
            isSelected = true
            mTranslation = PointF(x,y)
            requestLayout()
            mThreadPool?.execute {
                mPlayerContext?.refreshFrame()
            }
        }
    }

    open fun restoreLayer(id : Int){
        mThreadPool?.execute {
            val layer = mEditor!!.videoComposition().allLayers[id] as ClipLayer
            val layerConfig = layer.config
            mLayerStartPos = layerConfig.getIntNumber(Layer.CONFIG_START_POS)
            mCurrentLayerId = id
            restoreClip(layer)
            var property: Property? = layer.getProperty(Layer.PROP_OVERLAY)
            val resizeHolder = if (property != null)Layer.OverlayPropertyHolder(property) else Layer.OverlayPropertyHolder()
            mResizeProperty.holder = resizeHolder
            mCurrentLayerConfig = layerConfig
            mCurrentLayer = layer
            mClipDuration = layer.streamInfo.duration
            mLayerStartPos = layerConfig.getIntNumber(Layer.CONFIG_START_POS)
            mLayerEndPos = mLayerStartPos + mClipDuration

            if (mMaxLayerDuration == 0L) mMaxLayerDuration = mLayerStartPos + layer!!.streamInfo.duration

            if (mCurrentLayerType == LayerType.Text || mCurrentLayerType == LayerType.Bubble) return@execute
            mScale = resizeHolder.pzr_zoom.toFloat()
            val view = this
            val posProperty =  Layer.InteractionInfo(layer.getProperty(Layer.PROP_INTERACTION_INFO))
            val streamInfo = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo)
            val hp = streamInfo.width / mParentFrame.width().toFloat()
            mLayerMap?.put(mCurrentLayerId,view)
            context.runOnUiThread {
                val width = (posProperty.width / hp).toInt() + ContextUtils.dip2px(view.context,24f)
                val height = (posProperty.height / hp).toInt() + ContextUtils.dip2px(view.context,24f)
                mDefaultViewSize = TuSdkSize(width,height)
                mCHypotenuse = RectHelper.getDistanceOfTwoPoints(0F,0f,posProperty.width.toFloat(),posProperty.height.toFloat())
                val pX = posProperty.posX * mParentFrame.width()
                val pY = posProperty.posY * mParentFrame.width()
                x = (pX - (width / 2f)).toFloat()
                y = (pY - (height / 2f)).toFloat()
                setViewSize(view, width,height)
                mDegree = posProperty.rotation.toFloat()
                rotation = mDegree
                isLayout = true
                isSelected = true
                mTranslation = PointF(x,y)
                requestLayout()
                isSelected = false
            }
        }
    }

    abstract fun restoreClip(layer: ClipLayer)

    override fun setViewSize(subView: View?, width: Int, height: Int) {
        TuSdkViewHelper.setViewWidth(subView,width)
        TuSdkViewHelper.setViewHeight(subView,height)
    }

    abstract fun createClip(layer: ClipLayer)

    abstract fun setClipDuration(start : Long,end : Long)

    open fun setLayerStartPos(start : Long){
        mLayerStartPos = start
        mLayerEndPos = mLayerStartPos + mClipDuration
        mEditor!!.player?.lock()
        mCurrentLayerConfig?.setNumber(Layer.CONFIG_START_POS,mLayerStartPos)
        mCurrentLayer?.setConfig(mCurrentLayerConfig)

        if (!mEditor!!.build()) {
            TLog.e("Editor reBuild failed")
            throw Exception()
        }
        mEditor!!.player?.unlock()
    }

    fun getClipDuration() : Long{
        return mClipDuration
    }

    fun getLayerStartPos() : Long{
        return mLayerStartPos
    }

    fun getLayerEndPos() : Long{
        return mLayerEndPos
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return mConfigTouchListner.onTouch(this,event)
    }

    open protected fun handleTransActionStart(event : MotionEvent){
        isMoveEvent = false
        mListener?.onItemSelected(this)
    }

    protected fun handleTransActionEnd(event: MotionEvent){
        if (!isMoveEvent){
            mListener?.onItemReleased(this)
        }
    }

    protected fun handleTransActionMove(gesture : TuSdkGestureRecognizer,event: MotionEvent){
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

        val cPointInLayer = PointF()

        cPointInLayer.set(mLastCenterPoint.x / mParentRect.width().toFloat(),mLastCenterPoint.y / mParentRect.height().toFloat())

        mThreadPool?.execute {
            updatePan(cPointInLayer)
            updateProperty()
        }
    }

    public open fun updateBlendMode(mode : String){
        mEditor!!.player.lock()
        mCurrentLayerConfig!!.setString(Layer.CONFIG_BLEND_MODE,mode)
        mCurrentLayer!!.setConfig(mCurrentLayerConfig)
        if (!mEditor!!.build()){

        }
        mEditor!!.player.unlock()
        mPlayerContext?.refreshFrame()
    }

    public open fun getBlendMode() : String{
        if (mCurrentLayerConfig == null) return BLEND_MODE_Default
        return mCurrentLayerConfig!!.getStringOr(Layer.CONFIG_BLEND_MODE,BLEND_MODE_Default)
    }

    public open fun getBlendMix() : Double{
        return mResizeProperty.holder.blend_strength
    }

    public open fun updateBlendMix(value : Double){
        mResizeProperty.holder.blend_strength = value
        updateProperty()
        mPlayerContext?.refreshFrame()
    }

    protected open fun updatePan(cPointInLayer: PointF) {
        mResizeProperty.holder.pzr_pan_x = cPointInLayer.x.toDouble()
        mResizeProperty.holder.pzr_pan_y = cPointInLayer.y.toDouble()
    }

    protected open fun updateProperty() {
        TLog.e("${mCurrentLayer} updateProperty")
        mCurrentLayer?.setProperty(Layer.PROP_OVERLAY,mResizeProperty.makeProperty())
        mPlayerContext?.refreshFrame()
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

        var res : Future<Unit>? = mThreadPool?.submit(Callable<Unit> {
            updateRotate()
            updateZoom()
            updateProperty()
            updateViewSize()
        })

        res?.get()
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
        var res : Future<Unit>? = mThreadPool?.submit(Callable<Unit> {
            updateRotate()
            updateZoom()
            updateProperty()
            updateViewSize()
        })

        res?.get()
    }

    protected open fun updateZoom() {
        mResizeProperty.holder.pzr_zoom = mScale.toDouble()
    }

    protected open fun updateRotate() {
        mResizeProperty.holder.pzr_rotate = mDegree.toDouble()
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



    protected fun getCenterOpposite(trans : PointF) : PointF{
        val oPoint = PointF()
        oPoint.x = trans.x + this.width * 0.5f
        oPoint.y = trans.y + this.height * 0.5f

        return oPoint
    }

    protected fun getCenterOpposite(trans : Rect) : PointF{
        val globalOffset = Point()
        getGlobalVisibleRect(trans, globalOffset)
        val centerPoint = PointF(trans.centerX().toFloat(), trans.centerY().toFloat())
        centerPoint[trans.centerX().toFloat()] = trans.centerY().toFloat()
        return centerPoint
    }

    protected fun computerScale(point: PointF,cPoint: PointF){
        val sDistance = RectHelper.getDistanceOfTwoPoints(cPoint,mLastPoint)

        val cDistance = RectHelper.getDistanceOfTwoPoints(cPoint,point)

        computerScale(cDistance - sDistance,cPoint)
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
//        mTranslation.offset((lastSize.width - scaleSize.width) * 0.5f,(lastSize.height - scaleSize.height) * 0.5f)



//        this.fixedMovePoint(mTranslation,outRect)
//        TLog.e("(x , y) $mTranslation current scale $mScale lastSize $lastSize scaleSize $scaleSize maxscale ${mMaxScale}")

//        setViewSize(this,scaleSize.width,scaleSize.height)
//        x = mTranslation.x
//        y = mTranslation.y


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

    open public fun requestShower(frame : Long){
        TLog.e("id ${mCurrentLayerId} start pos ${mLayerStartPos} end pos ${mLayerEndPos}")
        if (frame in (mLayerStartPos) until mLayerEndPos){
            visibility = View.VISIBLE
        } else {
            visibility = View.INVISIBLE
        }
    }

    protected open fun updateViewSize(){

        var interactionInfoProperty: Property? = mCurrentLayer!!.getProperty(Layer.PROP_INTERACTION_INFO)
                ?: return

        val posProperty =  Layer.InteractionInfo(interactionInfoProperty)
        val streamInfo = (mEditor!!.videoComposition().streamInfo as VideoStreamInfo)
        val hp = streamInfo.width / mParentFrame.width().toFloat()
        val lastSize = mDefaultViewSize.copy()
        TLog.e("current size w : ${posProperty.width} h : ${posProperty.height} scale ${mScale}")
        context.runOnUiThread {
            val width = (posProperty.width.absoluteValue / hp).toInt() + ContextUtils.dip2px(context,26f)
            val height = (posProperty.height.absoluteValue / hp).toInt() + ContextUtils.dip2px(context,26f)
            mDefaultViewSize = TuSdkSize(width,height)
            val scaleSize = mDefaultViewSize.copy()
            val pX = posProperty.posX * mParentFrame.width()
            val pY = posProperty.posY * mParentFrame.width()
            x = (pX - (width / 2f)).toFloat()
            y = (pY - (height / 2f)).toFloat()
            mTranslation.set(x,y)
            setViewSize(this@LayerItemViewBase, width,height)
            x = mTranslation.x
            y = mTranslation.y
        }
    }

}