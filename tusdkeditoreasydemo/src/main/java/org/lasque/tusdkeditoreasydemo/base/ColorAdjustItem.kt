/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base$
 *  @author  H.ys
 *  @Date    2020/11/26$ 17:22$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base

import com.tusdk.pulse.editor.effects.ColorAdjustEffect
import org.lasque.tusdkeditoreasydemo.R

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/26  17:22
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
enum class ColorAdjustItem(val key : String,val params : MutableList<ParamsItem>,val titleIds : Int,val propertyItem : ColorAdjustEffect.PropertyItem) {
    WhiteBalance(ColorAdjustEffect.PROP_TYPE_WhiteBalance,
            mutableListOf<ParamsItem>(ParamsItem(-1.0,1.0,0.0,R.string.lsq_adjust_color_temperature), ParamsItem(0.0,1.0,0.0,R.string.lsq_adjust_color_tone)),
            R.string.lsq_adjust_white_balance,
            ColorAdjustEffect.PropertyItem(ColorAdjustEffect.PROP_TYPE_WhiteBalance,0.0,0.0)
    ),
    HighlightShadow(ColorAdjustEffect.PROP_TYPE_HighlightShadow,
            mutableListOf(ParamsItem(0.0,1.0,0.0,R.string.lsq_adjust_highlight), ParamsItem(0.0,1.0,0.0,R.string.lsq_adjust_shadow)),
            R.string.lsq_adjust_highlight_shadow,
            ColorAdjustEffect.PropertyItem(ColorAdjustEffect.PROP_TYPE_HighlightShadow,0.0,0.0)
            ),
    Sharpen(ColorAdjustEffect.PROP_TYPE_Sharpen,
            mutableListOf(ParamsItem(-1.0,1.0,0.0,R.string.lsq_adjust_mixied)),
            R.string.lsq_adjust_sharpen,
            ColorAdjustEffect.PropertyItem(ColorAdjustEffect.PROP_TYPE_Sharpen,0.0)
            ),
    Brightness(ColorAdjustEffect.PROP_TYPE_Brightness,
            mutableListOf(ParamsItem(-1.0,1.0,0.0,R.string.lsq_adjust_mixied)),
            R.string.lsq_adjust_brightness,
            ColorAdjustEffect.PropertyItem(ColorAdjustEffect.PROP_TYPE_Brightness,0.0)
            ),
    Contrast(ColorAdjustEffect.PROP_TYPE_Contrast,
            mutableListOf(ParamsItem(0.0,1.0,0.0,R.string.lsq_adjust_mixied)),
            R.string.lsq_adjust_contrast,
            ColorAdjustEffect.PropertyItem(ColorAdjustEffect.PROP_TYPE_Contrast,0.0)
            ),
    Saturation(ColorAdjustEffect.PROP_TYPE_Saturation,
            mutableListOf(ParamsItem(-1.0,1.0,0.0,R.string.lsq_adjust_mixied)),
            R.string.lsq_adjust_saturation,
            ColorAdjustEffect.PropertyItem(ColorAdjustEffect.PROP_TYPE_Saturation,0.0)
            ),
    Exposure(ColorAdjustEffect.PROP_TYPE_Exposure,
            mutableListOf(ParamsItem(-1.0,1.0,0.0,R.string.lsq_adjust_mixied)),
            R.string.lsq_adjust_exposure,
            ColorAdjustEffect.PropertyItem(ColorAdjustEffect.PROP_TYPE_Exposure,0.0)
            )
    ;

    fun resetValue(){
        for (i in 0 until params.size){
            params[i].defaultValue = 0.0
            propertyItem.values[i] = 0.0
        }
    }
}

data class ParamsItem(val min : Double,val max : Double,var defaultValue : Double,val titleId : Int)