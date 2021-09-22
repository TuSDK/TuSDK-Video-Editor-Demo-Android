/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2021/8/25$ 15:50$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.View
import android.view.ViewTreeObserver
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.tusdk.pulse.*
import com.tusdk.pulse.editor.*
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.VideoMatteEffect
import kotlinx.android.synthetic.main.include_title_layer.*
import kotlinx.android.synthetic.main.matte_activity.*
import kotlinx.android.synthetic.main.matte_activity.lsq_matte_play
import kotlinx.android.synthetic.main.title_item_layout.*
import kotlinx.android.synthetic.main.title_item_layout.lsq_back
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.*
import org.lasque.tusdkeditoreasydemo.base.views.matte.MatteItem
import org.lasque.tusdkeditoreasydemo.base.views.matte.MatteLayerViewDelegate
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.DateHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/8/25  15:50
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class MatteActivity : BaseActivity() {

    private var mEditor: VideoEditor = VideoEditor()

    private var mThreadPool: ExecutorService = Executors.newSingleThreadExecutor()

    private var mPlayer: VideoEditor.Player? = null

    private var mCurrentState = Player.State.kREADY

    private var mVideoItem: VideoItem? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mMaxDuration = 1L

    private var mVideoRect: Rect? = null

    private var mOutputWidth = 800

    private var mOutputHeight = 800

    private var isSeekBarTouch = false

    private var mCurrentDuration = 0L

    private var mPlayerContext: EditorPlayerContext = EditorPlayerContext(mEditor)

    private var mCurrentProducer: VideoEditor.Producer? = null

    private var mCurrentSavePath = ""

    private var isNeedSave = true

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 1) and (resultCode == 1)) {
            var albumBundle = data!!.getBundleExtra("select")
            var albumList = albumBundle?.getSerializable("select") as java.util.ArrayList<AlbumInfo>
            if (albumList != null) {
                mThreadPool.execute {
                    val item = albumList!![0]
                    mVideoItem = VideoItem.createVideoItem(
                        item.path,
                        mEditor,
                        false,
                        item.type == AlbumItemType.Video,
                        item.audioPath
                    )
                    initLayer()
                    initPlayer()
                }
            }
        } else {
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.matte_activity)

        initView()
    }

    private fun initView() {

        lsq_back.setOnClickListener { finish() }

        lsq_title.setText(FunctionType.Matte.mTitleId)

        lsq_matte_displayView.init(Engine.getInstance().mainGLContext)
//        lsq_matte_displayView.setBackgroundColor(Color.WHITE)

        val modelPath = intent.getStringExtra(DraftItem.Draft_Path_Key)

        if (TextUtils.isEmpty(modelPath)) {
            val res = mThreadPool.submit(Callable<Boolean> {
                val openConfig = VideoEditor.OpenConfig()
                openConfig.width = mOutputWidth
                openConfig.height = mOutputHeight
                val ret = mEditor.create(openConfig)
                if (!ret) {
                    TLog.e("Editor Create failed")
                }
                ret
            })

            res.get()

            openAlbum(1, false, false)
        } else {
            mThreadPool.execute {
                val editorModel = EditorModel(modelPath)
                if (!mEditor.create(editorModel)) {
                    TLog.e("Editor Create failed")
                }
                initPlayer()
                restoreLayer()
            }
        }

        lsq_output_video.setOnClickListener {
            playerPause()
            MaterialDialog(this).show {
                title(text = "保存选项")
                message(text = "请选择保存为视频或者草稿")

                positiveButton(text = "保存为视频") { dialog ->
                    dialog.dismiss()
                    saveVideo()
                }

                negativeButton(text = "保存为草稿") { dialog ->
                    dialog.dismiss()
                    saveDraft()
                }

                cancelOnTouchOutside(true)
            }
        }

        lsq_matte_layer.setEditor(mEditor)
        lsq_matte_layer.setThreadPool(mThreadPool)
        lsq_matte_layer.setPlayerContext(mPlayerContext)

        lsq_matte_play.setOnClickListener {
            if (mCurrentState != Player.State.kPLAYING) {
                playerPlay()
            } else {
                playerPause()
            }
        }

        lsq_matte_displayView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout() {
                if (mVideoLayer != null){
                    var property = mVideoLayer!!.getProperty(Layer.PROP_INTERACTION_INFO)
                    if (property != null){
                        val info =
                            Layer.InteractionInfo(property)
                        mVideoRect = lsq_matte_displayView.getInteractionRect(info.width, info.height)
                        lsq_matte_layer.resize(mVideoRect!!)

                        val listener = this
                        lsq_matte_displayView.post {
                            lsq_matte_displayView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                        }
                    }
                }
            }

        })

        val matteList = ArrayList<MatteItem>()
        matteList.add(MatteItem(R.drawable.lsq_matte_close, R.string.lsq_matte_none, VideoMatteEffect.MatteType.NONE))
        matteList.add(MatteItem(R.drawable.mask_linear_ic, R.string.lsq_matte_linear, VideoMatteEffect.MatteType.LINEAR))
        matteList.add(MatteItem(R.drawable.mask_mirror_ic, R.string.lsq_matte_mirror, VideoMatteEffect.MatteType.MIRROR))
        matteList.add(MatteItem(R.drawable.mask_radial_ic, R.string.lsq_matte_radial, VideoMatteEffect.MatteType.CIRCLE))
        matteList.add(MatteItem(R.drawable.mask_rect_ic, R.string.lsq_matte_rect, VideoMatteEffect.MatteType.RECT))
        matteList.add(MatteItem(R.drawable.mask_heart_ic, R.string.lsq_matte_heart, VideoMatteEffect.MatteType.LOVE))
        matteList.add(MatteItem(R.drawable.mask_star_ic, R.string.lsq_matte_star, VideoMatteEffect.MatteType.STAR))

        val matteAdapter = MatteAdapter(matteList, this)
        matteAdapter.setOnItemClickListener(object :
            OnItemClickListener<MatteItem, MatteAdapter.MatteViewHolder> {
            override fun onItemClick(
                pos: Int,
                holder: MatteAdapter.MatteViewHolder,
                item: MatteItem
            ) {
                matteAdapter.setCurrentPosition(pos)
                lsq_matte_layer.post {
                    val info =
                        Layer.InteractionInfo(mVideoLayer!!.getProperty(Layer.PROP_INTERACTION_INFO))
                    val infoSize = TuSdkSize.create(info.width,info.height)
                    val targetSize = when(item.type){
                        VideoMatteEffect.MatteType.STAR,
                        VideoMatteEffect.MatteType.LOVE,
                        VideoMatteEffect.MatteType.RECT,
                        VideoMatteEffect.MatteType.CIRCLE -> {
                            val info =
                                Layer.InteractionInfo(mVideoLayer!!.getProperty(Layer.PROP_INTERACTION_INFO))
                            TuSdkSize.create(info.width,info.height)
                        }

                        VideoMatteEffect.MatteType.LINEAR,
                        VideoMatteEffect.MatteType.MIRROR -> {
                            TuSdkSize.create(lsq_matte_displayView.width,lsq_matte_displayView.height)
                        }
                        else -> {
                            lsq_mask_alpha_title.visibility = View.INVISIBLE
                            lsq_matte_mask.visibility = View.INVISIBLE
                            lsq_matte_inv.visibility = View.INVISIBLE
                            mThreadPool.execute {
                                lsq_matte_layer.removeMatte(mVideoItem!!.mVideoClip)
                                mPlayerContext.refreshFrame()
                                lsq_matte_layer.visibility = View.INVISIBLE
                            }



                            return@post
                        }
                    }
                    mVideoRect = lsq_matte_displayView.getInteractionRect(targetSize.width, targetSize.height)
                    lsq_matte_layer.resize(mVideoRect!!)

                    lsq_mask_alpha_title.visibility = View.VISIBLE
                    lsq_matte_mask.visibility = View.VISIBLE
                    lsq_matte_inv.visibility = View.VISIBLE

                    mThreadPool.execute {
                        val rect = lsq_matte_displayView.getInteractionRect(infoSize.width,infoSize.height)
                        TLog.e("current rect ${rect}")
                        lsq_matte_layer.updateRect(rect)

                        lsq_matte_layer.changeMatte(item.type, mVideoItem!!.mVideoClip)
                        mPlayerContext.refreshFrame()
                    }
                    lsq_matte_layer.visibility = View.VISIBLE
                }



            }

        })

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        lsq_matte_list.layoutManager = linearLayoutManager
        lsq_matte_list.adapter = matteAdapter

        if (TextUtils.isEmpty(modelPath)){
            matteAdapter.setCurrentPosition(0)
            lsq_mask_alpha_title.visibility = View.INVISIBLE
            lsq_matte_mask.visibility = View.INVISIBLE
            lsq_matte_inv.visibility = View.INVISIBLE
        }


        lsq_matte_mask.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return

                val mixed = progress / 100.0 * 1.0
                mThreadPool.execute {
                    lsq_matte_layer.updateMixed(mixed)
                    mPlayerContext.refreshFrame()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        lsq_matte_inv.setOnClickListener {
            mThreadPool.execute {
                lsq_matte_layer.updateInv()
                mPlayerContext.refreshFrame()
            }
        }


    }

    private fun saveVideo() {
        isNeedSave = true
        val producer = mEditor.newProducer()

        val outputFilePath =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath}/editor_output${System.currentTimeMillis()}.mp4"
        TLog.e("output path $outputFilePath")

        mCurrentSavePath = outputFilePath

        val config = Producer.OutputConfig()
        config.watermark = BitmapHelper.getRawBitmap(this, R.raw.sample_watermark)
        config.watermarkPosition = 1
        producer.setOutputConfig(config)
        producer.setListener { state, ts ->
            if (state == Producer.State.kEND) {
                mThreadPool.execute {
                    producer.release()
                    mEditor.resetProducer()
                }

                sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(File(outputFilePath))
                    )
                )
                runOnUiThread {
                    setEnable(true)
                    lsq_editor_cut_load.visibility = View.GONE
                    lsq_editor_cut_load_parogress.setValue(0f)
                    if (isNeedSave)
                        Toast.makeText(applicationContext, "保存成功", Toast.LENGTH_SHORT).show()
                }
            } else if (state == Producer.State.kWRITING) {
                val currentProgress = (ts / producer.duration.toFloat()) * 100f
                runOnUiThread {
                    lsq_editor_cut_load.setVisibility(View.VISIBLE)
                    lsq_editor_cut_load_parogress.setValue(currentProgress)
                }
            }
        }

        if (!producer.init(outputFilePath)) {
            return
        }
        setEnable(false)
        if (!producer.start()) {
            TLog.e("[Error] EditorProducer Start failed")
        }
        mCurrentProducer = producer
    }

    private fun saveDraft() {
        mThreadPool.execute {
            val outputFileName = "editor_draft${System.currentTimeMillis()}"
            val outputFile = "${externalCacheDir!!.absolutePath}/draft"
            val file = File(outputFile)
            if (file.mkdirs()) {

            }
            val outputFilePath = "${outputFile}/${outputFileName}"
            TLog.e("draft path ${outputFilePath}")
            mEditor!!.model.save(outputFilePath)

            val currentModule = FunctionType.Mosaic

            val sp = getSharedPreferences("Tu-Draft-list", Context.MODE_PRIVATE)
            val cal = Calendar.getInstance()
            val draftItem = DraftItem(
                currentModule.ordinal,
                outputFilePath,
                DateHelper.format(cal, "yyyy/MM/dd HH:mm:ss"),
                outputFileName
            )
            val gson = Gson()
            val draftListJson = sp.getString(DraftItem.Draft_List_Key, "")
            var draftList = gson.fromJson(draftListJson, ArrayList::class.java)
            if (draftList != null) {
                (draftList as ArrayList<DraftItem>).add(draftItem)
            } else {
                draftList = ArrayList<DraftItem>()
                draftList.add(draftItem)
            }
            val draftString = gson.toJson(draftList)
            sp.edit().putString(DraftItem.Draft_List_Key, draftString).apply()

            runOnUiThread {
                MaterialDialog(this).show {
                    title(text = "草稿箱保存成功")
                    positiveButton(text = "确定") { dialog ->
                        dialog.dismiss()
                    }

                    cancelOnTouchOutside(true)
                }
            }
        }
    }

    private fun restoreLayer() {
        val videoLayer = mEditor.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor.audioComposition().allLayers[11] as ClipLayer

        val videoId = videoLayer.allClips.keys.first()
        val videoClip = videoLayer.allClips.values.first()
        val audioClip = audioLayer.allClips.values.first()

        val videoPath = videoClip.config.getString(VideoFileClip.CONFIG_PATH)

        mVideoItem = VideoItem(videoPath, videoId.toLong(), videoClip, audioClip)

        val duration = videoClip.streamInfo.duration

        val info =
            Layer.InteractionInfo(videoLayer!!.getProperty(Layer.PROP_INTERACTION_INFO))
        val infoSize = TuSdkSize.create(info.width,info.height)

        val rect = lsq_matte_displayView.getInteractionRect(infoSize.width,infoSize.height)
        lsq_matte_layer.updateRect(rect)

        lsq_matte_layer.restoreMatte(videoClip)

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
        mMaxDuration = duration
    }

    private fun initPlayer() {
        if (!mEditor.build()) {

        }

        mPlayer = mEditor.newPlayer()
        mPlayer?.setListener { state, ts ->
            mCurrentState = state
            mPlayerContext.state = mCurrentState
            if (state == Player.State.kDO_PLAY || state == Player.State.kDO_PAUSE) {
                runOnUiThread {
                    val durationMS = mPlayer!!.duration
                    mMaxDuration = durationMS
                    seekBar.max = durationMS.toInt()
                }
            } else if (state == Player.State.kEOS) {
                runOnUiThread {
                    lsq_matte_play.setImageResource(R.mipmap.edit_ic_play)
                    mThreadPool.execute { mPlayer!!.previewFrame(0) }
                }
            } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW) {

                mPlayerContext.currentFrame = ts

                val currentVideoHour = ts / 3600000
                val currentVideoMinute = (ts % 3600000) / 60000

                val currentVideoSecond = (ts % 60000 / 1000)

                val durationMS = mPlayer!!.duration
                val durationVideoHour = durationMS / 3600000

                val durationVideoMinute = (durationMS % 3600000) / 60000

                val durationVideoSecond = (durationMS % 3600000 / 1000)

                mCurrentDuration = ts
                runOnUiThread {
                    if (!isSeekBarTouch) {
                        seekBar.progress = ts.toInt()
                    }
                    lsq_player_duration.text =
                        "${currentVideoHour}:$currentVideoMinute:$currentVideoSecond/$durationVideoHour:$durationVideoMinute:$durationVideoSecond"
                }
            }
        }

        if (!mPlayer!!.open()) {
            TLog.e("Editor Player Open failed")
        }

        runOnUiThread {
            lsq_matte_displayView.attachPlayer(mPlayer)
            val dur = mPlayer!!.duration.toInt()
            seekBar.max = dur
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                var current = 0

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (!fromUser) return
                    current = progress
                    mThreadPool.execute {
                        mPlayer?.previewFrame(progress.toLong())
                    }

                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isSeekBarTouch = true
                    playerPause()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isSeekBarTouch = false
                }

            })
        }

        mPlayer!!.previewFrame(0)

    }

    private fun playerPlay() {
        mThreadPool.execute {
            mPlayer?.play()
            runOnUiThread {
                lsq_matte_play.setImageResource(R.mipmap.edit_ic_pause)
            }
        }
    }

    private fun playerPause() {
        mThreadPool.execute {
            mPlayer?.pause()
            runOnUiThread {
                lsq_matte_play.setImageResource(R.mipmap.edit_ic_play)
            }
        }
    }

    private fun initLayer() {
        val duration = mVideoItem!!.mVideoClip.streamInfo.duration

        val videoLayer = ClipLayer(mEditor!!.context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        videoLayer.setConfig(videoLayerConfig)
        if (!videoLayer.addClip(mVideoItem!!.mId.toInt(), mVideoItem!!.mVideoClip)) {
            return
        }

        val audioLayer = ClipLayer(mEditor.context, false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        audioLayer.setConfig(audioLayerConfig)

        if (!audioLayer.addClip(mVideoItem!!.mId.toInt(), mVideoItem!!.mAudioClip)) {
            return
        }

        mEditor.videoComposition().addLayer(11, videoLayer)
        mEditor.audioComposition().addLayer(11, audioLayer)

//        val matteEffect = Effect(mEditor.context,VideoMatteEffect.TYPE_NAME)
//        val matteConfig = Config()
//        matteConfig.setString(VideoMatteEffect.CONFIG_MATTE_TYPE,VideoMatteEffect.MatteType.LINEAR.type)
//        matteEffect.setConfig(matteConfig)

//        mVideoItem!!.videoClip.effects().add(401,matteEffect)

//        val property = matteEffect.getProperty(VideoMatteEffect.PROP_INTERACTION_INFO)
//        TLog.e("matte info ${property.toString()}")

//        val infoP = VideoMatteEffect.InteractionInfo(property)


        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
        mMaxDuration = duration
    }

    override fun onPause() {
        super.onPause()
        playerPause()
    }


}