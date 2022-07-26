package org.lasque.tusdkeditoreasydemo

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.tusdk.pulse.Engine
import com.tusdk.pulse.PermissionManager
import com.tusdk.pulse.editor.TuVideoClipSDK
import com.tusdk.pulse.utils.AssetsMapper
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity
import org.lasque.tusdkeditoreasydemo.base.OnItemClickListener
import org.lasque.tusdkeditoreasydemo.utils.Constants
import org.lasque.tusdkeditoreasydemo.utils.PermissionUtils
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.listener.TuSdkOrientationEventListener
import org.lasque.tusdkpulse.core.utils.ContextUtils
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.ThreadHelper
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE
    )

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
        FunctionType.AudioFade,
        FunctionType.Transform,
        FunctionType.CanvasBackgroundType,
        FunctionType.FilterEffect,
        FunctionType.MVEffect,
        FunctionType.AudioPitch,
        FunctionType.TransitionsEffect,
        FunctionType.SceneEffect,
        FunctionType.ParticleEffect,
        FunctionType.Text,
        FunctionType.Bubble,
        FunctionType.Graffiti,
        FunctionType.Freeze,
        FunctionType.Mosaic,
        FunctionType.Matte,
        FunctionType.DraftList,
        FunctionType.GIF
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionUtils.requestRequiredPermissions(this, PERMISSIONS_STORAGE)

        val versionCode = TuVideoClipSDK.SDK_VERSION

        lsq_copyright_info.setText(
            "TuSDK Video Editor SDK ${versionCode}-${TuVideoClipSDK.BUILD_VERSION} \n" +
                    " © 2020 TUTUCLOUD.COM"
        )

        val sp = getSharedPreferences("TU-TTF", Context.MODE_PRIVATE)
        if (!sp.contains(Constants.TTF_KEY)) {
            val assetsMapper = AssetsMapper(this)
            val path = assetsMapper.mapAsset("Coiny-Regular.ttf")
            sp.edit().putString(Constants.TTF_KEY, path).apply()
        }

        val ttfPath = sp.getString(Constants.TTF_KEY, "")
        if (!TextUtils.isEmpty(ttfPath)) {
            val ttfFile = File(ttfPath)
            if (!ttfFile.exists()) {
                val assetsMapper = AssetsMapper(this)
                val path = assetsMapper.mapAsset("Coiny-Regular.ttf")
                sp.edit().putString(Constants.TTF_KEY, path).apply()
            }
        }


        val adapter = ApiAdapter(FunctionList, this)
        adapter.setOnItemClickListener(object :
            OnItemClickListener<FunctionType, ApiAdapter.ApiViewHolder> {
            override fun onItemClick(
                pos: Int,
                holder: ApiAdapter.ApiViewHolder,
                item: FunctionType
            ) {
                when (item) {
                    FunctionType.ParticleEffect -> {
                        startActivity<ParticleActivity>()
                    }
                    FunctionType.PictureInPicture -> {
                        startActivity<ImageStickerActivity>("FunctionType" to FunctionType.PictureInPicture)
                    }
                    FunctionType.Text -> {
                        startActivity<TextStickerActivity>()
                    }
                    FunctionType.DraftList -> {
                        startActivity<DraftActivity>()
                    }
                    FunctionType.Bubble -> {
                        startActivity<BubbleTextActivity>()
                    }
                    FunctionType.Graffiti -> {
                        startActivity<GraffitiActivity>()
                    }
                    FunctionType.Mosaic -> {
                        startActivity<MosaicActivity>()
                    }
                    FunctionType.VoiceToText->{
                        startActivity<VoiceToTextActivity>()
                    }
                    FunctionType.VoiceVC->{
                        startActivity<VoiceAPIEffectActivity>()
                    }
                    FunctionType.Matte->{
                        startActivity<MatteActivity>()
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
//        mEngine.setLogLevel(-1)

        ThreadHelper.runThread {
            val sp = TuSdkContext.context().getSharedPreferences("TU-TTF", Context.MODE_PRIVATE)
            val assetsMapper = AssetsMapper(TuSdkContext.context())
            if (!sp.contains(Constants.BUBBLE_TTF_KEY)) {
                val gson = Gson()
                val fontPathsList = Arrays.asList(
                    assetsMapper.mapAsset("AliHYAiHei.ttf"),
                    assetsMapper.mapAsset("NotoColorEmoji.ttf"),
                    assetsMapper.mapAsset("SOURCEHANSANSCN-LIGHT.OTF"),
                    assetsMapper.mapAsset("SOURCEHANSANSCN-REGULAR.OTF"),
                    assetsMapper.mapAsset("站酷快乐体2016修订版_0.ttf")
                )
                val path = TuSdkContext.context().externalCacheDirs[0].absolutePath + "/assets"

                val stringBuilder = StringBuilder()

                for (s in fontPathsList){
                    stringBuilder.append(s).append(",")
                }

                val fontJson = stringBuilder.toString()
                sp.edit().putString(Constants.TEXT_TTF_LIST,fontJson).apply()

                sp.edit().putString(Constants.BUBBLE_TTF_KEY, path).apply()
            }
            if (!sp.contains(Constants.BUBBLE_5)) {
                val path = assetsMapper.mapAsset("bubbles/lsq_bubble_5.bt")
                sp.edit().putString(Constants.BUBBLE_5, path).apply()
            } else {
                val path = sp.getString(Constants.BUBBLE_5, "")
                val file = File(path)
                if (TextUtils.isEmpty(path) || !file.exists()) {
                    val path = assetsMapper.mapAsset("bubbles/lsq_bubble_5.bt")
                    sp.edit().putString(Constants.BUBBLE_5, path).apply()
                }
            }
            if (!sp.contains(Constants.BUBBLE_6)) {
                val path = assetsMapper.mapAsset("bubbles/lsq_bubble_6.bt")
                sp.edit().putString(Constants.BUBBLE_6, path).apply()
            } else {
                val path = sp.getString(Constants.BUBBLE_6, "")
                val file = File(path)
                if (TextUtils.isEmpty(path) || !file.exists()) {
                    val path = assetsMapper.mapAsset("bubbles/lsq_bubble_6.bt")
                    sp.edit().putString(Constants.BUBBLE_6, path).apply()
                }
            }
            if (!sp.contains(Constants.BUBBLE_7)) {
                val path = assetsMapper.mapAsset("bubbles/lsq_bubble_7.bt")
                sp.edit().putString(Constants.BUBBLE_7, path).apply()
            } else {
                val path = sp.getString(Constants.BUBBLE_7, "")
                val file = File(path)
                if (TextUtils.isEmpty(path) || !file.exists()) {
                    val path = assetsMapper.mapAsset("bubbles/lsq_bubble_7.bt")
                    sp.edit().putString(Constants.BUBBLE_7, path).apply()
                }
            }
        }

    }

    /**
     * 授予权限的结果，在对话结束后调用
     *
     * @param permissionGranted
     * true or false, 用户是否授予相应权限
     */
    protected var mGrantedResultDelgate: PermissionUtils.GrantedResultDelgate =
        object : PermissionUtils.GrantedResultDelgate {
            override fun onPermissionGrantedResult(permissionGranted: Boolean) {
                if (permissionGranted) {
                    ThreadHelper.runThread {
                        val sp = TuSdkContext.context()
                            .getSharedPreferences("TU-TTF", Context.MODE_PRIVATE)
                        val assetsMapper = AssetsMapper(TuSdkContext.context())
                        if (!sp.contains(Constants.BUBBLE_TTF_KEY)) {
                            assetsMapper.mapAsset("AliHYAiHei.ttf")
                            assetsMapper.mapAsset("NotoColorEmoji.ttf")
                            assetsMapper.mapAsset("SOURCEHANSANSCN-LIGHT.OTF")
                            assetsMapper.mapAsset("SOURCEHANSANSCN-REGULAR.OTF")
                            assetsMapper.mapAsset("站酷快乐体2016修订版_0.ttf")
                            val path =
                                TuSdkContext.context().externalCacheDirs[0].absolutePath + "/assets"
                            sp.edit().putString(Constants.BUBBLE_TTF_KEY, path).apply()
                        }
                        if (!sp.contains(Constants.BUBBLE_5)) {
                            val path = assetsMapper.mapAsset("bubbles/lsq_bubble_5.bt")
                            sp.edit().putString(Constants.BUBBLE_5, path).apply()
                        }
                        if (!sp.contains(Constants.BUBBLE_6)) {
                            val path = assetsMapper.mapAsset("bubbles/lsq_bubble_6.bt")
                            sp.edit().putString(Constants.BUBBLE_6, path).apply()
                        }
                        if (!sp.contains(Constants.BUBBLE_7)) {
                            val path = assetsMapper.mapAsset("bubbles/lsq_bubble_7.bt")
                            sp.edit().putString(Constants.BUBBLE_7, path).apply()
                        }
                    }
                } else {

                }
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtils.handleRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this,
            mGrantedResultDelgate
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Engine.getInstance().release()
    }
}