/**
 *  TuSDK
 *  VEDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2021/1/19$ 17:05$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.app.Activity
import android.os.Bundle
import kotlinx.android.synthetic.main.splash_activity.*
import org.jetbrains.anko.startActivity
import org.lasque.tusdkeditoreasydemo.base.BaseActivity
import org.lasque.tusdkpulse.core.utils.ThreadHelper

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * VEDemo
 *
 * @author        H.ys
 * @Date        2021/1/19  17:05
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_activity)
        lsq_splash_img.postDelayed({
            startActivity<MainActivity>()
            overridePendingTransition(R.anim.lsq_fade_in,R.anim.lsq_fade_out)
            finish()
        },2000)
    }
}