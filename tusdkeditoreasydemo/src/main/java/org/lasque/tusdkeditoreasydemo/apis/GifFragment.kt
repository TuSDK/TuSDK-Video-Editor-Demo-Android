/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo.apis$
 *  @author  H.ys
 *  @Date    2022/3/25$ 10:51$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo.apis

import android.content.Context
import android.os.Bundle
import android.view.View
import com.tusdk.pulse.Config
import com.tusdk.pulse.editor.Clip
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.clips.GifClip
import com.tusdk.pulse.editor.clips.SilenceClip
import com.tusdk.pulse.utils.AssetsMapper
import org.lasque.tusdkeditoreasydemo.FunctionType
import org.lasque.tusdkeditoreasydemo.R
import org.lasque.tusdkeditoreasydemo.base.BaseFragment

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.apis
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2022/3/25  10:51
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class GifFragment : BaseFragment(FunctionType.GIF){

    private var mGIFPath = ""

    private var mGIFClip : Clip? = null

    private var mAudioClip : Clip? = null

    private var mVideoLayer : ClipLayer? = null

    private var mAudioLayer : ClipLayer? = null

    private var mGIFConfig : Config? = null

    private var isRestore = false;

    override fun getLayoutId(): Int {
        return R.layout.gif_fragment
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mThreadPool?.execute {
            initLayer()
        }
    }

    private fun initLayer(){

        val sp = requireContext().getSharedPreferences("gif-path",Context.MODE_PRIVATE)

        mGIFPath = sp.getString("gif","")!!

        if (mGIFPath.isEmpty()){
            val mapper = AssetsMapper(requireContext());

            mGIFPath = mapper.mapAsset("test_gif.gif")
            sp.edit().putString("gif",mGIFPath).apply()
        }


        var videoLayer = ClipLayer(mEditor!!.context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        videoLayer.config = videoLayerConfig


        var audioLayer = ClipLayer(mEditor!!.context,false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
        audioLayer.config = audioLayerConfig

        val gifClip = Clip(mEditor!!.context,GifClip.TYPE_NAME)
        val gifConfig = Config()
        gifConfig.setNumber(GifClip.CONFIG_DURATION,3000)
        gifConfig.setString(GifClip.CONFIG_PATH,mGIFPath)
        gifClip.config = gifConfig

        val audioClip = Clip(mEditor!!.context,SilenceClip.TYPE_NAME)
        val audioConfig = Config()
        audioConfig.setNumber(SilenceClip.CONFIG_DURATION,3000)
        audioClip.config = audioConfig

        videoLayer.addClip(100,gifClip)

        audioLayer.addClip(100,audioClip)

        mEditor!!.videoComposition().addLayer(11,videoLayer)
        mEditor!!.audioComposition().addLayer(11,audioLayer)

        mGIFClip = gifClip
        mAudioClip = audioClip
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer



    }


}