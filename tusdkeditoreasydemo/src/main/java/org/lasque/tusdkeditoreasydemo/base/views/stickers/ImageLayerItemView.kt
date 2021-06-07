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
import com.tusdk.pulse.editor.clips.ImageClip
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.view.TuSdkImageView
import org.lasque.tusdkeditoreasydemo.base.views.TuSdkImageButton
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
public class ImageLayerItemView : LayerItemViewBase {

    companion object {
        const val IMAGE_CLIP_ID = 400

        fun getLayoutId(): Int {
            return TuSdkContext.getLayoutResId("tusdk_impl_component_widget_sticker_image_item_view")
        }
    }

    enum class ImageType {
        Image, Path
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

    private var imageType: ImageType = ImageType.Image

    private var mCurrentClip: Clip? = null

    private var mCurrentClipConfig: Config? = null

    private var mCurrentImage: Bitmap? = null

    private var mCurrentPath: String = ""
    override fun restoreClip(layer: ClipLayer) {
        val imageClip = layer.allClips[IMAGE_CLIP_ID]
        val imageClipConfig = imageClip?.config
        mClipDuration = imageClipConfig?.getIntNumber(ImageClip.CONFIG_DURATION)!!
        mCurrentClip = imageClip
        mCurrentClipConfig = imageClipConfig

        mClipStartPos = 0
        mClipEndPos = mClipDuration
    }

    override fun restoreLayer(id: Int) {
        super.restoreLayer(id)
        mStrokeWidth = ContextUtils.dip2px(context,2f)
        mStrokeColor = Color.WHITE
    }

    override fun createClip(layer: ClipLayer) {
        val imageClip = Clip(mEditor!!.context, ImageClip.TYPE_NAME)
        val imageClipConfig = Config()
        when (imageType) {
            ImageType.Image -> {
                val blob = Blob.wrap(mCurrentImage)
                imageClipConfig.setBuffer(ImageClip.CONFIG_BLOB, blob.data())
            }
            ImageType.Path -> {
                imageClipConfig.setString(ImageClip.CONFIG_PATH, mCurrentPath)
            }
        }
        imageClipConfig.setNumber(ImageClip.CONFIG_DURATION, mClipDuration)
        imageClip.setConfig(imageClipConfig)
        if (!imageClip.activate()) return
        if (!layer.addClip(IMAGE_CLIP_ID, imageClip)) {

        }
        mCurrentClip = imageClip
        mCurrentClipConfig = imageClipConfig

        mClipStartPos = 0
        mClipEndPos = mClipDuration
        mClipMaxDuration = mClipDuration
    }

    override fun setClipDuration(start: Long, end: Long) {
//        val end = max(1000,end)
        mClipDuration = end - start

        mClipStartPos = start
        mClipEndPos = end

        mLayerEndPos = mLayerStartPos + mClipDuration

        mEditor!!.player?.lock()
        if (mClipDuration != 0L) {
            mCurrentClipConfig?.setNumber(ImageClip.CONFIG_DURATION, mClipDuration)
            mCurrentClip?.setConfig(mCurrentClipConfig)
        }

        if (!mEditor!!.build()) {
            TLog.e("Editor reBuild failed")
            throw Exception()
        }
        mEditor!!.player?.unlock()

    }


    fun setImage(image: Bitmap) {
        imageType = ImageType.Image
        mCurrentImage = image
    }

    fun setImage(path: String) {
        imageType = ImageType.Path
        mCurrentPath = path
    }


}