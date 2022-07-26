/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2021/5/17$ 11:19$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.ArraySet
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import cn.bar.DoubleHeadedDragonBar
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.tusdk.pulse.*
import com.tusdk.pulse.editor.*
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.MosaicEffect
import kotlinx.android.synthetic.main.include_title_layer.*
import kotlinx.android.synthetic.main.mosaic_activity.*
import kotlinx.android.synthetic.main.mosaic_activity.lsq_editor_cut_load
import kotlinx.android.synthetic.main.mosaic_activity.lsq_editor_cut_load_parogress
import kotlinx.android.synthetic.main.mosaic_activity.lsq_player_duration
import kotlinx.android.synthetic.main.mosaic_activity.seekBar
import org.jetbrains.anko.toast
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.BaseActivity
import org.lasque.tusdkeditoreasydemo.base.DraftItem
import org.lasque.tusdkeditoreasydemo.base.EditorPlayerContext
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import org.lasque.tusdkeditoreasydemo.base.views.EffectItemView
import org.lasque.tusdkeditoreasydemo.base.views.EffectLayerViewDelegate
import org.lasque.tusdkeditoreasydemo.base.views.EffectType
import org.lasque.tusdkeditoreasydemo.base.views.MosaicItemView
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.DateHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.collections.LinkedHashMap

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/5/17  11:19
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class MosaicActivity : BaseActivity() {

    private var mEditor : VideoEditor = VideoEditor()

    private var mThreadPool : ExecutorService = Executors.newSingleThreadExecutor()

    private var mPlayer : VideoEditor.Player? = null

    private var mCurrentState = Player.State.kREADY

    private var mVideoItem : VideoItem? = null

    private var mVideoLayer : ClipLayer? = null

    private var mAudioLayer : ClipLayer? = null

    private var mMaxDuration = 1L

    private var mVideoRect : Rect? = null

    private var mOutputWidth = 800

    private var mOutputHeight = 800

    private var isSeekBarTouch = false

    private var mCurrentStartPos = 0L

    private var mCurrentEndPos = 0L

    private var mCurrentDuration = 0L

    private var mPlayerContext : EditorPlayerContext = EditorPlayerContext(mEditor)

    /**************************************** Mosaic *************************************************** */

    private var mCurrentMosaic : Effect? = null

    private var mCurrentPathMosaicIndex = -1

    private var mCurrentConfig : Config? = null

    private var mCurrentMosaicProperty : MosaicEffect.PropertyBuilder? = null

    private var mCurrentPath : MosaicEffect.MosaicPath? =null

    private var mCurrentCode : String = MosaicEffect.CODE_FILL

    private var mCurrentThickness : Double = 0.1

    private var mCurrentItemView : MosaicItemView? = null

    private var mPathMosaicIndexList : LinkedHashMap<Int,MosaicEffect.PropertyBuilder> = LinkedHashMap()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mosaic_activity)

        lsq_back.setOnClickListener { finish() }

        lsq_title.setText(FunctionType.Mosaic.mTitleId)

        lsq_mosaic_displayView.init(Engine.getInstance().mainGLContext)

        val modelPath = intent.getStringExtra(DraftItem.Draft_Path_Key)

        if (TextUtils.isEmpty(modelPath)){
            val res = mThreadPool.submit(Callable<Boolean>{
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


        lsq_mosaic_layer.setOnTouchListener { v, event ->
            if (mCurrentMosaic == null) createMosaic()

            lsq_mosaic_reset.visibility = View.VISIBLE

            when(event.action){
                MotionEvent.ACTION_DOWN->{
                    playerPause()

                    val res : Future<MosaicEffect.MosaicPath> = mThreadPool.submit(Callable<MosaicEffect.MosaicPath> {
                        val thickness = lsq_mosaic_thickness_bar.progress
                        val currentPath = MosaicEffect.MosaicPath()
                        val firstPoint = convertedPoint(event.x,event.y)
                        currentPath.points.add(firstPoint)
                        currentPath.thickness = mCurrentThickness
                        currentPath.code = mCurrentCode
                        mCurrentMosaicProperty!!.holder.mosaicPaths.add(currentPath)
                        mCurrentMosaic!!.setProperty(MosaicEffect.PATH_PROP_PARAM,mCurrentMosaicProperty!!.makePathProperty())

                        mPlayerContext.refreshFrame()

                        currentPath
                    })

                    mCurrentPath = res.get()

                }

                MotionEvent.ACTION_MOVE -> {
                    val res : Future<Boolean> = mThreadPool.submit(Callable<Boolean>{
                        if (mCurrentPath == null) return@Callable false

                        val point = convertedPoint(event.x,event.y)
                        mCurrentPath!!.points.add(point)

                        mCurrentMosaic!!.setProperty(MosaicEffect.PATH_PROP_PARAM,mCurrentMosaicProperty!!.makePathProperty())

                        mPlayerContext.refreshFrame()

                        true

                    })

                    res.get()

                }

                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {
                    val res : Future<Boolean> = mThreadPool.submit(Callable<Boolean> {
                        if (mCurrentPath == null) return@Callable false
                        val point = convertedPoint(event.x,event.y)
                        mCurrentPath!!.points.add(point)
                        mCurrentMosaic!!.setProperty(MosaicEffect.PATH_PROP_PARAM,mCurrentMosaicProperty!!.makePathProperty())

                        mPlayerContext.refreshFrame()

                        true

                    })

                    res.get()
                    mCurrentPath == null
                }


            }

            return@setOnTouchListener true

        }

        lsq_effect_layer.setEditor(mEditor)
        lsq_effect_layer.setThreadPool(mThreadPool)
        lsq_effect_layer.setPlayerContext(mPlayerContext)
        lsq_effect_layer.setEffectLayerViewDelegate(object : EffectLayerViewDelegate{
            override fun onItemViewSelected(type: EffectType, view: EffectItemView) {
                mCurrentItemView = view as MosaicItemView

                lsq_mosaic_duration.post {
                    lsq_mosaic_duration.minValue = (mCurrentItemView!!.getStartPos().toFloat() / mMaxDuration * 100.0).toInt()
                    lsq_mosaic_duration.maxValue = (mCurrentItemView!!.getEndPos().toFloat() / mMaxDuration * 100).toInt()
                    lsq_mosaic_duration.invalidate()
                }

            }

            override fun onItemViewUnselected(type: EffectType, view: EffectItemView) {
                mCurrentItemView = null
            }

            override fun onItemViewClose(type: EffectType, view: EffectItemView) {
                mCurrentItemView = null
                if (!lsq_effect_layer.hasEffect()){
                    lsq_mosaic_reset.visibility = View.GONE
                }
            }

        })

        lsq_mosaic_play.setOnClickListener {
            if (mCurrentState != Player.State.kPLAYING){
                playerPlay()
            } else {
                playerPause()
            }
        }

        lsq_mosaic_thickness_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mCurrentThickness = progress.toDouble() / 100.0
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        lsq_mosaic_duration.setCallBack(object : DoubleHeadedDragonBar.DhdBarCallBack(){

            override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                val startTime = (mMaxDuration * minPercentage / 100).toLong()
                val endTime = (mMaxDuration * maxPercentage / 100).toLong()

                if (startTime == endTime){
                    toast("马赛克起止位置不能相同")
                    lsq_mosaic_duration.minValue = (mCurrentStartPos / mMaxDuration.toFloat() * 100.0).toInt()
                    lsq_mosaic_duration.maxValue = (mCurrentEndPos / mMaxDuration.toFloat() * 100.0).toInt()
                    lsq_mosaic_duration.invalidate()
                    return
                }

                var targetPreviewPos = mCurrentStartPos

                if (mCurrentStartPos == startTime){
                    targetPreviewPos = endTime
                } else {
                    targetPreviewPos = startTime
                }

                mCurrentStartPos = startTime
                mCurrentEndPos = endTime

                if (mCurrentMosaic != null){
                    mThreadPool?.execute {
                        mPlayer!!.lock()
                        mCurrentConfig?.setNumber(MosaicEffect.CONFIG_START_POS,mCurrentStartPos)
                        mCurrentConfig?.setNumber(MosaicEffect.CONFIG_END_POS,mCurrentEndPos)
                        mCurrentMosaic?.setConfig(mCurrentConfig)
                        mPlayer!!.unlock()
                    }
                } else {
                    mCurrentItemView?.updateEffectPos(mCurrentStartPos,mCurrentEndPos)
                }

                mThreadPool.execute {
                    mEditor.player.previewFrame(targetPreviewPos)
                }
            }
        })

        lsq_mosaic_displayView.viewTreeObserver.addOnGlobalLayoutListener {
            mVideoRect = lsq_mosaic_displayView.getInteractionRect(mOutputWidth,mOutputHeight)
            lsq_effect_layer.resize(mVideoRect!!)
        }

        lsq_mosaic_back_to_main.setOnClickListener {
            lsq_effect_layer.visibility = View.VISIBLE
            lsq_mosaic_layer.visibility = View.GONE
            lsq_mosaic_back_to_main.visibility = View.GONE
            lsq_mosaic_rect.visibility = View.VISIBLE
            lsq_mosaic_path.visibility = View.VISIBLE
            lsq_mosaic_path_layer.visibility = View.GONE

            mCurrentPathMosaicIndex = -1
            mCurrentMosaic = null
        }

        lsq_mosaic_rect.setOnClickListener {
            lsq_mosaic_layer.visibility = View.GONE
            lsq_mosaic_rect.visibility = View.VISIBLE
            lsq_mosaic_path.visibility = View.VISIBLE
            lsq_mosaic_back_to_main.visibility = View.GONE
            lsq_effect_layer.visibility = View.VISIBLE

            playerPause()

            lsq_effect_layer.appendMosaic(mCurrentDuration,mMaxDuration)

            lsq_mosaic_reset.visibility = View.VISIBLE

        }

        lsq_mosaic_path.setOnClickListener {
            lsq_mosaic_layer.visibility = View.VISIBLE
            lsq_effect_layer.visibility = View.INVISIBLE
            lsq_mosaic_path_layer.visibility = View.VISIBLE
            lsq_mosaic_back_to_main.visibility = View.VISIBLE
            lsq_mosaic_rect.visibility = View.GONE
            lsq_mosaic_path.visibility = View.GONE
        }

        lsq_mosaic_fill.setOnClickListener {
            mCurrentCode = MosaicEffect.CODE_FILL
            lsq_mosaic_fill.setBackgroundResource(R.drawable.blue_background)
            lsq_mosaic_eraser.setBackgroundResource(R.drawable.gray_background)

        }

        lsq_mosaic_eraser.setOnClickListener {
            mCurrentCode = MosaicEffect.CODE_ERASER
            lsq_mosaic_fill.setBackgroundResource(R.drawable.gray_background)
            lsq_mosaic_eraser.setBackgroundResource(R.drawable.blue_background)

        }

        lsq_mosaic_reset.setOnClickListener {
            val layerIndex = lsq_effect_layer.getLastEffectIndex()
            val pathIndex = if (mPathMosaicIndexList.isEmpty()){-1}else{mPathMosaicIndexList.keys.last()}

            if (pathIndex != -1 && pathIndex > layerIndex){
                val targetMosaic = mVideoLayer!!.effects()[pathIndex]
                if (targetMosaic != null){
                    val res = mThreadPool.submit{
                        val property = mPathMosaicIndexList[pathIndex]!!
                        if (property.holder.mosaicPaths.isNotEmpty()){
                            try {
                                property!!.holder.mosaicPaths.removeAt(property!!.holder.mosaicPaths.size - 1)
                                targetMosaic!!.setProperty(MosaicEffect.PATH_PROP_PARAM,property!!.makePathProperty())
                            } catch (e : Exception){

                            }

                        }

                        if (property!!.holder.mosaicPaths.isEmpty()){
                            mPlayer?.lock()
                            mVideoLayer!!.effects().delete(pathIndex)
                            mEditor.build()
                            mPlayer?.unlock()
                            mPathMosaicIndexList.remove(pathIndex)
                        }
                        mPlayerContext.refreshFrame()
                    }

                    res.get()
                }
            } else {
                if (lsq_effect_layer.hasEffect()){
                    if (layerIndex > 0){
                        lsq_effect_layer.removeEffect(layerIndex)
                    }
                }
            }
            if (!lsq_effect_layer.hasEffect() && mPathMosaicIndexList.isEmpty()){
                lsq_mosaic_reset.visibility = View.GONE
            }
        }

        lsq_mosaic_reset.visibility = View.GONE


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

            val currentModule = FunctionType.Mosaic

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
            mPlayerContext.state = mCurrentState
            if (state == Player.State.kDO_PLAY || state == Player.State.kDO_PAUSE){
                runOnUiThread {
                    val durationMS = mPlayer!!.duration
                    mMaxDuration = durationMS
                    seekBar.max = durationMS.toInt()
                }
            } else if(state == Player.State.kEOS){
                runOnUiThread {
                    lsq_mosaic_play.setImageResource(R.mipmap.edit_ic_play)
                    mThreadPool.execute { mPlayer!!.previewFrame(0) }
                }
            } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW){

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
                    lsq_effect_layer.requestShower(ts)

                    if (!isSeekBarTouch){
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
            lsq_mosaic_displayView.attachPlayer(mPlayer)
            val dur = mPlayer!!.duration.toInt()
            seekBar.max = dur
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{

                var current = 0

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
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

        val effects = mEditor.videoComposition().effects().all



        EffectItemView.CURRENT_EFFECT_ID += effects.size
        EffectItemView.CURRENT_EFFECT_ID += videoLayer.effects().all.size

        val effectMap = videoLayer.effects()
        for (set in effectMap.all){
            val property = set.value.getProperty(MosaicEffect.PATH_PROP_PARAM)
            val holder = MosaicEffect.PropertyHolder(property)
            val builder = MosaicEffect.PropertyBuilder(holder)

            mPathMosaicIndexList.put(set.key,builder)
        }

        runOnUiThread {
            for ( key in effects.keys){
                lsq_effect_layer.restoreMosaic(key)
            }
            if (mCurrentMosaic != null || lsq_effect_layer.hasEffect()){
                lsq_mosaic_reset.visibility = View.VISIBLE
            }

        }
    }

    private fun playerPlay(){
        mThreadPool.execute {
            mPlayer?.play()
            runOnUiThread {
                lsq_mosaic_play.setImageResource(R.mipmap.edit_ic_pause)
            }
        }
    }

    private fun playerPause(){
        mThreadPool.execute {
            mPlayer?.pause()
            runOnUiThread {
                lsq_mosaic_play.setImageResource(R.mipmap.edit_ic_play)
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

    private fun createMosaic(){
        val res : Future<Boolean> = mThreadPool.submit(Callable<Boolean> {

            mPlayer!!.lock()

            val mosaicEffect = Effect(mEditor!!.context,MosaicEffect.TYPE_NAME)
            val mosaicConfig = Config()
            mosaicConfig.setNumber(MosaicEffect.CONFIG_START_POS,mCurrentDuration)
            val endPos = mMaxDuration
            mosaicConfig.setNumber(MosaicEffect.CONFIG_END_POS,endPos)
            mCurrentEndPos = endPos

            mosaicEffect.setConfig(mosaicConfig)

            val mosaicProperty = MosaicEffect.PropertyBuilder()

            val currentIndex = ++EffectItemView.CURRENT_EFFECT_ID

            mVideoLayer!!.effects().add(currentIndex,mosaicEffect)

            mEditor.build()

            mPlayer!!.unlock()

            mCurrentPathMosaicIndex = currentIndex
            mCurrentMosaic = mosaicEffect
            mCurrentConfig = mosaicConfig
            mCurrentMosaicProperty = mosaicProperty

            mPathMosaicIndexList.put(currentIndex,mosaicProperty)


            true
        })

        lsq_mosaic_duration.post {
            lsq_mosaic_duration.minValue = (mCurrentDuration.toFloat() / mMaxDuration * 100).toInt()
            lsq_mosaic_duration.maxValue = (mCurrentEndPos.toFloat() / mMaxDuration * 100).toInt()
            lsq_mosaic_duration.invalidate()
        }

        res.get()
    }

    private fun convertedPoint(x: Float, y: Float): PointF {
        val topOffset = if (mVideoRect!!.top != 0) {
            mVideoRect!!.top
        } else {
            TuSdkViewHelper.locationInWindowTop(lsq_mosaic_layer)
        }
        val previewSize: TuSdkSize = TuSdkSize(lsq_mosaic_layer.measuredWidth, lsq_mosaic_layer.measuredHeight)
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