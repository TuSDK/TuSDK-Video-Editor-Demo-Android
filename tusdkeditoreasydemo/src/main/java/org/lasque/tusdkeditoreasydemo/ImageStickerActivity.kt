/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/11/18$ 11:33$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import cn.bar.DoubleHeadedDragonBar
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.tusdk.pulse.Config
import com.tusdk.pulse.Engine
import com.tusdk.pulse.Player
import com.tusdk.pulse.Producer
import com.tusdk.pulse.editor.*
import com.tusdk.pulse.editor.Layer.*
import com.tusdk.pulse.editor.clips.ImageClip
import com.tusdk.pulse.editor.clips.VideoFileClip
import kotlinx.android.synthetic.main.image_sticker_activity.*
import kotlinx.android.synthetic.main.image_sticker_activity.lsq_editor_cut_load
import kotlinx.android.synthetic.main.image_sticker_activity.lsq_sticker_play
import kotlinx.android.synthetic.main.include_image_blend_layout.*
import kotlinx.android.synthetic.main.include_image_duration_layout.*
import kotlinx.android.synthetic.main.include_image_function_layout.*
import kotlinx.android.synthetic.main.include_image_layer_list_layout.*
import kotlinx.android.synthetic.main.include_sticker_view.*
import kotlinx.android.synthetic.main.include_sticker_view.lsq_api_displayView
import kotlinx.android.synthetic.main.include_sticker_view.lsq_editor_play
import kotlinx.android.synthetic.main.include_sticker_view.lsq_player_duration
import kotlinx.android.synthetic.main.include_sticker_view.seekBar
import kotlinx.android.synthetic.main.include_title_layer.*
import kotlinx.android.synthetic.main.repeat_fragment.view.*
import org.jetbrains.anko.toast
import org.lasque.tusdkpulse.core.utils.DateHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlHelper
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.*
import org.lasque.tusdkeditoreasydemo.base.views.stickers.*
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.max

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/18  11:33
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class ImageStickerActivity : BaseActivity() {

    companion object{
        const val IMAGE_REQUEST_CODE = 2
        const val VIDEO_REQUEST_CODE = 3
        const val MATERIAL_ADD_REQUEST_CODE = 4
    }

    private var mEditor : VideoEditor = VideoEditor()

    private var mImageList = ArrayList<String>()

    private var mThreadPool = Executors.newSingleThreadExecutor()

    private var mPlayer : VideoEditor.Player? = null

    private var mCurrentState = Player.State.kREADY

    private var isSeekBarTouch = false

    private var mCurrentDutation = 0L

    private var mVideoItem : VideoItem? = null

    private var mVideoLayer : ClipLayer? = null

    private var mAudioLayer : ClipLayer? = null

    private var mMaxDuration = 0L

    private var mVideoRect : Rect? = null

    private var mCurrentItemViewStart : Long = 0

    private var mCurrentItemViewEnd : Long = 0

    private var mCurrentItemView : LayerItemViewBase? = null

    private var mPlayerContext : EditorPlayerContext = EditorPlayerContext(mEditor)

    private var mFunctionType = FunctionType.PictureInPicture

    private var isLayerInit = false

    private var isFromModel = false

    private var mBlendAdapter : BlendAdapter? = null

    private var isNeedBackToMain = false

    private val mGlobalLayoutListener : ViewTreeObserver.OnGlobalLayoutListener? = ViewTreeObserver.OnGlobalLayoutListener {
        mVideoRect = lsq_api_displayView.getInteractionRect(800,800)
        lsq_sticker_view.resize(mVideoRect!!)
    }

    public fun backToMain(){
        lsq_image_function_layer.visibility = View.VISIBLE
        lsq_image_duration_layer.visibility = View.GONE
        lsq_image_blend_layer.visibility = View.GONE
        lsq_image_list_layer.visibility = View.GONE
        isNeedBackToMain = false
        lsq_add_image.setText("添加一张图片/视频")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_sticker_activity)
        mFunctionType = intent.getSerializableExtra("FunctionType") as FunctionType
        lsq_back.setOnClickListener {
            finish()
        }

        lsq_title.setText(mFunctionType.mTitleId)

        lsq_api_displayView.init(Engine.getInstance().mainGLContext)

        lsq_sticker_view.setCurrentLayerType(LayerType.ImageAndVideo)
        lsq_sticker_view.setEditor(mEditor)
        lsq_sticker_view.setThreadPool(mThreadPool)
        lsq_sticker_view.setPlayerContext(mPlayerContext)

        lsq_sticker_view.setLayerViewDelegate(object : LayerViewDelegate{
            override fun onItemViewSelected(type: LayerType, view: LayerItemViewBase) {
                playerPause()
                mCurrentItemView = view
                var startPos = (mCurrentItemView!!.getLayerStartPos() / mCurrentItemView!!.getLayerMaxDuration().toDouble()) * 100
                var endPos = (mCurrentItemView!!.getLayerEndPos() / mCurrentItemView!!.getLayerMaxDuration().toDouble()) * 100
                lsq_bar_can_touch.isClickable = false
                setTimeBarValue(startPos.toInt(), endPos.toInt())
                mBlendAdapter?.findMode(mCurrentItemView!!.getBlendMode())
                runOnUiThread {
                    lsq_image_duration_btn.visibility = View.VISIBLE
                    lsq_image_blend_btn.visibility = View.VISIBLE
                }

            }

            override fun onItemViewReleased(type: LayerType, view: LayerItemViewBase) {

            }

            override fun onItemUnselected(type: LayerType) {
                runOnUiThread {
                    mCurrentItemView = null
                    setTimeBarValue(0,0)
                    lsq_bar_can_touch.isClickable = true
                    mBlendAdapter?.setCurrentPosition(-1)
                    lsq_image_duration_btn.visibility = View.GONE
                    lsq_image_blend_btn.visibility = View.GONE
                    backToMain()
                }
            }

        })

        val modelPath = intent.getStringExtra(DraftItem.Draft_Path_Key)
        if (TextUtils.isEmpty(modelPath)){
            mThreadPool.execute {
                val openConfig = VideoEditor.OpenConfig()
                openConfig.width = 800
                openConfig.height = 800
                if (!mEditor.create(openConfig)){
                    TLog.e("Editor Create failed")
                }

                runOnUiThread {
                    openAlbum(1,false,false,requestCode = IMAGE_REQUEST_CODE)
                }
            }
        } else {
            isFromModel = true
            mThreadPool.execute {
                val editorModel = EditorModel(modelPath)
                if (!mEditor.create(editorModel)){
                    TLog.e("Editor Create failed")
                }
                if (!mEditor.build()){
                    TLog.e("Editor Build failed")
                }

                initPlayer()
                val videoLayers = ArrayList<Int>()
                val imageLayers = ArrayList<Int>()
                val layerMaps = mEditor!!.videoComposition().allLayers
                for (key in layerMaps.keys){
                    val currentLayer = (layerMaps[key] as ClipLayer)
                    if (currentLayer.getClip(ImageLayerItemView.IMAGE_CLIP_ID) != null){
                        imageLayers.add(key)
                    } else if (currentLayer.getClip(VideoLayerItemView.Video_CLIP_ID) != null){
                        videoLayers.add(key)
                    }
                }

                runOnUiThread {
                    Collections.sort(videoLayers)
                    Collections.sort(imageLayers)

                    var videoLast = videoLayers.lastOrNull()
                    if (videoLast == null) videoLast = 30

                    var imageLast = imageLayers.lastOrNull()
                    if (imageLast == null) imageLast = 30

                    LayerItemViewBase.CURRENT_LAYER_ID = max(videoLast,imageLast) + 1

                    for (i in videoLayers){
                        lsq_sticker_view.restoreVideo(i.toLong())
                    }
                    for (i in imageLayers){
                        lsq_sticker_view.restoreImage(i.toLong())
                    }
                }
            }
        }



        lsq_bar_can_touch.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                return lsq_bar_can_touch.isClickable
            }

        })

        lsq_editor_play.setOnClickListener {
            if (mCurrentState != Player.State.kPLAYING){
                mThreadPool.execute {
                    mPlayer!!.play()
                    runOnUiThread {
                        lsq_editor_play.setImageResource(R.mipmap.edit_ic_pause)
                    }
                }
            } else {
                mThreadPool.execute {
                    mPlayer!!.pause()
                    runOnUiThread {
                        lsq_editor_play.setImageResource(R.mipmap.edit_ic_play)
                    }
                }
            }
        }
        lsq_add_image.setText("添加一张图片/视频")
        lsq_start_title.setText("素材起止位置")


        lsq_api_displayView.viewTreeObserver.addOnGlobalLayoutListener(mGlobalLayoutListener)

        lsq_add_image.setOnClickListener {
            if (isNeedBackToMain){
                backToMain()
            } else {
                playerPause()
                openAlbum(1,false,false, requestCode = MATERIAL_ADD_REQUEST_CODE)
            }
        }


        lsq_output_video.setOnClickListener {
            playerPause()
            MaterialDialog(this).show {
                title(text = "保存选项")
                message(text = "请选择保存为视频或者草稿")

                positiveButton(text = "保存为视频") {dialog ->
                    dialog.dismiss()
                    saveVideo()
                }

                negativeButton(text = "保存为草稿") {dialog ->
                    dialog.dismiss()
                    saveDraft()
                }

                cancelOnTouchOutside(true)
            }
        }

        lsq_sticker_play.setOnClickListener {
            if (mCurrentState != Player.State.kPLAYING){
                playerPlay()
            } else {
                playerPause()
            }
        }
        val blendModes = mutableListOf<String>(
                BLEND_MODE_Default,
                BLEND_MODE_Normal,
                BLEND_MODE_Add,
                BLEND_MODE_Substract,
                BLEND_MODE_Negation,
                BLEND_MODE_Average,
                BLEND_MODE_Multiply,
                BLEND_MODE_Difference,
                BLEND_MODE_Screen,
                BLEND_MODE_Softlight,
                BLEND_MODE_Hardlight,
                BLEND_MODE_Lighten,
                BLEND_MODE_Darken,
                BLEND_MODE_Reflect,
                BLEND_MODE_Exclusion
        )
        val blendAdapter = BlendAdapter(blendModes,this)
        blendAdapter.setOnItemClickListener(object : OnItemClickListener<String, BlendAdapter.BlendViewHolder>{
            override fun onItemClick(pos: Int, holder: BlendAdapter.BlendViewHolder, item: String) {
                mThreadPool.execute {
                    mCurrentItemView?.updateBlendMode(item)
                }
                blendAdapter.setCurrentPosition(pos)
            }

        })
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        lsq_blend_list.layoutManager = layoutManager
        lsq_blend_list.adapter = blendAdapter
        mBlendAdapter = blendAdapter

        lsq_blend_mix_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (!p2) return
                mThreadPool.execute {
                    mCurrentItemView?.updateBlendMix(p1 / 100.0)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

        })

        val layerListAdapter = LayerListAdapter(mutableListOf(),this)
        layerListAdapter.setOnClipChangeListener(object : LayerListAdapter.OnClipChangeListener{
            override fun onSwap(item0: LayerItem, item1: LayerItem) {
                mThreadPool?.execute {
                    playerPause()
                    mPlayer!!.lock()
                    val res = mEditor!!.videoComposition().swapLayer(item0.id,item1.id)
                    if (!res){
                        return@execute
                    }
                    lsq_sticker_view.swapLayer(item0.id,item1.id)
                    val id = item0.id
                    item0.id = item1.id
                    item1.id = id
                    if (!mEditor!!.build()){

                    }
                    mPlayer!!.unlock()
                    mPlayerContext.refreshFrame()
                }
            }

        })
        val layerLayoutManager = LinearLayoutManager(this)
        layerLayoutManager.orientation = LinearLayoutManager.VERTICAL
        lsq_layer_list.layoutManager = layerLayoutManager
        lsq_layer_list.adapter = layerListAdapter

        lsq_image_duration_btn.setOnClickListener {
            isNeedBackToMain = true
            lsq_image_function_layer.visibility = View.GONE
            lsq_image_duration_layer.visibility = View.VISIBLE
            lsq_add_image.setText("返回首页")
        }

        lsq_image_blend_btn.setOnClickListener {
            isNeedBackToMain = true
            lsq_blend_mix_bar.progress = (mCurrentItemView!!.getBlendMix() * 100).toInt()
            lsq_image_function_layer.visibility = View.GONE
            lsq_image_blend_layer.visibility = View.VISIBLE
            lsq_add_image.setText("返回首页")
        }

        lsq_image_layer_btn.setOnClickListener {
            isNeedBackToMain = true
            lsq_image_function_layer.visibility = View.GONE
            lsq_image_list_layer.visibility = View.VISIBLE
            lsq_add_image.setText("返回首页")

            layerListAdapter.refreshItems(getLayerList())
        }


    }

    private fun getLayerList() : MutableList<LayerItem> {
        val layerList = ArrayList<LayerItem>()
        val layerMaps = mEditor.videoComposition().allLayers
        for (key in layerMaps.keys) {
            val layer = layerMaps[key] as ClipLayer
            var clip: Clip? = layer.getClip(VideoLayerItemView.Video_CLIP_ID)
            var path = ""
            if (clip == null) {
                clip = layer.getClip(ImageLayerItemView.IMAGE_CLIP_ID)
                path = clip!!.config.getString(ImageClip.CONFIG_PATH)
            } else {
                path = clip!!.config.getString(VideoFileClip.CONFIG_PATH)
            }

            val layerItem = LayerItem(key, layer, path)
            layerList.add(layerItem)
        }
        Collections.sort(layerList, kotlin.Comparator { t, t2 ->
            if (t.id > t2.id) {-1} else if (t.id < t2.id){1} else {0}
        })
        return layerList
    }

    private fun saveVideo() {
        mThreadPool.execute {
            val producer = mEditor.newProducer()
    //                val outputFilePath = "${TuSdkContext.context().getExternalFilesDir(DIRECTORY_DCIM)!!.absolutePath}/editor_output${System.currentTimeMillis()}.mp4"

            val outputFilePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath}/editor_output${System.currentTimeMillis()}.mp4"

            val config = Producer.OutputConfig()
            config.watermark = BitmapHelper.getRawBitmap(this, R.raw.sample_watermark)
            config.watermarkPosition = 1
            producer.setOutputConfig(config)
            producer.setListener { state, ts ->
                if (state == Producer.State.kEND) {
                    mThreadPool.execute {
                        producer.cancel()
                        producer.release()
                        mEditor!!.resetProducer()
                    }
                    val contentValue = ImageSqlHelper.getCommonContentValues()
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(outputFilePath))))
                    runOnUiThread {
                        setEnable(true)
                        lsq_editor_cut_load.setVisibility(View.GONE)
                        lsq_editor_cut_load_parogress.setValue(0f)
                        Toast.makeText(applicationContext, "保存成功", Toast.LENGTH_SHORT).show()
                    }
                } else if (state == Producer.State.kWRITING) {
                    runOnUiThread {
                        lsq_editor_cut_load.setVisibility(View.VISIBLE)
                        lsq_editor_cut_load_parogress.setValue((ts / producer.duration.toFloat()) * 100f)
                    }
                }
            }

            if (!producer.init(outputFilePath)) {
                return@execute
            }
            setEnable(false)
            if (!producer.start()) {
                TLog.e("[Error] EditorProducer Start failed")

            }
        }
    }

    private fun saveDraft(){
        mThreadPool.execute {
            val outputFileName = "editor_draft${System.currentTimeMillis()}"
            val outputFile = "${externalCacheDir!!.absolutePath}/draft"
            val file = File(outputFile)
            if (file.mkdirs()){

            }
            val outputFilePath = "${outputFile}/${outputFileName}"
            TLog.e("draft path ${outputFilePath}")
            mEditor!!.model.save(outputFilePath)

            val currentModule = FunctionType.PictureInPicture

            val sp = getSharedPreferences("Tu-Draft-list", Context.MODE_PRIVATE)
            val cal = Calendar.getInstance()
            val draftItem = DraftItem(currentModule.ordinal,outputFilePath, DateHelper.format(cal,"yyyy/MM/dd HH:mm:ss"),outputFileName)
            val gson = Gson()
            val draftListJson = sp.getString(DraftItem.Draft_List_Key,"")
            var draftList = gson.fromJson(draftListJson,ArrayList::class.java)
            if (draftList != null){
                (draftList as ArrayList<DraftItem>).add(draftItem)
            } else {
                draftList = ArrayList<DraftItem>()
                draftList.add(draftItem)
            }
            val draftString = gson.toJson(draftList)
            sp.edit().putString(DraftItem.Draft_List_Key,draftString).apply()

            runOnUiThread {
                MaterialDialog(this).show {
                    title(text = "草稿箱保存成功")
                    positiveButton(text = "确定"){dialog ->
                        dialog.dismiss()
                    }

                    cancelOnTouchOutside(true)
                }
            }
        }
    }

    private fun playerPlay() {
        mThreadPool.execute {
            mPlayer?.play()
            runOnUiThread {
                lsq_sticker_play.setImageResource(R.mipmap.edit_ic_pause)
            }
        }
    }

    private fun playerPause() {
        mThreadPool.execute {
            mPlayer?.pause()
            runOnUiThread {
                lsq_sticker_play.setImageResource(R.mipmap.edit_ic_play)
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == VIDEO_REQUEST_CODE || requestCode == IMAGE_REQUEST_CODE) and (resultCode == 1)) {
            var albumBundle = data!!.getBundleExtra("select")
            var albumList = albumBundle?.getSerializable("select") as ArrayList<AlbumInfo>
            if (albumList != null) {
                runOnUiThread {
                    val item = albumList[0]
                    val isVideo = requestCode == VIDEO_REQUEST_CODE
                    if (item.type == AlbumItemType.Video){
                        lsq_sticker_view.appendVideo(albumList[0].path,0,0)
                    } else {
                        lsq_sticker_view.appendImage(albumList[0].path,0,0)
                    }
                    mThreadPool.execute {
                        initPlayer()
                        isLayerInit = true
                    }
                }
            }
        } else if ((requestCode == MATERIAL_ADD_REQUEST_CODE) and (resultCode == 1)){
            var albumBundle = data!!.getBundleExtra("select")
            var albumList = albumBundle?.getSerializable("select") as ArrayList<AlbumInfo>
            if (albumList != null){
                runOnUiThread {
                    val item = albumList[0]
                    val isVideo = requestCode == VIDEO_REQUEST_CODE
                    if (item.type == AlbumItemType.Video){
                        lsq_sticker_view.appendVideo(albumList[0].path,mCurrentDutation,0)
                    } else {
                        lsq_sticker_view.appendImage(albumList[0].path,mCurrentDutation,0)
                    }
                }
            }

        }
        else {
            if (!isLayerInit)
                finish()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun initLayer(){
        val duration = mVideoItem!!.mVideoClip.streamInfo.duration

        val videoLayer = ClipLayer(mEditor!!.context, true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(CONFIG_MAIN_LAYER, 1)
        videoLayer.setConfig(videoLayerConfig)
        if (!videoLayer.addClip(mVideoItem!!.mId.toInt(), mVideoItem!!.mVideoClip)) {
            return
        }
        mEditor.videoComposition().addLayer(11, videoLayer)

        if (mVideoItem!!.mAudioClip != null){
            val audioLayer = ClipLayer(mEditor!!.context, false)
            val audioLayerConfig = Config()
            audioLayerConfig.setNumber(CONFIG_MAIN_LAYER, 1)
            audioLayer.setConfig(audioLayerConfig)

            if (!audioLayer.addClip(mVideoItem!!.mId.toInt(), mVideoItem!!.mAudioClip)) {
                return
            }
            mEditor.audioComposition().addLayer(11, audioLayer)
            mAudioLayer = audioLayer

        }
        mVideoLayer = videoLayer
        mMaxDuration = duration
    }

    private fun initPlayer(){
        if (!isFromModel){
            if (!mEditor.build()){

            }
        }
        mPlayer = mEditor.newPlayer()
        mPlayer?.setListener { state, ts ->
            mCurrentState = state
            mPlayerContext.state = mCurrentState
            if (state == Player.State.kEOS){
                mThreadPool.execute {
                    mPlayer!!.previewFrame(0)
                    runOnUiThread {
                        lsq_sticker_play.setImageResource(R.mipmap.edit_ic_play)
                    }
                }
            } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW|| state == Player.State.kDO_SEEK){
                mPlayerContext.currentFrame = ts
                if (state == Player.State.kDO_PREVIEW){
                    runOnUiThread {
                        lsq_sticker_play.setImageResource(R.mipmap.edit_ic_play)
                    }
                }
                val currentVideoHour = ts / 3600000
                val currentVideoMinute = (ts % 3600000) / 60000

                val currentVideoSecond = (ts % 60000 / 1000)

                val durationMS = mPlayer!!.duration
                mMaxDuration = durationMS

                val durationVideoHour = durationMS / 3600000

                val durationVideoMinute = (durationMS % 3600000) / 60000

                val durationVideoSecond = (durationMS % 60000 / 1000)
                runOnUiThread {
                    if (mPlayer == null) return@runOnUiThread
                    val duration = mPlayer!!.duration
                    seekBar.max = duration.toInt()
                    lsq_sticker_view.requestShower(ts)
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

        mMaxDuration = mPlayer!!.duration

        mPlayer!!.previewFrame(0)

        runOnUiThread {
            lsq_api_displayView.attachPlayer(mPlayer)
            val duration = mPlayer!!.duration.toInt()
            seekBar.max = duration
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    if (!p2) return
                    mThreadPool.execute {
                        mPlayer?.previewFrame(p1.toLong())
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    isSeekBarTouch = true
                    playerPause()
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    isSeekBarTouch = false
                }

            })

            lsq_start_bar.setCallBack(object : DoubleHeadedDragonBar.DhdBarCallBack() {
                override fun getMaxString(value: Int): String {
                    return super.getMaxString(value)
                }

                override fun getMinString(value: Int): String {
                    return super.getMinString(value)
                }

                override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                    val max = mCurrentItemView!!.getLayerMaxDuration()
                    val itemStart = (max * minPercentage / 100).toLong()
                    val itemEnd = (max * maxPercentage / 100).toLong()
                    if (itemEnd == itemStart){
                        lsq_start_bar.minValue = (mCurrentItemViewStart / max.toFloat() * 100.0).toInt()
                        lsq_start_bar.maxValue = (mCurrentItemViewEnd / max.toFloat() * 100.0).toInt()
                        lsq_start_bar.invalidate()
                        toast("画中画素材长度不能为0")
                        return
                    }
                    lsq_debug_info.setText("当前开始时间 ${itemStart} 当前结束时间 ${itemEnd}")
                    mCurrentItemViewStart = itemStart
                    mCurrentItemViewEnd = itemEnd
                    mThreadPool?.execute {
                        mCurrentItemView?.setClipDuration(mCurrentItemViewStart,mCurrentItemViewEnd)
                        mPlayer?.previewFrame(mCurrentItemViewStart)
                    }
                }

                override fun getMinMaxString(value: Int, value1: Int): String {
                    return super.getMinMaxString(value, value1)
                }

            })
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        mThreadPool.execute {
            lsq_api_displayView.release()

            mPlayer?.close()
            TLog.e("release")
            mEditor.destroy()
            mThreadPool.shutdownNow()

        }

        LayerItemViewBase.CURRENT_LAYER_ID = 30
    }

    private fun setTimeBarValue(min : Int,max : Int) {
        lsq_start_bar.minValue = min
        lsq_start_bar.maxValue = max
        lsq_start_bar.post {
            lsq_start_bar.invalidate()
        }
    }

}