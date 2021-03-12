package org.lasque.tusdkeditoreasydemo

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.tusdk.pulse.Engine
import com.tusdk.pulse.editor.TuVideoClipSDK
import com.tusdk.pulse.utils.AssetsMapper
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity
import org.lasque.tusdkpulse.core.utils.AssetsHelper
import org.lasque.tusdkeditoreasydemo.base.OnItemClickListener
import org.lasque.tusdkeditoreasydemo.utils.Constants
import org.lasque.tusdkeditoreasydemo.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE)

    private val FunctionList = mutableListOf<FunctionType>(
            FunctionType.MoiveCut,
            FunctionType.VideoSegmentation,
            FunctionType.VideoStitching,
            FunctionType.VideoImageStitching,
            FunctionType.ImageStitching,
            FunctionType.VideoAudioMix,
            FunctionType.ReverseEffect,
            FunctionType.SlowEffect,
            FunctionType.RepeatEffect,
            FunctionType.VideoRatio,
            FunctionType.Cover,
            FunctionType.Speed,
            FunctionType.PictureInPicture,
            FunctionType.Crop,
            FunctionType.ColorAdjust,
            FunctionType.AudioMix,
            FunctionType.Transform,
            FunctionType.CanvasBackgroundType,
            FunctionType.FilterEffect,
            FunctionType.MVEffect,
            FunctionType.AudioPitch,
            FunctionType.TransitionsEffect,
            FunctionType.SceneEffect,
            FunctionType.ParticleEffect,
            FunctionType.Text,
            FunctionType.DraftList
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionUtils.requestRequiredPermissions(this, PERMISSIONS_STORAGE)

        val versionCode = TuVideoClipSDK.SDK_VERSION

        lsq_copyright_info.setText("TuSDK Video Editor SDK ${versionCode}-${TuVideoClipSDK.BUILD_VERSION} \n" +
                " © 2020 TUTUCLOUD.COM")

        val sp = getSharedPreferences("TU-TTF",Context.MODE_PRIVATE)
        if (!sp.contains(Constants.TTF_KEY)){
            val assetsMapper = AssetsMapper(this)
            val path = assetsMapper.mapAsset("SourceHanSansSC-Normal.ttf")
            sp.edit().putString(Constants.TTF_KEY,path).commit()
        }


        val adapter = ApiAdapter(FunctionList, this)
        adapter.setOnItemClickListener(object : OnItemClickListener<FunctionType, ApiAdapter.ApiViewHolder> {
            override fun onItemClick(pos: Int, holder: ApiAdapter.ApiViewHolder, item: FunctionType) {
                when (item) {
                    FunctionType.ParticleEffect -> {
                        startActivity<ParticleActivity>()
                    }
                    FunctionType.PictureInPicture->{
                        startActivity<ImageStickerActivity>("FunctionType" to FunctionType.PictureInPicture)
                    }
                    FunctionType.Text -> {
                        startActivity<TextStickerActivity>()
                    }
                    FunctionType.DraftList->{
                        startActivity<DraftActivity>()
                    }
                    else -> {
                        startActivity<ApiActivity>("function" to item)
                    }
                }
            }
        })
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        lsq_api_list.layoutManager = layoutManager
        lsq_api_list.adapter = adapter

        val mEngine = Engine.getInstance()
        mEngine.init(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        Engine.getInstance().release()
    }
}