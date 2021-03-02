/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo.base.views.stickers$
 *  @author  H.ys
 *  @Date    2020/11/16$ 16:15$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.base.views.stickers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.tusdk.pulse.Blob
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.AudioFileClip
import com.tusdk.pulse.editor.clips.ImageClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.view.TuSdkImageView
import org.lasque.tusdkeditoreasydemo.base.views.TuSdkImageButton
import org.lasque.tusdkpulse.core.utils.TLog
import kotlin.math.max

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base.views.stickers
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/16  16:15
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class VideoLayerItemView : LayerItemViewBase {

    companion object {
        const val Video_CLIP_ID = 300

        fun getLayoutId(): Int {
            return TuSdkContext.getLayoutResId("tusdk_impl_component_widget_sticker_video_item_view")
        }
    }

    /** ------------------------------- View ------------------------------------------------- */

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /************************* view  */
    /** 图片视图  */
    private var mImageView: TuSdkImageView? = null

    /** 取消按钮  */
    private var mCancelButton: TuSdkImageButton? = null

    /** 旋转缩放按钮  */
    private var mTurnButton: TuSdkImageButton? = null

    private var mStrokeWidth = 1

    private var mStrokeColor = Color.TRANSPARENT

    private val mButtonClickListener = object : OnClickListener {
        override fun onClick(v: View?) {
            if (equalViewIds(v, getCancelButton())) {
                handleCancelButton()
            }
        }

    }

    /** 图片视图  */
    fun getImageView(): TuSdkImageView? {
        if (mImageView == null) {
            mImageView = this.getViewById("lsq_sticker_imageView")
        }
        return mImageView
    }

    /** 取消按钮  */
    fun getCancelButton(): TuSdkImageButton {
        if (mCancelButton == null) {
            mCancelButton = this.getViewById("lsq_sticker_cancelButton")
            if (mCancelButton != null) {
                mCancelButton!!.setOnClickListener(mButtonClickListener)
            }
        }
        return mCancelButton!!
    }

    /** 旋转缩放按钮  */
    @SuppressLint("ClickableViewAccessibility")
    fun getTurnButton(): TuSdkImageButton {
        if (mTurnButton == null) {
            mTurnButton = this.getViewById("lsq_sticker_turnButton")
            if (mTurnButton != null) {
                mTurnButton!!.setOnTouchListener(mOnTouchListener)
            }
        }
        return mTurnButton!!
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (mCurrentType != LayerType.ImageAndVideo) {
            if (mCurrentLayerType != mCurrentType)
                false
            else {
                super.onTouchEvent(event)
            }
        } else super.onTouchEvent(event)
    }

    override fun setStroke(color: Int, width: Int) {
        mStrokeColor = color
        mStrokeWidth = max(width, 0)
        getImageView()?.setStroke(mStrokeColor, mStrokeWidth)
    }

    private fun handleCancelButton() {
        mListener?.onItemClose(this)

        mThreadPool?.execute {
            mEditor!!.player.lock()
            mEditor!!.videoComposition().deleteLayer(mCurrentLayerId)
            mEditor!!.audioComposition().deleteLayer(mCurrentLayerId)
            if (!mEditor!!.build()){

            }
            mEditor!!.player.unlock()
            mPlayerContext?.refreshFrame()

        }
    }

    override fun loadView() {
        super.loadView()
        // 图片视图
        getImageView()
        // 取消按钮
        getCancelButton()
        // 旋转缩放按钮
        getTurnButton()
    }

    override fun setSelected(selected: Boolean) {
        val color = if (selected) {
            mStrokeColor
        } else {
            Color.TRANSPARENT
        }
        getImageView()?.setStroke(color, mStrokeWidth)
        showViewIn(getCancelButton(), selected)
        showViewIn(getTurnButton(), selected)
    }

    /** ----------------------------- Clip ------------------------------ */

    private var mCurrentClip: Clip? = null

    private var mCurrentClipConfig: Config? = null

    private var mCurrentAudioLayer : Layer? = null

    private var mCurrentAudioLayerConfig : Config? = null

    private var mCurrentAudioClip : Clip? = null

    private var mCurrentAudioClipConfig : Config? = null

    private var mCurrentPath: String = ""

    private var mNeedAudio = true
    override fun restoreClip(layer: ClipLayer) {
        val videoClip = layer.getClip(Video_CLIP_ID)
        val videoClipConfig = videoClip.config
        mCurrentPath = videoClipConfig.getString(VideoFileClip.CONFIG_PATH)
        mClipDuration = videoClipConfig.getIntNumber(VideoFileClip.CONFIG_TRIM_DURATION)

        var audioLayer = mEditor!!.audioComposition().allLayers[mCurrentLayerId] as ClipLayer
        val audioLayerConfig = audioLayer.config
        val audioClip = audioLayer.getClip(Video_CLIP_ID)
        val audioClipConfig = audioClip.config

        mCurrentClip = videoClip
        mCurrentClipConfig = videoClipConfig

        mCurrentAudioClip = audioClip
        mCurrentAudioClipConfig = audioClipConfig
        mCurrentAudioLayer = audioLayer
        mCurrentAudioLayerConfig = audioLayerConfig
    }

    override fun restoreLayer(id: Int) {
        super.restoreLayer(id)
        mStrokeWidth = ContextUtils.dip2px(context,2f)
        mStrokeColor = Color.WHITE
    }

    override fun createClip(layer: ClipLayer) {
        val videoClip = Clip(mEditor!!.context, VideoFileClip.TYPE_NAME)
        val videoClipConfig = Config()
        videoClipConfig.setString(VideoFileClip.CONFIG_PATH, mCurrentPath)

//        videoClipConfig.setNumber(VideoFileClip.CONFIG_TRIM_DURATION, mClipDuration)
        videoClip.setConfig(videoClipConfig)
        if (!videoClip.activate()) return
        TLog.e("video duration ${videoClip.streamInfo.duration}")
        if (!layer.addClip(Video_CLIP_ID, videoClip)) {

        }
        mCurrentClip = videoClip
        mCurrentClipConfig = videoClipConfig


        if (mNeedAudio){
            val audioClip = Clip(mEditor!!.context,AudioFileClip.TYPE_NAME)
            val audioConfig = Config()
            audioConfig.setString(AudioFileClip.CONFIG_PATH,mCurrentPath)
//            audioConfig.setNumber(AudioFileClip.CONFIG_TRIM_DURATION, mClipDuration)
            audioClip.setConfig(audioConfig)

            val audioLayer = ClipLayer(mEditor!!.context,false)
            val audioLayerConfig = Config()
            audioLayerConfig.setNumber(Layer.CONFIG_START_POS, mLayerStartPos)
            audioLayer.setConfig(audioLayerConfig)
            audioLayer.addClip(Video_CLIP_ID,audioClip)
            mEditor!!.audioComposition().addLayer(mCurrentLayerId,audioLayer)

            mCurrentAudioClip = audioClip
            mCurrentAudioClipConfig = audioConfig
            mCurrentAudioLayer = audioLayer
            mCurrentAudioLayerConfig = audioLayerConfig
        }
    }

    override fun setClipDuration(start: Long, end: Long) {

        mClipDuration = end - start
        mLayerStartPos = start
        mLayerEndPos = end
        mEditor!!.player?.lock()
        if (mClipDuration != 0L){
            mCurrentClipConfig?.setNumber(VideoFileClip.CONFIG_TRIM_DURATION, mClipDuration)
            mCurrentClip?.setConfig(mCurrentClipConfig)
        }
        mCurrentLayerConfig?.setNumber(Layer.CONFIG_START_POS, start)
        mCurrentLayer?.setConfig(mCurrentLayerConfig)
        if (mNeedAudio){
            mCurrentAudioClipConfig?.setNumber(AudioFileClip.CONFIG_TRIM_DURATION,mClipDuration)
            mCurrentAudioClip?.setConfig(mCurrentAudioClipConfig)
            mCurrentAudioLayerConfig?.setNumber(Layer.CONFIG_START_POS,start)
            mCurrentAudioLayer?.setConfig(mCurrentAudioLayerConfig)
        }
        if (!mEditor!!.build()) {
            TLog.e("Editor reBuild failed")
            throw Exception()
        }
        mEditor!!.player?.unlock()

    }

    fun setVideoPath(path: String){
        mCurrentPath = path

    }

    fun needAudio(need : Boolean){
        mNeedAudio = need
    }


}