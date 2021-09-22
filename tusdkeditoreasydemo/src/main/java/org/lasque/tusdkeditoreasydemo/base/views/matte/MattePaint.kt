package org.lasque.tusdkeditoreasydemo.base.views.matte

import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import org.lasque.tusdkpulse.core.struct.TuSdkSize

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views.matte
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/8/18  16:45
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
interface MattePaint {

    var centerPoint : PointF;
    var size : TuSdkSize;
    var rotate : Double;
    fun onDraw(canvas : Canvas)

    fun update(center : PointF, size : TuSdkSize, rotate : Double)

    fun updateCanvasSize(width : Int,height : Int)

    fun updateRect(rect : Rect)

}