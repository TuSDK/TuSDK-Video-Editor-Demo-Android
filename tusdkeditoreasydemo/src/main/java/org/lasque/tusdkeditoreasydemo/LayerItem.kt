/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/12/22$ 16:32$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import com.tusdk.pulse.editor.ClipLayer

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/12/22  16:32
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class LayerItem {

    var id : Int = -1
    val layer : ClipLayer
    val path : String

    constructor(id : Int,layer : ClipLayer,path : String){
        this.layer = layer
        this.path = path
        this.id = id
    }


}