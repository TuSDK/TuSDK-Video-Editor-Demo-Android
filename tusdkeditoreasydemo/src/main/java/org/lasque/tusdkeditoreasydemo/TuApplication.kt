/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/10/26$ 14:12$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.tusdk.pulse.editor.TuVideoClipSDK
import org.lasque.tusdkpulse.core.TuSdk
import org.lasque.tusdkpulse.core.TuSdkApplication

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/26  14:12
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class TuApplication : TuSdkApplication() {

    override fun onCreate() {
        super.onCreate()
        TuVideoClipSDK.register()

        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this))
        // 设置输出状态，建议在接入阶段开启该选项，以便定位问题。
        // 设置输出状态，建议在接入阶段开启该选项，以便定位问题。
        this.isEnableLog = true
        /**
         *  初始化SDK，应用密钥是您的应用在 TuSDK 的唯一标识符。每个应用的包名(Bundle Identifier)、密钥、资源包(滤镜、贴纸等)三者需要匹配，否则将会报错。
         *
         *  @param appkey 应用秘钥 (请前往 http://tusdk.com 申请秘钥)
         */
        /**
         * 初始化SDK，应用密钥是您的应用在 TuSDK 的唯一标识符。每个应用的包名(Bundle Identifier)、密钥、资源包(滤镜、贴纸等)三者需要匹配，否则将会报错。
         *
         * @param appkey 应用秘钥 (请前往 http://tusdk.com 申请秘钥)
         */
        TuSdk.setResourcePackageClazz(R::class.java)
        this.initPreLoader(this.applicationContext, "107c8c5e4486977c-04-ewdjn1",null,null)

    }
}