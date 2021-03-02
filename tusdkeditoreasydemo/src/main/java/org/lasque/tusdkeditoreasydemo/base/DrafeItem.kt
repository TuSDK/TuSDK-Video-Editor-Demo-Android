/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base$
 *  @author  H.ys
 *  @Date    2020/12/14$ 17:31$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base

import org.lasque.tusdkeditoreasydemo.FunctionType

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/12/14  17:31
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class DraftItem {

    companion object {
        const val Draft_List_Key ="d-list-key"
        const val Draft_Path_Key = "d-path-key"
    }

    val funcType : Int
    val draftPath : String
    val saveDate : String
    val fileName : String

    constructor(type: Int,path : String,time : String,name : String){
        funcType = type
        draftPath = path
        saveDate = time
        fileName = name
    }


}