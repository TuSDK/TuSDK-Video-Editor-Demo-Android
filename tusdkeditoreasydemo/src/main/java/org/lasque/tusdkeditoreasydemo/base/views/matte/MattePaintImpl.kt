/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.base.views.matte$
 *  @author  H.ys
 *  @Date    2021/8/27$ 14:42$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views.matte

import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import androidx.annotation.ColorRes
import androidx.core.graphics.drawable.toBitmap
import org.jetbrains.anko.matchParent
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.utils.RectHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views.matte
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/8/27  14:42
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */

class MatteLinearPaint : MattePaint{

    var paint : Paint = Paint()

    override var centerPoint: PointF = PointF(0f,0f)

    override var size: TuSdkSize = TuSdkSize(0,0)

    override var rotate: Double = 0.0

    private var canvasSize = TuSdkSize(0,0)

    private var bitmapPaint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var rect : Rect = Rect()

    override fun updateRect(rect: Rect) {
        this.rect = rect
    }

    init {
        paint.color = Color.WHITE
        paint.isAntiAlias = true
        //防抖动
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ContextUtils.dip2px(TuSdkContext.context(),2f).toFloat()
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        bitmapPaint.isFilterBitmap = true
        bitmapPaint.isDither = true
    }

    override fun onDraw(canvas: Canvas) {
        paint.isFilterBitmap = false
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.xfermode = null

        val canvasCenter = PointF(
            (canvasSize.width - rect.right) + (centerPoint.x * rect.width()),
            rect.top + (centerPoint.y * rect.height())
        )

        val matrix = Matrix()
        matrix.preRotate(rotate.toFloat(),canvasCenter.x,canvasCenter.y)

        val circleHalfR = ContextUtils.dip2px(TuSdkContext.context(),5f).toFloat()

        var dst = FloatArray(2)
        var src = FloatArray(2)


        var firstStartPoint = PointF(-4f * rect.width(),canvasCenter.y)
        var firstEndPoint = PointF(canvasCenter.x - circleHalfR,canvasCenter.y)
        var secondStartPoint = PointF(
            canvasCenter.x + circleHalfR,canvasCenter.y
        )
        var secondEndPoint = PointF(4f * rect.width(),canvasCenter.y)

        src[0] = firstStartPoint.x
        src[1] = firstStartPoint.y

        matrix.mapPoints(dst,src)
        firstStartPoint.x = dst[0]
        firstStartPoint.y = dst[1]

        src[0] = firstEndPoint.x
        src[1] = firstEndPoint.y
        matrix.mapPoints(dst, src)
        firstEndPoint.x = dst[0]
        firstEndPoint.y = dst[1]

        src[0] = secondStartPoint.x
        src[1] = secondStartPoint.y
        matrix.mapPoints(dst, src)
        secondStartPoint.x = dst[0]
        secondStartPoint.y = dst[1]

        src = FloatArray(2)
        src.set(0,secondEndPoint.x)
        src.set(1,secondEndPoint.y)

        matrix.mapPoints(dst, src)
        secondEndPoint.x = dst[0]
        secondEndPoint.y = dst[1]

        canvas.drawLine(firstStartPoint.x,firstStartPoint.y, firstEndPoint.x,firstEndPoint.y,paint)
        canvas.drawLine(secondStartPoint.x,secondStartPoint.y, secondEndPoint.x,secondEndPoint.y,paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        canvas.drawCircle(canvasCenter.x,canvasCenter.y,
            ContextUtils.dip2px(TuSdkContext.context(),5f).toFloat(),paint)

    }

    override fun update(center: PointF, size: TuSdkSize, rotate: Double) {
        centerPoint.set(center)
        this.size.set(size)
        this.rotate = rotate
    }

    override fun updateCanvasSize(width: Int, height: Int) {
        canvasSize.set(width,height)
    }



}

class MatteMirrorPaint : MattePaint{

    var paint : Paint = Paint()

    override var centerPoint: PointF = PointF(0f,0f)

    override var size: TuSdkSize = TuSdkSize(0,0)

    override var rotate: Double = 0.0

    private var canvasSize = TuSdkSize(0,0)

    private var rect : Rect = Rect()

    override fun updateRect(rect: Rect) {
        this.rect = rect
    }

    init {
        paint.color = Color.WHITE
        paint.isAntiAlias = true
        //防抖动
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ContextUtils.dip2px(TuSdkContext.context(),2f).toFloat()
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE

        val canvasCenter = PointF(
            (canvasSize.width - rect.right) + (centerPoint.x * rect.width()),
            rect.top + (centerPoint.y * rect.height())
        )

        val matrix = Matrix()
        matrix.preRotate(rotate.toFloat(),canvasCenter.x,canvasCenter.y)

        var dst = FloatArray(2)
        var src = FloatArray(2)

        var matteHeightPercent = size.height.toFloat() / 2f

        var topStartPoint = PointF(-4f * rect.width(),canvasCenter.y - matteHeightPercent)
        var topEndPoint = PointF(4f * rect.width(),canvasCenter.y - matteHeightPercent)

        val bottomStartPoint = PointF(-4f * rect.width(),canvasCenter.y + matteHeightPercent)
        val bottomEndPoint = PointF(4f * rect.width(),canvasCenter.y + matteHeightPercent)

        src[0] = topStartPoint.x
        src[1] = topStartPoint.y

        matrix.mapPoints(dst,src)

        topStartPoint.x = dst[0]
        topStartPoint.y = dst[1]

        src[0] = topEndPoint.x
        src[1] = topEndPoint.y

        matrix.mapPoints(dst, src)

        topEndPoint.x = dst[0]
        topEndPoint.y = dst[1]

        src[0] = bottomStartPoint.x
        src[1] = bottomStartPoint.y

        matrix.mapPoints(dst, src)

        bottomStartPoint.x = dst[0]
        bottomStartPoint.y = dst[1]

        src[0] = bottomEndPoint.x
        src[1] = bottomEndPoint.y

        matrix.mapPoints(dst, src)

        bottomEndPoint.x = dst[0]
        bottomEndPoint.y = dst[1]

        canvas.drawLine(topStartPoint.x,
             rect.top + topStartPoint.y,
            topEndPoint.x,
            rect.top + topEndPoint.y,paint)

        canvas.drawLine(bottomStartPoint.x,
             rect.top + bottomStartPoint.y,
            bottomEndPoint.x,
            rect.top + bottomEndPoint.y,paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        canvas.drawCircle(canvasCenter.x,canvasCenter.y,
            ContextUtils.dip2px(TuSdkContext.context(),5f).toFloat(),paint)

    }

    override fun update(center: PointF, size: TuSdkSize, rotate: Double) {
        centerPoint.set(center)
        this.size.set(size)
        this.rotate = rotate
    }

    override fun updateCanvasSize(width: Int, height: Int) {
        canvasSize.set(width,height)
    }

}

class MatteCirclePaint : MattePaint{
    var paint : Paint = Paint()

    override var centerPoint: PointF = PointF(0f,0f)

    override var size: TuSdkSize = TuSdkSize(0,0)

    override var rotate: Double = 0.0

    private var canvasSize = TuSdkSize(0,0)

    private var bitmapPaint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val centerBitmap : Bitmap

    private val topArrow : Bitmap

    private val bottomArrow : Bitmap

    private val leftAndRightLine : Bitmap

    private var rect : Rect = Rect()

    override fun updateRect(rect: Rect) {
        this.rect = rect
    }

    init {
        paint.color = Color.WHITE
        paint.isAntiAlias = true
        //防抖动
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ContextUtils.dip2px(TuSdkContext.context(),2f).toFloat()
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        centerBitmap = BitmapHelper.getResDrawable(TuSdkContext.context(),R.drawable.mask_paint_center).toBitmap()

        topArrow = BitmapHelper.getResDrawable(TuSdkContext.context(),R.drawable.mask_paint_top).toBitmap()

        bottomArrow = BitmapHelper.getResDrawable(TuSdkContext.context(),R.drawable.mask_paint_bottom).toBitmap()

        leftAndRightLine = BitmapHelper.getResDrawable(TuSdkContext.context(),R.drawable.mask_paint_left_right).toBitmap()

        bitmapPaint.isFilterBitmap = true
        bitmapPaint.isDither = true
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        val canvasCenter = PointF(centerPoint.x * canvasSize.width,centerPoint.y * canvasSize.height)

        val matrix = Matrix()
        matrix.preTranslate(canvasCenter.x,canvasCenter.y)
        matrix.preRotate(rotate.toFloat())
        matrix.preTranslate(-canvasCenter.x,-canvasCenter.y)

        canvas.rotate(rotate.toFloat(),canvasCenter.x,canvasCenter.y)

        var matteRect = RectF(
            canvasCenter.x - size.width / 2,
            canvasCenter.y - size.height / 2,
            canvasCenter.x + size.width / 2,
            canvasCenter.y + size.height / 2
        )


        canvas.drawOval(matteRect,paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        canvas.drawCircle(canvasCenter.x,canvasCenter.y,
            ContextUtils.dip2px(TuSdkContext.context(),5f).toFloat(),paint)

        val topArrowSize = TuSdkSize.create(topArrow)
        val topArrowRect = Rect(
            (canvasCenter.x - topArrowSize.width / 2).toInt(),
            (matteRect.top - 8 - topArrowSize.height).toInt(),
            (canvasCenter.x + topArrowSize.width / 2).toInt(),
            (matteRect.top - 8).toInt()
        )
        canvas.drawBitmap(topArrow,topArrowRect.left.toFloat(),topArrowRect.top.toFloat(),bitmapPaint)


        val bottomArrowSize = TuSdkSize.create(bottomArrow)
        val bottomArrowRect = Rect(
            (canvasCenter.x - bottomArrowSize.width / 2).toInt(),
            (matteRect.bottom + 8 ).toInt(),
            (canvasCenter.x + bottomArrowSize.width / 2).toInt(),
            (matteRect.top + 8 + bottomArrowSize.height).toInt()
        )
        canvas.drawBitmap(bottomArrow,bottomArrowRect.left.toFloat(),bottomArrowRect.top.toFloat(),bitmapPaint)

        val lineSize = TuSdkSize.create(leftAndRightLine)
        val leftLineRect = Rect(
            (matteRect.left - 8 - lineSize.width).toInt(),
            (canvasCenter.y - lineSize.height / 2).toInt(),
            (matteRect.left - 8).toInt(),
            (canvasCenter.y + lineSize.height / 2).toInt()
        )
        canvas.drawBitmap(leftAndRightLine,leftLineRect.left.toFloat(),leftLineRect.top.toFloat(),bitmapPaint)

        val rightLineRect = RectF(
            matteRect.right + 8,
            canvasCenter.y - lineSize.height / 2,
            matteRect.right + 8 + lineSize.width,
            canvasCenter.y + lineSize.height / 2
        )
        canvas.drawBitmap(leftAndRightLine,rightLineRect.left,rightLineRect.top,bitmapPaint)
    }

    override fun update(center: PointF, size: TuSdkSize, rotate: Double) {
        centerPoint.set(center)
        this.size.set(size)
        this.rotate = rotate
    }

    override fun updateCanvasSize(width: Int, height: Int) {
        canvasSize.set(width,height)
    }
}


class MatteRectPaint : MattePaint{

    var paint : Paint = Paint()

    override var centerPoint: PointF = PointF(0f,0f)

    override var size: TuSdkSize = TuSdkSize(0,0)

    override var rotate: Double = 0.0

    private var canvasSize = TuSdkSize(0,0)

    private var radius = 0.0

    private var bitmapPaint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val centerBitmap : Bitmap

    private val topArrow : Bitmap

    private val bottomArrow : Bitmap

    private val leftAndRightLine : Bitmap

    private val arcBitmap : Bitmap

    private var rect : Rect = Rect()

    override fun updateRect(rect: Rect) {
        this.rect = rect
    }


    init {
        paint.color = Color.WHITE
        paint.isAntiAlias = true
        //防抖动
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ContextUtils.dip2px(TuSdkContext.context(),2f).toFloat()
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        centerBitmap = BitmapHelper.getResDrawable(TuSdkContext.context(),R.drawable.mask_paint_center).toBitmap()

        topArrow = BitmapHelper.getResDrawable(TuSdkContext.context(),R.drawable.mask_paint_top).toBitmap()

        bottomArrow = BitmapHelper.getResDrawable(TuSdkContext.context(),R.drawable.mask_paint_bottom).toBitmap()

        leftAndRightLine = BitmapHelper.getResDrawable(TuSdkContext.context(),R.drawable.mask_paint_left_right).toBitmap()

        arcBitmap = BitmapHelper.getResDrawable(TuSdkContext.context(),R.drawable.mask_paint_rotate).toBitmap()

        bitmapPaint.isFilterBitmap = true
        bitmapPaint.isDither = true
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        val canvasCenter = PointF(centerPoint.x * canvasSize.width,centerPoint.y * canvasSize.height)

        val matrix = Matrix()
        matrix.preTranslate(canvasCenter.x,canvasCenter.y)
        matrix.preRotate(rotate.toFloat())
        matrix.preTranslate(-canvasCenter.x,-canvasCenter.y)

        canvas.rotate(rotate.toFloat(),canvasCenter.x,canvasCenter.y)

        var matteRect = RectF(
            canvasCenter.x - size.width / 2,
            canvasCenter.y - size.height / 2,
            canvasCenter.x + size.width / 2,
            canvasCenter.y + size.height / 2
        )


        canvas.drawRoundRect(matteRect,
            (radius * matteRect.width()).toFloat(), (radius * matteRect.height()).toFloat(),paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        canvas.drawCircle(canvasCenter.x,canvasCenter.y,
            ContextUtils.dip2px(TuSdkContext.context(),5f).toFloat(),paint)


        val topArrowSize = TuSdkSize.create(topArrow)
        val topArrowRect = Rect(
            (canvasCenter.x - topArrowSize.width / 2).toInt(),
            (matteRect.top - 8 - topArrowSize.height).toInt(),
            (canvasCenter.x + topArrowSize.width / 2).toInt(),
            (matteRect.top - 8).toInt()
        )
        canvas.drawBitmap(topArrow,topArrowRect.left.toFloat(),topArrowRect.top.toFloat(),bitmapPaint)


        val bottomArrowSize = TuSdkSize.create(bottomArrow)
        val bottomArrowRect = Rect(
            (canvasCenter.x - bottomArrowSize.width / 2).toInt(),
            (matteRect.bottom + 8 ).toInt(),
            (canvasCenter.x + bottomArrowSize.width / 2).toInt(),
            (matteRect.top + 8 + bottomArrowSize.height).toInt()
        )
        canvas.drawBitmap(bottomArrow,bottomArrowRect.left.toFloat(),bottomArrowRect.top.toFloat(),bitmapPaint)

        val lineSize = TuSdkSize.create(leftAndRightLine)
        val leftLineRect = Rect(
            (matteRect.left - 8 - lineSize.width).toInt(),
            (canvasCenter.y - lineSize.height / 2).toInt(),
            (matteRect.left - 8).toInt(),
            (canvasCenter.y + lineSize.height / 2).toInt()
        )
        canvas.drawBitmap(leftAndRightLine,leftLineRect.left.toFloat(),leftLineRect.top.toFloat(),bitmapPaint)

        val rightLineRect = RectF(
            matteRect.right + 8,
            canvasCenter.y - lineSize.height / 2,
            matteRect.right + 8 + lineSize.width,
            canvasCenter.y + lineSize.height / 2
        )
        canvas.drawBitmap(leftAndRightLine,rightLineRect.left,rightLineRect.top,bitmapPaint)

//        val arcSize = TuSdkSize.create(arcBitmap)
//        val arcRect = RectF(
//            matteRect.left - arcSize.width / 2 - 16,
//            matteRect.bottom - arcSize.height / 2 + 16,
//            0f,0f
//        )
//        canvas.drawBitmap(arcBitmap,arcRect.left,arcRect.top,bitmapPaint)
    }

    override fun update(center: PointF, size: TuSdkSize, rotate: Double) {
        centerPoint.set(center)
        this.size.set(size)
        this.rotate = rotate
    }

    override fun updateCanvasSize(width: Int, height: Int) {
        canvasSize.set(width,height)
    }

    fun updateRadius(radius : Double){
        this.radius = radius
    }

}

class MatteLovePaint : MattePaint{

    var paint : Paint = Paint()

    override var centerPoint: PointF = PointF(0f,0f)

    override var size: TuSdkSize = TuSdkSize(0,0)

    override var rotate: Double = 0.0

    private var canvasSize = TuSdkSize(0,0)

    private var loveDrawable : Drawable

    private var rect : Rect = Rect()

    override fun updateRect(rect: Rect) {
        this.rect = rect
    }

    init {
        paint.color = Color.WHITE
        paint.isAntiAlias = true
        //防抖动
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ContextUtils.dip2px(TuSdkContext.context(),2f).toFloat()
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        loveDrawable = TuSdkContext.getDrawable(R.drawable.mask_love_a)
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        val canvasCenter = PointF(centerPoint.x * canvasSize.width,centerPoint.y * canvasSize.height)

        val matrix = Matrix()
        matrix.preTranslate(canvasCenter.x,canvasCenter.y)
        matrix.preRotate(rotate.toFloat())
        matrix.preTranslate(-canvasCenter.x,-canvasCenter.y)

        canvas.rotate(rotate.toFloat(),canvasCenter.x,canvasCenter.y)

        var matteRect = RectF(
            canvasCenter.x - size.width / 2,
            canvasCenter.y - size.height / 2,
            canvasCenter.x + size.width / 2,
            canvasCenter.y + size.height / 2
        )

        loveDrawable.setBounds(
            matteRect.left.toInt(),
            matteRect.top.toInt(),
            matteRect.right.toInt(),
            matteRect.bottom.toInt()
        )
//        loveDrawable.alpha = 50;

        loveDrawable.draw(canvas)

        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        canvas.drawCircle(canvasCenter.x,canvasCenter.y,
            ContextUtils.dip2px(TuSdkContext.context(),5f).toFloat(),paint)
    }

    override fun update(center: PointF, size: TuSdkSize, rotate: Double) {
        centerPoint.set(center)
        this.size.set(size)
        this.rotate = rotate
    }

    override fun updateCanvasSize(width: Int, height: Int) {
        canvasSize.set(width,height)
    }

}

class MatteStarPaint : MattePaint{
    var paint : Paint = Paint()

    override var centerPoint: PointF = PointF(0f,0f)

    override var size: TuSdkSize = TuSdkSize(0,0)

    override var rotate: Double = 0.0

    private var canvasSize = TuSdkSize(0,0)

    private var starDrawable : Drawable

    private var rect : Rect = Rect()

    override fun updateRect(rect: Rect) {
        this.rect = rect
    }

    init {
        paint.color = Color.WHITE
        paint.isAntiAlias = true
        //防抖动
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ContextUtils.dip2px(TuSdkContext.context(),2f).toFloat()
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        starDrawable = TuSdkContext.getDrawable(R.drawable.mask_star_a)
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        val canvasCenter = PointF(centerPoint.x * canvasSize.width,centerPoint.y * canvasSize.height)

        val matrix = Matrix()
        matrix.preTranslate(canvasCenter.x,canvasCenter.y)
        matrix.preRotate(rotate.toFloat())
        matrix.preTranslate(-canvasCenter.x,-canvasCenter.y)

        canvas.rotate(rotate.toFloat(),canvasCenter.x,canvasCenter.y)

        var matteRect = RectF(
            canvasCenter.x - size.width / 2,
            canvasCenter.y - size.height / 2,
            canvasCenter.x + size.width / 2,
            canvasCenter.y + size.height / 2
        )

        starDrawable.setBounds(
            matteRect.left.toInt(),
            matteRect.top.toInt(),
            matteRect.right.toInt(),
            matteRect.bottom.toInt()
        )
        starDrawable.draw(canvas)

        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        canvas.drawCircle(canvasCenter.x,canvasCenter.y,
            ContextUtils.dip2px(TuSdkContext.context(),5f).toFloat(),paint)
    }

    override fun update(center: PointF, size: TuSdkSize, rotate: Double) {
        centerPoint.set(center)
        this.size.set(size)
        this.rotate = rotate
    }

    override fun updateCanvasSize(width: Int, height: Int) {
        canvasSize.set(width,height)
    }
}