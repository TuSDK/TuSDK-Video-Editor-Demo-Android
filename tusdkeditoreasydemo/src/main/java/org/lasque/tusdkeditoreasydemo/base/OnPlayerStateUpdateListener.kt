package org.lasque.tusdkeditoreasydemo.base

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/27  10:41
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
interface OnPlayerStateUpdateListener {

    fun onDurationUpdate()

    fun onPlayerPlay()

    fun onPlayerPause()

    fun onRefreshFrame()
}