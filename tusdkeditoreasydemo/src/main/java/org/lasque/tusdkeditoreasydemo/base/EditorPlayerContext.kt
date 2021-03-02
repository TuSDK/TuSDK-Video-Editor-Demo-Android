/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base$
 *  @author  H.ys
 *  @Date    2020/11/19$ 10:40$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base

import com.tusdk.pulse.Player
import com.tusdk.pulse.editor.VideoEditor

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/19  10:40
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */

data class EditorPlayerContext(val editor: VideoEditor,var currentFrame : Long = 0,var state : Player.State = Player.State.kREADY){

    fun refreshFrame(){
        editor.player?.previewFrame(currentFrame)
    }
}