/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.base$
 *  @author  H.ys
 *  @Date    2021/4/19$ 16:40$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base

import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.GraffitiClip

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/4/19  16:40
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */

data class GraffitiItem(val id : Int,val layer : Layer,val graffitiClip : Clip,val graffitiProperty : GraffitiClip.PropertyBuilder)