package org.lasque.tusdkeditoreasydemo.apis

import org.lasque.tusdkeditoreasydemo.R

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/10  15:25
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
enum class VideoRatio(width : Int,height : Int,iconId : Int,selIconId : Int,titleId : Int) {

    Ratio_16_9(16,9,R.mipmap.crop_16_9_nor,R.mipmap.crop_16_9_sel,R.string.lsq_trim_16_9),
    Ratio_3_2(3,2,R.mipmap.crop_3_2_nor,R.mipmap.crop_3_2_sel,R.string.lsq_trim_3_2),
    Ratio_4_3(4,3,R.mipmap.crop_4_3_nor,R.mipmap.crop_4_3_sel,R.string.lsq_trim_4_3),
    Ratio_1_1(1,1,R.mipmap.crop_1_1_nor,R.mipmap.crop_1_1_sel,R.string.lsq_trim_1_1),
    Ratio_3_4(3,4,R.mipmap.crop_3_4_nor,R.mipmap.crop_3_4_sel,R.string.lsq_trim_3_4),
    Ratio_2_3(2,3,R.mipmap.crop_2_3_nor,R.mipmap.crop_2_3_sel,R.string.lsq_trim_2_3),
    Ratio_9_16(9,16,R.mipmap.crop_9_16_nor,R.mipmap.crop_9_16_sel,R.string.lsq_trim_9_16),
    ;

    public val width = width
    public val height = height
    public val iconId = iconId
    public val titleId = titleId
    public val selIconId = selIconId
}