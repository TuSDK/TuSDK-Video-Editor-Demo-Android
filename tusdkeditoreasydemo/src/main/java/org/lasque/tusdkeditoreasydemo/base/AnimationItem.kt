/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.base$
 *  @author  H.ys
 *  @Date    2021/5/10$ 11:04$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base

import android.content.Context
import android.text.TextUtils
import com.tusdk.pulse.utils.AssetsMapper
import org.lasque.tusdkpulse.core.TuSdkContext

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/5/10  11:04
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AnimationItem {

    public val itemCode : String
    public val itemFileName : String
    public val itemIndex : Int

    constructor(name : String,index : Int){
        itemCode = name
        itemIndex = index
        itemFileName = "lsq_animation_text_${index}.at"
    }

    public fun getAnimationFilePath() : String?{
        val sp = TuSdkContext.context().getSharedPreferences("TuSDK",Context.MODE_PRIVATE)

        var path = sp.getString(itemCode,"")
        if (TextUtils.isEmpty(path)){
            val mapper = AssetsMapper(TuSdkContext.context())
            path = mapper.mapAsset("animation/${itemFileName}")

            if (!TextUtils.isEmpty(path)){
                sp.edit().putString(itemCode,path).apply()
                return path
            }
        }
        return path
    }

    public fun getThumbId() : Int{
        return TuSdkContext.getRawResId("lsq_anitext_thumb_${itemIndex}")
    }

    public fun getName() : Int{
        return TuSdkContext.getStringResId("lsq_anitext_name_${itemIndex}")
    }



}