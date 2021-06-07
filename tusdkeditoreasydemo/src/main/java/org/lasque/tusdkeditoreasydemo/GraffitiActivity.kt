/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2021/4/19$ 15:48$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import cn.bar.DoubleHeadedDragonBar
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.tusdk.pulse.Config
import com.tusdk.pulse.Engine
import com.tusdk.pulse.Player
import com.tusdk.pulse.Producer
import com.tusdk.pulse.editor.*
import com.tusdk.pulse.editor.clips.GraffitiClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import kotlinx.android.synthetic.main.graffiti_activity.*
import kotlinx.android.synthetic.main.graffiti_activity.lsq_editor_cut_load
import kotlinx.android.synthetic.main.graffiti_activity.lsq_editor_cut_load_parogress
import kotlinx.android.synthetic.main.graffiti_activity.lsq_player_duration
import kotlinx.android.synthetic.main.graffiti_activity.seekBar
import kotlinx.android.synthetic.main.include_title_layer.*
import kotlinx.android.synthetic.main.particle_activity.*
import kotlinx.android.synthetic.main.repeat_fragment.view.*
import org.jetbrains.anko.toast
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseActivity
import org.lasque.tusdkeditoreasydemo.base.DraftItem
import org.lasque.tusdkeditoreasydemo.base.GraffitiItem
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkeditoreasydemo.base.views.ColorView
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.DateHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/4/19  15:48
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class GraffitiActivity : BaseActivity(){

    private var mEditor : VideoEditor = VideoEditor()

    private var mThreadPool : ExecutorService = Executors.newSingleThreadExecutor()

    private var mPlayer : VideoEditor.Player? = null

    private var mCurrentState = Player.State.kREADY

    private var mVideoItem : VideoItem? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mMaxDuration = 1L

    private var mVideoRect : Rect? = null

    private var mParticleSize = 0f

    private var mOutputWidth = 800

    private var mOutputHeight = 800

    private var isSeekBarTouch = false

    private var mCurrentStartPos = 0L

    private var mCurrentEndPos = 0L

    private var mCurrentDuration = 0L


    /** ---------------------- Graffiti --------------------------------- * */

    private var mGraffitiId = 400

    private var mCurrentGraffitiItem: GraffitiItem? = null

    private var pathIndex = 1

    private var currentPathIndex = -1


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 1) and (resultCode == 1)) {
            var albumBundle = data!!.getBundleExtra("select")
            var albumList = albumBundle?.getSerializable("select") as ArrayList<AlbumInfo>
            if (albumList != null) {
                mThreadPool.execute {
                    val item = albumList!![0]
                    mVideoItem = VideoItem.createVideoItem(item.path, mEditor, true, item.type == AlbumItemType.Video,item.audioPath)
                    initLayer()
                    initPlayer()
                }
            }
        } else {
            finish()
        }
    }

    private var currentPath : GraffitiClip.GraffitiPath? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.graffiti_activity)

        lsq_back.setOnClickListener {
            finish()
        }

        lsq_title.setText(FunctionType.Graffiti.mTitleId)

        lsq_graffiti_displayView.init(Engine.getInstance().mainGLContext)

        val modelPath = intent.getStringExtra(DraftItem.Draft_Path_Key)

        if (TextUtils.isEmpty(modelPath)){
            val res = mThreadPool.submit(Callable<Boolean> {
                val openConfig = VideoEditor.OpenConfig()
                openConfig.width = mOutputWidth
                openConfig.height = mOutputHeight
                val ret = mEditor.create(openConfig)
                if (!ret){
                    TLog.e("Editor Create failed")
                }
                ret
            })

            res.get()

            openAlbum(1,false,false)
        } else {
            mThreadPool.execute {
                val editorModel = EditorModel(modelPath)
                if (!mEditor.create(editorModel)){
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

        lsq_graffiti_play.setOnClickListener {
            if (mCurrentState != Player.State.kPLAYING){
                playerPlay()
            } else {
                playerPause()
            }
        }

        lsq_graffiti_layer.setOnTouchListener { v, event ->
            if (mCurrentGraffitiItem == null) return@setOnTouchListener false



            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    var res : Future<GraffitiClip.GraffitiPath> = mThreadPool.submit(Callable<GraffitiClip.GraffitiPath> {

                        val color = lsq_graffiti_color_bar.selectColor
                        val pathWidth = lsq_graffiti_size_bar.progress

                        val currentPath = GraffitiClip.GraffitiPath()
                        val firstPoint = convertedPoint(event.x,event.y)
                        currentPath.paths.add(firstPoint)
                        currentPath.color = lsq_graffiti_color_bar.selectColor
                        currentPath.width = lsq_graffiti_size_bar.progress
                        mCurrentGraffitiItem!!.graffitiProperty.holder.graffitiPaths.add(currentPath)
                        currentPathIndex = pathIndex++
                        currentPath.index = currentPathIndex
                        mCurrentGraffitiItem!!.graffitiClip.setProperty(GraffitiClip.PROP_APPEND_PARAM,mCurrentGraffitiItem!!.graffitiProperty.makeAppendProperty(firstPoint.x,firstPoint.y,currentPath.color,currentPath.width,currentPathIndex))

                        mPlayer?.previewFrame(mCurrentDuration)
                        currentPath
                    } )

                    currentPath = res.get()

                }

                MotionEvent.ACTION_MOVE -> {

                    var res : Future<Boolean> = mThreadPool.submit(Callable<Boolean> {
                        if (currentPath == null) return@Callable false

                        val point = convertedPoint(event.x,event.y)
                        currentPath!!.paths.add(point)
                        mCurrentGraffitiItem!!.graffitiClip.setProperty(GraffitiClip.PROP_EXTEND_PARAM,mCurrentGraffitiItem!!.graffitiProperty.makeExtendProperty(point.x,point.y,currentPathIndex))
                        mPlayer?.previewFrame(mCurrentDuration)
                    })

                    res.get()
                }

                MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_UP -> {
                    var res : Future<Boolean> = mThreadPool.submit(Callable<Boolean> {
                        if (currentPath == null) return@Callable false
                        val point = convertedPoint(event.x,event.y)
                        currentPath!!.paths.add(point)
//                        mCurrentGraffitiItem!!.graffitiClip.setProperty(GraffitiClip.PROP_EXTEND_PARAM,mCurrentGraffitiItem!!.graffitiProperty.makeExtendProperty(point.x,point.y,currentPathIndex))

                        mCurrentGraffitiItem!!.graffitiClip.setProperty(GraffitiClip.PROP_DELETE_PARAM,mCurrentGraffitiItem!!.graffitiProperty.makeDeleteProperty(currentPathIndex))

                        mCurrentGraffitiItem!!.graffitiClip.setProperty(GraffitiClip.PROP_PARAM,mCurrentGraffitiItem!!.graffitiProperty.makeProperty())
                        mPlayer?.previewFrame(mCurrentDuration)
                    })

                    res.get()
                    currentPath = null
                }
            }

            return@setOnTouchListener true
        }
        
        lsq_graffiti_color_bar.setOnColorChangeListener(object : ColorView.OnColorChangeListener {
            override fun changeColor(colorId: Int) {

            }

            override fun changePosition(percent: Float) {

            }

        })

        lsq_graffiti_color_bar.findColorInt(Color.BLUE)

        lsq_graffiti_size_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        lsq_start_bar.setCallBack(object : DoubleHeadedDragonBar.DhdBarCallBack() {

            override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                super.onEndTouch(minPercentage, maxPercentage)

                val startTime = (mMaxDuration * minPercentage / 100).toLong()
                val endTime = (mMaxDuration * maxPercentage / 100).toLong()

                if (startTime == endTime){
                    toast("涂鸦起止位置不能相同")
                    lsq_start_bar.minValue = (mCurrentStartPos / mMaxDuration.toFloat() * 100.0).toInt()
                    lsq_start_bar.maxValue = (mCurrentEndPos / mMaxDuration.toFloat() * 100.0).toInt()
                    lsq_start_bar.invalidate()
                    return
                }

                mCurrentStartPos = startTime
                mCurrentEndPos = endTime

                mThreadPool.execute {
                    mEditor.player.previewFrame(mCurrentStartPos)

                }
                if (mCurrentGraffitiItem == null) return

                playerPause()


                mThreadPool.execute {
                    mPlayer?.lock()
                    val clipConfig = mCurrentGraffitiItem!!.graffitiClip.config
                    clipConfig.setNumber(GraffitiClip.CONFIG_DURATION,mCurrentEndPos - mCurrentStartPos)
                    mCurrentGraffitiItem!!.graffitiClip.setConfig(clipConfig)

                    val layerConfig = mCurrentGraffitiItem!!.layer.config
                    layerConfig.setNumber(Layer.CONFIG_START_POS,mCurrentStartPos)
                    mCurrentGraffitiItem!!.layer.setConfig(layerConfig)

                    if (!mEditor!!.build()) {
                        TLog.e("Editor reBuild failed")
                        throw Exception()
                    }

                    mPlayer?.unlock()
                    mEditor.player.previewFrame(mCurrentStartPos)
                }


            }

        })

        lsq_graffiti_displayView.viewTreeObserver.addOnGlobalLayoutListener {
            mVideoRect = lsq_graffiti_displayView.getInteractionRect(mOutputWidth,mOutputHeight)
        }

        lsq_add_graffiti.setOnClickListener {
            playerPause()

            mCurrentStartPos = mCurrentDuration
            mCurrentEndPos = mMaxDuration

            lsq_start_bar.post {
                lsq_start_bar.minValue = ((mCurrentStartPos / mMaxDuration.toFloat()) * 100).toInt()
                lsq_start_bar.maxValue = ((mCurrentEndPos / mMaxDuration.toFloat()) * 100).toInt()
                lsq_start_bar.invalidate()
            }

            createGraffiti()

            lsq_stop_graffiti.visibility = View.VISIBLE
            lsq_add_graffiti.visibility = View.GONE

            lsq_revoke_graffiti.isEnabled = true
        }

        lsq_stop_graffiti.setOnClickListener {
            stopGraffitiDraw()
            lsq_stop_graffiti.visibility = View.GONE
            lsq_add_graffiti.visibility = View.VISIBLE

            lsq_revoke_graffiti.isEnabled = true
        }

        lsq_revoke_graffiti.setOnClickListener {
            if (mCurrentGraffitiItem != null){
                val paths = mCurrentGraffitiItem!!.graffitiProperty.holder.graffitiPaths

                if (paths.size == 0) return@setOnClickListener

                val lastPath = paths.last()

                var res : Future<Boolean> = mThreadPool.submit(Callable<Boolean>{
                    mCurrentGraffitiItem!!.graffitiClip.setProperty(GraffitiClip.PROP_DELETE_PARAM,mCurrentGraffitiItem!!.graffitiProperty.makeDeleteProperty(lastPath.index))
                    paths.remove(lastPath)
                    mPlayer?.previewFrame(mCurrentDuration)
                })

                res.get()

            } else {
                val layers = mEditor.videoComposition().allLayers.keys.toMutableList()

                Collections.sort(layers)

                layers.remove(11)

                if (layers.size == 0) return@setOnClickListener

                var res : Future<Boolean> = mThreadPool.submit(Callable<Boolean>{
                    mPlayer!!.lock()
                    val index = layers.last()
                    mEditor.videoComposition().deleteLayer(index)
                    layers.remove(index)
                    mEditor!!.build()
                    mPlayer!!.unlock()

                    runOnUiThread {
                        if (layers.size == 0) lsq_revoke_graffiti.isEnabled = false
                        else lsq_revoke_graffiti.isEnabled = true
                    }
                    mPlayer?.previewFrame(mCurrentDuration)
                })

                res.get()

            }
        }

    }

    private var mCurrentProducer : VideoEditor.Producer? = null

    private var mCurrentSavePath = ""

    private var isNeedSave = true

    private fun saveVideo(){
        isNeedSave = true
        val producer = mEditor.newProducer()

        val outputFilePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath}/editor_output${System.currentTimeMillis()}.mp4"
        TLog.e("output path $outputFilePath")

        mCurrentSavePath = outputFilePath

        val config = Producer.OutputConfig()
        config.watermark = BitmapHelper.getRawBitmap(this, R.raw.sample_watermark)
        config.watermarkPosition = 1
        producer.setOutputConfig(config)
        producer.setListener { state, ts ->
            if (state == Producer.State.kEND){
                mThreadPool.execute {
                    producer.release()
                    mEditor.resetProducer()
                }

                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(outputFilePath))))
                runOnUiThread {
                    setEnable(true)
                    lsq_editor_cut_load.visibility = View.GONE
                    lsq_editor_cut_load_parogress.setValue(0f)
                    if (isNeedSave)
                        Toast.makeText(applicationContext, "保存成功", Toast.LENGTH_SHORT).show()
                }
            } else if (state == Producer.State.kWRITING){
                val currentProgress = (ts / producer.duration.toFloat()) * 100f
                runOnUiThread {
                    lsq_editor_cut_load.setVisibility(View.VISIBLE)
                    lsq_editor_cut_load_parogress.setValue(currentProgress)
                }
            }
        }

        if (!producer.init(outputFilePath)){
            return
        }
        setEnable(false)
        if (!producer.start()){
            TLog.e("[Error] EditorProducer Start failed")
        }
        mCurrentProducer = producer
    }

    private fun saveDraft(){
        mThreadPool.execute {
            val outputFileName = "editor_draft${System.currentTimeMillis()}"
            val outputFile = "${externalCacheDir!!.absolutePath}/draft"
            val file = File(outputFile)
            if (file.mkdirs()) {

            }
            val outputFilePath = "${outputFile}/${outputFileName}"
            TLog.e("draft path ${outputFilePath}")
            mEditor!!.model.save(outputFilePath)

            val currentModule = FunctionType.Graffiti

            val sp = getSharedPreferences("Tu-Draft-list", Context.MODE_PRIVATE)
            val cal = Calendar.getInstance()
            val draftItem = DraftItem(currentModule.ordinal, outputFilePath, DateHelper.format(cal, "yyyy/MM/dd HH:mm:ss"), outputFileName)
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


    private fun initPlayer(){
        if (!mEditor.build()){

        }

        mPlayer = mEditor.newPlayer()
        mPlayer?.setListener { state, ts ->
            mCurrentState = state
            if (state == Player.State.kDO_PLAY || state == Player.State.kDO_PAUSE){
                runOnUiThread {
                    val durationMS = mPlayer!!.duration
                    mMaxDuration = durationMS
                    seekBar.max = durationMS.toInt()
                }
            } else if (state == Player.State.kEOS){
                runOnUiThread {
                    lsq_graffiti_play.setImageResource(R.mipmap.edit_ic_play)
                    mThreadPool.execute {
                        mPlayer!!.previewFrame(0)
                    }
                }
            } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW){
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
                    lsq_player_duration.text = "${currentVideoHour}:$currentVideoMinute:$currentVideoSecond/$durationVideoHour:$durationVideoMinute:$durationVideoSecond"
                }
            }
        }

        if (!mPlayer!!.open()){
            TLog.e("Editor Player Open failed")
        }

        runOnUiThread {
            lsq_graffiti_displayView.attachPlayer(mPlayer)
            val dur = mPlayer!!.duration.toInt()
            seekBar.max = dur
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
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

    private fun initLayer(){
        val duration = mVideoItem!!.mVideoClip.streamInfo.duration

        val videoLayer = ClipLayer(mEditor!!.context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        videoLayer.setConfig(videoLayerConfig)
        if (!videoLayer.addClip(mVideoItem!!.mId.toInt(), mVideoItem!!.mVideoClip)) {
            return
        }

        val audioLayer = ClipLayer(mEditor!!.context, false)
        val audioLayerConfig = Config()
        audioLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER, 1)
        audioLayer.setConfig(audioLayerConfig)

        if (!audioLayer.addClip(mVideoItem!!.mId.toInt(), mVideoItem!!.mAudioClip)) {
            return
        }

        mEditor!!.videoComposition().addLayer(11, videoLayer)
        mEditor.audioComposition().addLayer(11, audioLayer)

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
        mMaxDuration = duration

        mCurrentEndPos = duration
    }

    private fun restoreLayer(){
        val videoLayer = mEditor.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor.audioComposition().allLayers[11] as ClipLayer

        val videoId = videoLayer.allClips.keys.first()
        val videoClip = videoLayer.allClips.values.first()
        val audioClip = audioLayer.allClips.values.first()

        val videoPath = videoClip.config.getString(VideoFileClip.CONFIG_PATH)

        mVideoItem = VideoItem(videoPath,videoId.toLong(),videoClip,audioClip)

        val duration = videoClip.streamInfo.duration

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
        mMaxDuration = duration
        mCurrentEndPos = duration

        val layers = mEditor.videoComposition().allLayers

        mGraffitiId += layers.size + 1

        runOnUiThread {

            if (layers.size == 1) lsq_revoke_graffiti.isEnabled = false
            else lsq_revoke_graffiti.isEnabled = true

        }



    }

    private fun playerPlay(){
        mThreadPool.execute {
            mPlayer?.play()
            runOnUiThread {
                lsq_graffiti_play.setImageResource(R.mipmap.edit_ic_pause)
            }
        }
    }

    private fun playerPause(){
        mThreadPool.execute {
            mPlayer?.pause()
            runOnUiThread {
                lsq_graffiti_play.setImageResource(R.mipmap.edit_ic_play)
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        playerPause()
    }


    private fun createGraffiti(){
        val res : Future<GraffitiItem> = mThreadPool.submit(Callable<GraffitiItem> {

            mPlayer!!.lock()

            val graffitiClip = Clip(mEditor.context,GraffitiClip.TYPE_NAME)
            val graffitiConfig = Config()
            graffitiConfig.setNumber(GraffitiClip.CONFIG_DURATION,mCurrentEndPos - mCurrentStartPos)
            graffitiClip.setConfig(graffitiConfig)

            val graffitiLayer = ClipLayer(mEditor.context,true)
            val layerConfig = Config()
            layerConfig.setNumber(Layer.CONFIG_START_POS,mCurrentStartPos)
            graffitiLayer.setConfig(layerConfig)

            graffitiLayer.addClip(1,graffitiClip)

            val graffitiProperty = GraffitiClip.PropertyBuilder()


            val item = GraffitiItem(mGraffitiId,graffitiLayer,graffitiClip,graffitiProperty)

            mEditor.videoComposition().addLayer(mGraffitiId++,graffitiLayer)

            mPlayer!!.unlock()

            item
        })

        mCurrentGraffitiItem = res.get()
    }

    private fun stopGraffitiDraw(){
        mCurrentGraffitiItem = null
    }




    private fun convertedPoint(x: Float, y: Float): PointF {
        val topOffset = if (mVideoRect!!.top != 0) {
            mVideoRect!!.top
        } else {
            TuSdkViewHelper.locationInWindowTop(lsq_graffiti_layer)
        }
        val previewSize: TuSdkSize = TuSdkSize(lsq_graffiti_layer.measuredWidth, lsq_graffiti_layer.measuredHeight)
        val screenSize = previewSize

        val previewRectF = RectF(
                0f,
                0f,
                previewSize.width.toFloat(),
                previewSize.height.toFloat()
        )
        if (!previewRectF.contains(x, y)) {
            return PointF( (-1f), (-1f))
        }

        var pointX = -1f
        var pointY = -1f

        pointX = x / previewSize.width.toFloat()
        pointY = (y) / previewSize.height.toFloat()

        return PointF( pointX, pointY)
    }
}