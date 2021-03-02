/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base$
 *  @author  H.ys
 *  @Date    2020/10/23$ 10:21$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tusdk.pulse.editor.VideoEditor
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.ApiActivity
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import java.util.concurrent.ExecutorService

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/23  10:21
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
abstract class BaseFragment(type : FunctionType = FunctionType.Null) : Fragment() {

    protected var mEditor : VideoEditor? = null

    protected var mType = type

    protected var mVideoList : MutableList<AlbumInfo>? = null
    protected var mAudioList : MutableList<AlbumInfo>? = null

    protected var mThreadPool : ExecutorService? = null

    protected var mOnPlayerStateUpdateListener: OnPlayerStateUpdateListener? = null

    protected var mPlayerContext : EditorPlayerContext? = null

    abstract fun getLayoutId() : Int

    abstract fun initView(view: View, savedInstanceState: Bundle?)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getLayoutId(),container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view,savedInstanceState)
    }

    public fun setVideoEditor(editor: VideoEditor){
        mEditor = editor
    }

    public open fun setVideoList(list : MutableList<AlbumInfo>){
        this.mVideoList = list
    }

    public fun setAudioList(list : MutableList<AlbumInfo>){
        this.mAudioList = list
    }

    public fun setCurrentThreadPool(pool : ExecutorService){
        this.mThreadPool = pool
    }

    public fun setPlayerStateUpdateListener(listenerOn: OnPlayerStateUpdateListener){
        this.mOnPlayerStateUpdateListener = listenerOn
    }

    public fun setPlayerContext(editorPlayerContext: EditorPlayerContext){
        mPlayerContext = editorPlayerContext;
    }

    protected fun refreshEditor() {
        if (!mEditor!!.build()) {
            TLog.e("Editor reBuild failed")
            throw Exception()
        }
    }

    protected fun playerLock(){
        mEditor!!.player.lock()
    }

    protected fun playerUnlock(){
        mEditor!!.player.unlock()
    }

    protected fun setCanBackPressed(b : Boolean){
        (requireActivity() as ApiActivity).setCanBackPressed(b)
    }


}