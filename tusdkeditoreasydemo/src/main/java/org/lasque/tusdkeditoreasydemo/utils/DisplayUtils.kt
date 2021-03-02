package org.lsque.tusdkevademo.utils

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration

/**
 * TuSDK
 * $desc$
 *
 * @author        H.ys
 * @Date        $data$ $time$
 * @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 */
object DisplayUtils {

    /**
     * 适配：修改设备密度
     */
    private var sNoncompatDensity: Float = 0.toFloat()
    private var sNoncompatScaledDensity: Float = 0.toFloat()

    fun setCustomDensity(activity: Activity, application: Application) {
        val appDisplayMetrics = application.getResources().getDisplayMetrics()
        if (sNoncompatDensity == 0f) {
            sNoncompatDensity = appDisplayMetrics.density
            sNoncompatScaledDensity = appDisplayMetrics.scaledDensity
            // 防止系统切换后不起作用
            application.registerComponentCallbacks(object : ComponentCallbacks {
               override fun onLowMemory() {

                }

                override fun onConfigurationChanged(newConfig: Configuration) {
                    if (newConfig != null && newConfig!!.fontScale > 0) {
                        sNoncompatScaledDensity = application.getResources().getDisplayMetrics().scaledDensity
                    }
                }
            })
        }
        val targetDensity = appDisplayMetrics.widthPixels / 375f
        // 防止字体变小
        val targetScaleDensity = targetDensity * (sNoncompatScaledDensity / sNoncompatDensity)
        val targetDensityDpi = (160 * targetDensity).toInt()

        appDisplayMetrics.density = targetDensity
        appDisplayMetrics.scaledDensity = targetScaleDensity
        appDisplayMetrics.densityDpi = targetDensityDpi

        val activityDisplayMetrics = activity.getResources().getDisplayMetrics()
        activityDisplayMetrics.density = targetDensity
        activityDisplayMetrics.scaledDensity = targetScaleDensity
        activityDisplayMetrics.densityDpi = targetDensityDpi

    }
}