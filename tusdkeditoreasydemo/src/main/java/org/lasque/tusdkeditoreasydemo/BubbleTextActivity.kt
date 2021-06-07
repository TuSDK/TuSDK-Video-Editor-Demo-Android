/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2021/4/12$ 18:26$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import cn.bar.DoubleHeadedDragonBar
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.tusdk.pulse.Config
import com.tusdk.pulse.Engine
import com.tusdk.pulse.Player
import com.tusdk.pulse.Producer
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.EditorModel
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.VideoEditor
import kotlinx.android.synthetic.main.bubble_text_activity.*
import kotlinx.android.synthetic.main.bubble_text_activity.lsq_edit_close
import kotlinx.android.synthetic.main.bubble_text_activity.lsq_editor_cut_load
import kotlinx.android.synthetic.main.bubble_text_activity.lsq_editor_cut_load_parogress
import kotlinx.android.synthetic.main.bubble_text_activity.lsq_player_duration
import kotlinx.android.synthetic.main.bubble_text_activity.lsq_text_input
import kotlinx.android.synthetic.main.bubble_text_activity.lsq_text_input_layer
import kotlinx.android.synthetic.main.bubble_text_activity.seekBar
import kotlinx.android.synthetic.main.include_bubble_function_layout.*
import kotlinx.android.synthetic.main.include_bubble_list_layout.*
import kotlinx.android.synthetic.main.include_image_blend_layout.*
import kotlinx.android.synthetic.main.include_image_duration_layout.*
import kotlinx.android.synthetic.main.include_image_duration_layout.lsq_bar_can_touch
import kotlinx.android.synthetic.main.include_image_duration_layout.lsq_start_bar
import kotlinx.android.synthetic.main.include_image_duration_layout.lsq_start_title
import kotlinx.android.synthetic.main.include_sticker_view.*
import kotlinx.android.synthetic.main.include_title_layer.*
import kotlinx.android.synthetic.main.text_sticker_activity.*
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.*
import org.lasque.tusdkeditoreasydemo.base.views.stickers.*
import org.lasque.tusdkeditoreasydemo.utils.Constants
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.utils.DateHelper
import org.lasque.tusdkpulse.core.utils.FileHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlHelper
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.collections.ArrayList

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/4/12  18:26
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
public class BubbleTextActivity : BaseActivity(){

    private var mEditor: VideoEditor = VideoEditor()

    private var mThreadPool = Executors.newSingleThreadExecutor()

    private var mPlayer: VideoEditor.Player? = null

    private var mCurrentState = Player.State.kREADY

    private var isSeekBarTouch = false

    private var mCurrentDuration = 0L

    private var mVideoItem : VideoItem? = null

    private var mVideoLayer : ClipLayer? = null

    private var mAudioLayer : ClipLayer? = null

    private var mMaxDuration = 1L

    private var mVideoRect : Rect? = null

    private var mCurrentItemViewStart = 0L

    private var mCurrentItemViewEnd = 0L

    private var mPlayerContext : EditorPlayerContext = EditorPlayerContext(mEditor)

    private var mCurrentDutation = 0L

    private fun initLayer(){
        val duration = mVideoItem!!.mVideoClip.streamInfo.duration

        val videoLayer = ClipLayer(mEditor.context,true)
        val videoLayerConfig = Config()
        videoLayerConfig.setNumber(Layer.CONFIG_MAIN_LAYER,1)
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

        mEditor!!.build()

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
        mMaxDuration = duration
    }

    /************************************************ View *********************************************************/

    private var mCurrentItemView: BubbleLayerItemView? = null

    private var isFirstCallSoftInput = true

    private var isFromModel = false

    private var mWindowHeight = 0

    private var isNeedBackToMain = false

    private var mBlendAdapter : BlendAdapter? = null

    private var mCurrentTextIndex = -1

    private val mGlobalLayoutListener : ViewTreeObserver.OnGlobalLayoutListener? = ViewTreeObserver.OnGlobalLayoutListener {
        mVideoRect = lsq_api_displayView.getInteractionRect(800,800)
        lsq_sticker_view.resize(mVideoRect!!)
    }

    private val mKeyBoardListener : ViewTreeObserver.OnGlobalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener{
        override fun onGlobalLayout() {
            val r = Rect()
            window.decorView.getWindowVisibleDisplayFrame(r)
            val height = r.height()
            if (mWindowHeight == 0){
                mWindowHeight = height
            } else {
                if (mWindowHeight != height){
                    val softKeyboardHeight = mWindowHeight - height
                    (lsq_text_input_layer.layoutParams as ConstraintLayout.LayoutParams).setMargins(0,0,0,softKeyboardHeight)
                    lsq_text_input_layer.visibility = View.VISIBLE
                    lsq_text_input_layer.postInvalidate()
                } else {
                    lsq_text_input_layer.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bubble_text_activity)
        lsq_back.setOnClickListener {
            finish()
        }

        lsq_title.setText(FunctionType.Bubble.mTitleId)

        lsq_api_displayView.init(Engine.getInstance().mainGLContext)

        lsq_sticker_view.setCurrentLayerType(LayerType.Bubble)
        lsq_sticker_view.setEditor(mEditor)
        lsq_sticker_view.setThreadPool(mThreadPool)
        lsq_sticker_view.setPlayerContext(mPlayerContext)

        val modelPath = intent.getStringExtra(DraftItem.Draft_Path_Key)

        if (TextUtils.isEmpty(modelPath)){
            val res : Future<Boolean> = mThreadPool.submit(Callable<Boolean>() {
                val openConfig = VideoEditor.OpenConfig()
                openConfig.width = 800
                openConfig.height = 800
                val ret = mEditor.create(openConfig)
                if (!ret){
                    TLog.e("Editor Create failed")
                }
                return@Callable ret
            })

            res.get()

            openAlbum(1,false,false)
        } else {
            isFromModel = true
            val res : Future<Boolean> = mThreadPool.submit(Callable<Boolean> (){
                val editorModel = EditorModel(modelPath)
                if (!mEditor.create(editorModel)) {
                    TLog.e("Editor Create failed")
                }
                if (!mEditor.build()) {
                    TLog.e("Editor Build failed")
                }

                initPlayer()

                val textLayers = ArrayList<Int>()
                mMaxDuration = mEditor!!.videoComposition().streamInfo.duration
                val layerMaps = mEditor!!.videoComposition().allLayers
                for (key in layerMaps.keys) {
                    val currentLayer = (layerMaps[key] as ClipLayer)

                    if (currentLayer.getClip(BubbleLayerItemView.BUBBLE_CLIP_ID) != null) {
                        textLayers.add(key)
                    }
                }

                runOnUiThread {
                    Collections.sort(textLayers)
                    LayerItemViewBase.CURRENT_LAYER_ID = textLayers.last() + 1
                    for (i in textLayers) {
                        lsq_sticker_view.restoreBubble(i.toLong())
                    }
                }

                true
            })
        }

        lsq_bar_can_touch.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
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

        lsq_add_bubble.setText("添加气泡文字")
        lsq_start_title.setText("气泡起止位置")

        lsq_api_displayView.viewTreeObserver.addOnGlobalLayoutListener(mGlobalLayoutListener)

        lsq_add_bubble.setOnClickListener {
            if (isNeedBackToMain){
                backToMain()
            } else {
                playerPause()
                lsq_bubble_list_layer.visibility = View.VISIBLE
                lsq_bubble_function_layer.visibility = View.GONE
                lsq_bubble_duration_layer.visibility = View.GONE
                lsq_bubble_blend_layer.visibility =View.GONE
                isNeedBackToMain = true
                lsq_add_bubble.setText("返回")
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

        lsq_bubble_play.setOnClickListener {
            if (mCurrentState != Player.State.kPLAYING){
                playerPlay()
            } else {
                playerPause()
            }
        }

        val blendModes = mutableListOf<String>(
                Layer.BLEND_MODE_Default,
                Layer.BLEND_MODE_Normal,
                Layer.BLEND_MODE_Overlay,
                Layer.BLEND_MODE_Add,
                Layer.BLEND_MODE_Subtract,
                Layer.BLEND_MODE_Negation,
                Layer.BLEND_MODE_Average,
                Layer.BLEND_MODE_Multiply,
                Layer.BLEND_MODE_Difference,
                Layer.BLEND_MODE_Screen,
                Layer.BLEND_MODE_Softlight,
                Layer.BLEND_MODE_Hardlight,
                Layer.BLEND_MODE_Linearlight,
                Layer.BLEND_MODE_Pinlight,
                Layer.BLEND_MODE_Lighten,
                Layer.BLEND_MODE_Darken,
                Layer.BLEND_MODE_Exclusion
        )

        val blendAdapter = BlendAdapter(blendModes,this)
        blendAdapter.setOnItemClickListener(object : OnItemClickListener<String,BlendAdapter.BlendViewHolder>{
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

        val sp = TuSdkContext.context().getSharedPreferences("TU-TTF", Context.MODE_PRIVATE)

        val b5 = BubbleItem(sp.getString(Constants.BUBBLE_5,"")!!,"message","bubble_5")
        val b6 = BubbleItem(sp.getString(Constants.BUBBLE_6,"")!!,"带劲","bubble_6")
        val b7 = BubbleItem(sp.getString(Constants.BUBBLE_7,"")!!,"快乐水","bubble_7")

        val bubbleItemAdapter = BubbleItemAdapter(Arrays.asList(b5,b6,b7),this)
        bubbleItemAdapter.setOnItemClickListener(object : OnItemClickListener<BubbleItem, BubbleItemAdapter.BubbleItemViewHolder> {
            override fun onItemClick(pos: Int, holder: BubbleItemAdapter.BubbleItemViewHolder, item: BubbleItem) {
                lsq_sticker_view.appendBubble(item.path,mCurrentDuration,mMaxDuration)
            }
        })

        val bubbleLayoutManager = LinearLayoutManager(this)
        bubbleLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        lsq_bubble_item_list.layoutManager = bubbleLayoutManager
        lsq_bubble_item_list.adapter = bubbleItemAdapter


        lsq_bubble_duration_btn.setOnClickListener {
            isNeedBackToMain = true
            lsq_bubble_function_layer.visibility = View.GONE
            lsq_bubble_duration_layer.visibility = View.VISIBLE
            lsq_add_bubble.setText("返回首页")
        }

        lsq_bubble_blend_btn.setOnClickListener {
            isNeedBackToMain = true
            lsq_blend_mix_bar.progress = (mCurrentItemView!!.getBlendMix() * 100).toInt()
            lsq_bubble_function_layer.visibility = View.GONE
            lsq_bubble_blend_layer.visibility = View.VISIBLE
            lsq_add_bubble.setText("返回首页")
        }

        lsq_text_input.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val s: String = s.toString()
                mCurrentItemView?.updateText(mCurrentTextIndex,s)
            }

        })

        lsq_edit_close.setOnClickListener {
            hideSoftInput()
            lsq_text_input_layer.clearFocus()
            lsq_text_input_layer.visibility = View.GONE
        }

        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(mKeyBoardListener)

    }

    private fun initPlayer() {
        if (!isFromModel){
            if (!mEditor.build()){

            }
        }

        mPlayer = mEditor.newPlayer()
        mPlayer?.setListener { state, ts ->
            mCurrentState = state
            mPlayerContext.state = mCurrentState
            if (state == Player.State.kDO_PLAY || state == Player.State.kDO_PAUSE){
                val duration = mPlayer!!.duration
                runOnUiThread {

                    mMaxDuration = duration
                    seekBar.max = duration.toInt()
                }
            } else if (state == Player.State.kEOS){
                mThreadPool.execute {
                    mPlayer!!.previewFrame(0)
                    runOnUiThread {
                        lsq_bubble_play.setImageResource(R.mipmap.edit_ic_play)
                    }
                }
            } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW){
                mCurrentDutation = ts
                mPlayerContext.currentFrame = ts
                val currentVideoHour = ts / 3600000
                val currentVideoMinute = (ts % 3600000) / 60000

                val currentVideoSecond = (ts % 60000 / 1000)

                val durationMS = mPlayer!!.duration
                val durationVideoHour = durationMS / 3600000

                val durationVideoMinute = (durationMS % 3600000) / 60000

                val durationVideoSecond = (durationMS % 3600000 / 1000)

                runOnUiThread {
                    lsq_sticker_view.requestShower(ts)
                    if (!isSeekBarTouch){
                        seekBar.progress = ts.toInt()
                    }
                    lsq_player_duration.text =  "${currentVideoHour}:$currentVideoMinute:$currentVideoSecond/$durationVideoHour:$durationVideoMinute:$durationVideoSecond"
                }
            }
        }

        if (!mPlayer!!.open()){
            TLog.e("Editor Player Open failed")
        }

        mPlayer!!.previewFrame(0)

        runOnUiThread {
            lsq_api_displayView.attachPlayer(mPlayer)
            val duration = mPlayer!!.duration.toInt()
            seekBar.max = duration
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

            lsq_start_bar.setCallBack(object : DoubleHeadedDragonBar.DhdBarCallBack(){
                override fun onEndTouch(minPercentage: Float, maxPercentage: Float) {
                    mCurrentItemViewStart = (mMaxDuration * minPercentage / 100).toLong()
                    mCurrentItemViewEnd = (mMaxDuration * maxPercentage / 100).toLong()
                    if (mCurrentItemViewEnd - mCurrentItemViewStart < 100) {
                        mCurrentItemViewEnd = mCurrentItemViewStart + 100
                    }
                    mThreadPool?.execute {
                        mCurrentItemView?.setClipDuration(mCurrentItemViewStart, mCurrentItemViewEnd)
                        mPlayer?.previewFrame(mCurrentItemViewStart)
//                        mPlayerContext.refreshFrame()
                    }
                }
            })

            lsq_sticker_view.setLayerViewDelegate(object : LayerViewDelegate{
                override fun onItemViewSelected(type: LayerType, view: LayerItemViewBase) {
                    playerPause()
                    mCurrentItemView = view as BubbleLayerItemView
                    var startPos = (mCurrentItemView!!.getLayerStartPos() / mMaxDuration.toDouble()) * 100
                    var endPos = (mCurrentItemView!!.getLayerEndPos() / mMaxDuration.toDouble()) * 100
                    lsq_bar_can_touch.isClickable = false
                    setTimeBarValue(startPos.toInt(), endPos.toInt())
                    val blendMode = mCurrentItemView!!.getBlendMode()
                    runOnUiThread {
                        lsq_bubble_duration_btn.visibility = View.VISIBLE
                        lsq_bubble_blend_btn.visibility = View.VISIBLE
                        lsq_layer_start_bar.progress = mCurrentItemView!!.getLayerStartPos().toInt()
                        mBlendAdapter?.findMode(blendMode)
                    }
                }

                override fun onItemViewReleased(type: LayerType, view: LayerItemViewBase) {

                }

                override fun onItemUnselected(type: LayerType) {
                    runOnUiThread {
                        hideSoftInput()
                        lsq_text_input_layer.clearFocus()
                        lsq_text_input_layer.visibility = View.GONE
                        mCurrentItemView = null
                        mCurrentTextIndex = -1
                        setTimeBarValue(0,0)
                        lsq_bar_can_touch.isClickable = true
                        lsq_bubble_duration_btn.visibility = View.GONE
                        lsq_bubble_blend_btn.visibility = View.GONE
                        backToMain()
                    }
                }

            })

            lsq_sticker_view.setBubbleLayerViewDelegate(object : BubbleLayerViewDelegate{
                override fun onBubbleUpdateText(view: BubbleLayerItemView, textIndex: Int) {
                    runOnUiThread {
                        mCurrentTextIndex = textIndex
                        val currentText = mCurrentItemView!!.getTextByIndex(textIndex)
                        lsq_text_input.setText(currentText)
//                        lsq_text_input_layer.visibility = View.VISIBLE
                        lsq_text_input.requestFocus()
                        showSoftInput()
                    }
                }

            })


        }



    }

    fun backToMain(){
        lsq_bubble_function_layer.visibility = View.VISIBLE
        lsq_bubble_duration_layer.visibility = View.GONE
        lsq_bubble_blend_layer.visibility = View.GONE
        lsq_bubble_list_layer.visibility = View.GONE
        isNeedBackToMain = false
        lsq_add_bubble.setText("添加气泡文字")
    }

    /** 显示软键盘  */
    private fun showSoftInput() {
        var view: View? = getCurrentFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(InputMethodManager::class.java).showSoftInput(view, 0)
        } else {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(view, 0)
        }
    }

    /** 隐藏软键盘  */
    private fun hideSoftInput() {
        var view: View? = getCurrentFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(view?.windowToken, 0)
        } else {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view?.windowToken, 0)

        }
    }

    private fun setTimeBarValue(min: Int, max: Int) {
        lsq_start_bar.minValue = min
        lsq_start_bar.maxValue = max
        lsq_start_bar.invalidate()
    }

    private fun playerPlay() {
        mThreadPool.execute {
            if (mPlayer != null){
                if (mPlayer!!.play()) {
                    runOnUiThread {
                        lsq_bubble_play.setImageResource(R.mipmap.edit_ic_pause)
                    }
                }
                runOnUiThread {
                    lsq_sticker_view.cancelAllItemSelected()
                }
            }

        }
    }

    private fun playerPause() {
        mThreadPool.execute {
            if (mPlayer != null){
                if (mPlayer!!.pause()) {
                    runOnUiThread {
                        lsq_bubble_play.setImageResource(R.mipmap.edit_ic_play)
                    }
                }
            }
        }

    }

    private var mCurrentProducer : VideoEditor.Producer? = null

    private var mCurrentSavePath = ""

    private var isNeedSave = true

    private fun saveVideo() {
        mThreadPool.execute {
            isNeedSave = true
            val producer = mEditor.newProducer()
            //                val outputFilePath = "${TuSdkContext.context().getExternalFilesDir(DIRECTORY_DCIM)!!.absolutePath}/editor_output${System.currentTimeMillis()}.mp4"

            val outputFilePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath}/editor_output${System.currentTimeMillis()}.mp4"

            mCurrentSavePath = outputFilePath
            val config = Producer.OutputConfig()
            config.watermark = BitmapHelper.getRawBitmap(this, R.raw.sample_watermark)
            config.watermarkPosition = 1
            producer.setOutputConfig(config)
            producer.setListener { state, ts ->
                if (state == Producer.State.kEND) {
                    mThreadPool.execute {
                        producer.release()
                        mEditor!!.resetProducer()
                        mPlayer!!.seekTo(mCurrentDuration)
                        mCurrentProducer = null
                    }
                    val contentValue = ImageSqlHelper.getCommonContentValues()
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(outputFilePath))))
                    runOnUiThread {
                        setEnable(true)
                        lsq_editor_cut_load.setVisibility(View.GONE)
                        lsq_editor_cut_load_parogress.setValue(0f)
                        if (isNeedSave)
                            Toast.makeText(applicationContext, "保存成功", Toast.LENGTH_SHORT).show()
                    }
                } else if (state == Producer.State.kWRITING) {
                    val duration = producer.duration
                    runOnUiThread {
                        lsq_editor_cut_load.setVisibility(View.VISIBLE)
                        lsq_editor_cut_load_parogress.setValue((ts / duration.toFloat()) * 100f)
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

            mCurrentProducer = producer
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

            val currentModule = FunctionType.Bubble

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


    override fun onResume() {
        super.onResume()
        if (!Engine.getInstance().checkEGL()){
            throw Exception("dsssssssssssssssss");
        }
    }

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

    override fun onStop() {
        super.onStop()
        playerPause()

        if (mCurrentProducer != null){
            mThreadPool.execute {
                isNeedSave = false
                mCurrentProducer?.cancel()
                FileHelper.delete(File(mCurrentSavePath))
                mCurrentProducer = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(mKeyBoardListener)
        mThreadPool.execute {
            lsq_api_displayView.release()

            mPlayer?.close()
            TLog.e("release")
            mEditor.destroy()
            mThreadPool.shutdownNow()

        }
    }
}