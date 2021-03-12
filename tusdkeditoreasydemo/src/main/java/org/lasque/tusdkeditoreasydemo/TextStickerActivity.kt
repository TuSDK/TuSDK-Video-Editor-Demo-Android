/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/11/19$ 10:15$
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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
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
import com.tusdk.pulse.editor.clips.Text2DClip
import kotlinx.android.synthetic.main.include_sticker_view.*
import kotlinx.android.synthetic.main.include_title_layer.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_align.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_alpha.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_array.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_background.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_blend.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_color.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_color.view.lsq_editor_component_text_font_back
import kotlinx.android.synthetic.main.lsq_editor_component_text_spacing.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_stroke.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_style.view.*
import kotlinx.android.synthetic.main.text_sticker_activity.*
import kotlinx.android.synthetic.main.text_sticker_activity.lsq_editor_cut_load
import kotlinx.android.synthetic.main.text_sticker_activity.lsq_editor_cut_load_parogress
import kotlinx.android.synthetic.main.text_sticker_activity.lsq_start_bar
import kotlinx.android.synthetic.main.text_sticker_activity.lsq_sticker_play
import org.lasque.tusdkpulse.core.utils.DateHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlHelper
import org.lasque.tusdkpulse.impl.view.widget.TuSeekBar
import org.lasque.tusdkeditoreasydemo.TextStickerActivity.Companion.TextFunction.*
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.*
import org.lasque.tusdkeditoreasydemo.base.views.ColorView
import org.lasque.tusdkeditoreasydemo.base.views.stickers.LayerItemViewBase
import org.lasque.tusdkeditoreasydemo.base.views.stickers.LayerItemViewBase.Companion.CURRENT_LAYER_ID
import org.lasque.tusdkeditoreasydemo.base.views.stickers.LayerType
import org.lasque.tusdkeditoreasydemo.base.views.stickers.LayerViewDelegate
import org.lasque.tusdkeditoreasydemo.base.views.stickers.TextLayerItemView
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.collections.ArrayList

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/19  10:15
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class TextStickerActivity : BaseActivity() {

    companion object {
        const val MAX_STROKE_WIDTH = 2

        enum class TextFunction(iconId: Int, textId: Int) {
            ADD(R.drawable.edit_ic_add, R.string.lsq_editor_text_add),
            COLOR(R.drawable.edit_ic_colour, R.string.lsq_editor_text_color),
            ALPHA(R.drawable.t_ic_transparency, R.string.lsq_editor_text_transparency),
            STROKE(R.drawable.t_ic_stroke, R.string.lsq_editor_text_stroke),
            BACKGROUND(R.drawable.t_ic_bg, R.string.lsq_editor_text_background),
            SPACE(R.drawable.t_ic_space, R.string.lsq_editor_text_space),
            ALIGN(R.drawable.t_ic_align, R.string.lsq_editor_text_align),
            ARRAY(R.drawable.t_ic_array, R.string.lsq_editor_text_array),
            STYLE(R.drawable.edit_ic_style, R.string.lsq_editor_text_style),
            FONT(R.drawable.edit_ic_font, R.string.lsq_editor_text_font),
            BLEND(R.drawable.t_ic_transparency, R.string.lsq_editor_text_blend),
            ;

            public val iconId = iconId
            public val textId = textId
        }
    }

    /******************************************** Editor **********************************************************/

    private var mEditor: VideoEditor = VideoEditor()

    private var mThreadPool = Executors.newSingleThreadExecutor()

    private var mPlayer: VideoEditor.Player? = null

    private var mCurrentState = Player.State.kREADY

    private var isSeekBarTouch = false

    private var mCurrentDutation = 0L

    private var mVideoItem: VideoItem? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mMaxDuration = 1L

    private var mVideoRect: Rect? = null

    private var mCurrentItemViewStart: Long = 0

    private var mCurrentItemViewEnd: Long = 0

    private var mPlayerContext: EditorPlayerContext = EditorPlayerContext(mEditor)

    private fun initLayer() {
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

        mEditor!!.build()

        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
        mMaxDuration = duration
    }

    /************************************************ View *********************************************************/

    private val mTextFunctions: MutableList<TextFunction> = mutableListOf(
            ADD, COLOR, ALPHA, STROKE, BACKGROUND, SPACE, ALIGN, ARRAY, STYLE, FONT, BLEND
    )

    private var mCurrentItemView: TextLayerItemView? = null

    private var mColorOption: TextColorOption? = null

    private var mAlphaOption: TextAlphaOption? = null

    private var mStrokeOption: TextStrokeOption? = null

    private var mBackgroundOption: TextBackgroundOption? = null

    private var mSpacingOption: TextSpacingOption? = null

    private var mAlignOption: TextAlignOption? = null

    private var mArrayOption: TextArrayOption? = null

    private var mStyleOption: TextStyleOption? = null

    private var mFontOption: TextFontOption? = null

    private var mBlendOption: TextBlendOption? = null

    private var isFirstCallSoftInput = true

    private var isFromModel = false

    private var mWindowHeight = 0

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
                    lsq_text_input_layer.invalidate()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.text_sticker_activity)
        lsq_back.setOnClickListener {
            finish()
        }

        lsq_title.setText(FunctionType.Text.mTitleId)

        lsq_api_displayView.init(Engine.getInstance().mainGLContext)

        val modelPath = intent.getStringExtra(DraftItem.Draft_Path_Key)

        if (TextUtils.isEmpty(modelPath)) {
            mThreadPool.execute {
                val openConfig = VideoEditor.OpenConfig()
                openConfig.width = 800
                openConfig.height = 800
                if (!mEditor.create(openConfig)) {
                    TLog.e("Editor Create failed")
                }
            }

            openAlbum(1, false, false)
        } else {
            isFromModel = true
            mThreadPool.execute {
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

                    if (currentLayer.getClip(TextLayerItemView.TEXT_CLIP_ID) != null) {
                        textLayers.add(key)
                    }
                }

                runOnUiThread {
                    Collections.sort(textLayers)
                    LayerItemViewBase.CURRENT_LAYER_ID = textLayers.last() + 1
                    for (i in textLayers) {
                        lsq_sticker_view.restoreText(i.toLong())
                    }
                }
            }
        }


        lsq_bar_can_touch.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                return lsq_bar_can_touch.isClickable
            }

        })

        setTimeBarValue(0, 0)

        lsq_editor_play.setOnClickListener {
            if (mCurrentState != Player.State.kPLAYING) {
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
        lsq_sticker_view.setEditor(mEditor)
        lsq_sticker_view.setThreadPool(mThreadPool)
        lsq_sticker_view.setPlayerContext(mPlayerContext)
        lsq_sticker_view.setCurrentLayerType(LayerType.Text)

        if (isFromModel) {

        }

        lsq_api_displayView.viewTreeObserver.addOnGlobalLayoutListener {
            mVideoRect = lsq_api_displayView.getInteractionRect(800, 800)
            lsq_sticker_view.resize(mVideoRect!!)
            TLog.e("Video rect $mVideoRect")
        }

        lsq_text_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val s: String = p0.toString()
                mThreadPool.execute {
                    mCurrentItemView?.updateText(s)
                }
            }

        })

        val textFunctionAdapter: TextFunctionAdapter = TextFunctionAdapter(mTextFunctions, this)
        textFunctionAdapter.setOnItemClickListener(object : OnItemClickListener<TextFunction, TextFunctionAdapter.TextFunctionViewHolder> {
            override fun onItemClick(pos: Int, holder: TextFunctionAdapter.TextFunctionViewHolder, item: TextFunction) {
                if (item == ADD) {
                    playerPause()
                    mThreadPool.execute {
                        runOnUiThread {
                            lsq_sticker_view.appendText("", mCurrentDutation, mMaxDuration)
                        }
                    }
                } else {
                    if (mCurrentItemView == null) {
                        return
                    }
                    lsq_text_options_list_panel.visibility = View.INVISIBLE
                    lsq_text_options_view_panel.visibility = View.VISIBLE
                    lsq_text_options_view_panel.removeAllViews()
                    lsq_text_options_view_panel.addView(getOptionView(item))
                }
            }
        })
        val linearManger = LinearLayoutManager(this)
        linearManger.orientation = LinearLayoutManager.HORIZONTAL
        lsq_text_function_option_list.layoutManager = linearManger
        lsq_text_function_option_list.adapter = textFunctionAdapter

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

        lsq_edit_close.setOnClickListener {
            hideSoftInput()
            lsq_text_input_layer.clearFocus()
            lsq_text_input_layer.visibility = View.GONE
        }

        lsq_sticker_play.setOnClickListener {
            if (mCurrentState != Player.State.kPLAYING) {
                playerPlay()
            } else {
                playerPause()
            }
        }

        lsq_start_bar.isClickable = false

        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(mKeyBoardListener)
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

            val currentModule = FunctionType.Text

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
                    positiveButton(text = "确定"){dialog ->
                        dialog.dismiss()
                    }

                    cancelOnTouchOutside(true)
                }
            }
        }
    }


    private fun saveVideo() {
        mThreadPool.execute {
            val producer = mEditor.newProducer()
            //                val outputFilePath = "${TuSdkContext.context().getExternalFilesDir(DIRECTORY_DCIM)!!.absolutePath}/editor_output${System.currentTimeMillis()}.mp4"

            val outputFilePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath}/editor_output${System.currentTimeMillis()}.mp4"
            TLog.e("output path $outputFilePath")

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

    /** 隐藏软键盘  */
    private fun hideSoftInput() {
        var view: View? = getCurrentFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(view?.windowToken, 0)
        } else {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view?.windowToken, 0)

        }
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

    private fun playerPlay() {
        mThreadPool.execute {
            if (mPlayer!!.play()) {
                runOnUiThread {
                    lsq_sticker_play.setImageResource(R.mipmap.edit_ic_pause)
                }
            }
            runOnUiThread {
                lsq_sticker_view.cancelAllItemSelected()
            }
        }
    }

    private fun playerPause() {
        mThreadPool.execute {
            if (mPlayer!!.pause()) {
                runOnUiThread {
                    lsq_sticker_play.setImageResource(R.mipmap.edit_ic_play)
                }
            }
        }
    }

    private fun getOptionView(function: TextFunction): View? {
        return when (function) {
            ADD -> {
                null
            }
            COLOR -> {
                if (mColorOption == null) {
                    mColorOption = TextColorOption()
                }
                mColorOption?.getOptionView()
            }
            ALPHA -> {
                if (mAlphaOption == null) {
                    mAlphaOption = TextAlphaOption()
                }
                mAlphaOption?.getOptionView()
            }
            STROKE -> {
                if (mStrokeOption == null) {
                    mStrokeOption = TextStrokeOption()
                }
                mStrokeOption?.getOptionView()
            }
            BACKGROUND -> {
                if (mBackgroundOption == null) {
                    mBackgroundOption = TextBackgroundOption()
                }
                mBackgroundOption?.getOptionView()
            }
            SPACE -> {
                if (mSpacingOption == null) {
                    mSpacingOption = TextSpacingOption()
                }
                mSpacingOption?.getOptionView()
            }
            ALIGN -> {
                if (mAlignOption == null) {
                    mAlignOption = TextAlignOption()
                }
                mAlignOption?.getOptionView()
            }
            ARRAY -> {
                if (mArrayOption == null) {
                    mArrayOption = TextArrayOption()
                }
                mArrayOption?.getOptionView()
            }
            STYLE -> {
                if (mStyleOption == null) {
                    mStyleOption = TextStyleOption()
                }
                mStyleOption?.getOptionView()
            }
            FONT -> {
                if (mFontOption == null) {
                    mFontOption = TextFontOption()
                }
                mFontOption?.getOptionView()
            }
            BLEND -> {
                if (mBlendOption == null) {
                    mBlendOption = TextBlendOption()
                }
                mBlendOption?.getOptionView()
            }
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
                    mVideoItem = VideoItem.createVideoItem(item.path, mEditor, true, item.type == AlbumItemType.Video)
                    initLayer()
                    initPlayer()
                }
            }
        } else {
            finish()
        }
    }

    private fun initPlayer() {
        if (!isFromModel) {
            if (!mEditor.build()) {

            }
        }
        mPlayer = mEditor.newPlayer()
        mPlayer?.setListener { state, ts ->
            mCurrentState = state
            mPlayerContext.state = mCurrentState
            if (state == Player.State.kDO_PLAY || state == Player.State.kDO_PAUSE) {
                runOnUiThread {
                    val duration = mPlayer!!.duration
                    mMaxDuration = duration
                    seekBar.max = duration.toInt()
                }
            } else if (state == Player.State.kEOS) {
                mThreadPool.execute {
                    mPlayer!!.previewFrame(0)
                    runOnUiThread {
                        lsq_sticker_play.setImageResource(R.mipmap.edit_ic_play)
                    }
                }
            } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW) {
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
                    if (!isSeekBarTouch) {
                        seekBar.progress = ts.toInt()
                    }
                    lsq_player_duration.text = "${currentVideoHour}:$currentVideoMinute:$currentVideoSecond/$durationVideoHour:$durationVideoMinute:$durationVideoSecond"
                }
            }
        }

        if (!mPlayer!!.open()) {
            TLog.e("Editor Player Open failed")
        }

        mPlayer!!.previewFrame(0)

        runOnUiThread {
            lsq_api_displayView.attachPlayer(mPlayer)
            val duration = mPlayer!!.duration.toInt()
            seekBar.max = duration
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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

                override fun getMinMaxString(value: Int, value1: Int): String {
                    return super.getMinMaxString(value, value1)
                }

            })

            lsq_sticker_view.setLayerViewDelegate(object : LayerViewDelegate {
                override fun onItemViewSelected(type: LayerType, view: LayerItemViewBase) {
                    playerPause()
                    if (mCurrentItemView != view) isFirstCallSoftInput = true
                    mCurrentItemView = view as TextLayerItemView
                    var startPos = (mCurrentItemView!!.getLayerStartPos() / mMaxDuration.toDouble()) * 100
                    var endPos = (mCurrentItemView!!.getLayerEndPos() / mMaxDuration.toDouble()) * 100
                    lsq_bar_can_touch.isClickable = false
                    setTimeBarValue(startPos.toInt(), endPos.toInt())
                    mColorOption?.findColor(mCurrentItemView!!.getTextColor())
                    mBlendOption?.findBlendMode(mCurrentItemView!!.getBlendMode())
                    mBlendOption?.setBlendMix(mCurrentItemView!!.getBlendMix())
                    mArrayOption?.setTextReverse(mCurrentItemView!!.isTextReverse())
                    mBackgroundOption?.findColor(mCurrentItemView!!.getBackgroundColor())
                    mStyleOption?.updateUnderline(mCurrentItemView!!.isUnderLine())
                    lsq_text_input.setText(mCurrentItemView!!.getCurrentText())
                }

                override fun onItemViewReleased(type: LayerType, view: LayerItemViewBase) {
                    val currentText = mCurrentItemView!!.getCurrentText()
                    lsq_text_input.setText(currentText)
                    if (isFirstCallSoftInput) {
                        isFirstCallSoftInput = false
                    } else {
                        lsq_text_input_layer.visibility = View.VISIBLE
                        lsq_text_input.requestFocus()
                        showSoftInput()
                    }
                }

                override fun onItemUnselected(type: LayerType) {
                    runOnUiThread {
                        hideSoftInput()
                        lsq_text_input_layer.clearFocus()
                        lsq_text_input_layer.visibility = View.GONE
                        mCurrentItemView = null
                        setTimeBarValue(0, 0)
                        lsq_bar_can_touch.isClickable = true
                        isFirstCallSoftInput = true
                        backToMain()
                    }
                }

            })
        }


    }

    private fun setTimeBarValue(min: Int, max: Int) {
        lsq_start_bar.minValue = min
        lsq_start_bar.maxValue = max
        lsq_start_bar.invalidate()
    }

    private fun backToMain() {
        lsq_text_options_list_panel.visibility = View.VISIBLE
        lsq_text_options_view_panel.visibility = View.INVISIBLE
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

    inner class TextColorOption() {
        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_color, null)

        init {
            mOptionView.lsq_editor_component_text_font_back.setOnClickListener {
                backToMain()
            }
            mOptionView.lsq_editor_text_color_seek_font.setOnColorChangeListener(object : ColorView.OnColorChangeListener {
                override fun changeColor(colorId: Int) {
                    mCurrentItemView?.updateFontColor(colorId)
                }

                override fun changePosition(percent: Float) {

                }

            })
            if (mCurrentItemView != null)
                mOptionView.lsq_editor_text_color_seek_font.findColorInt(mCurrentItemView!!.getTextColor())
        }

        public fun getOptionView(): View {
            return mOptionView
        }

        public fun findColor(color: Int) {
            mOptionView.lsq_editor_text_color_seek_font.findColorInt(color)
            mOptionView.lsq_editor_text_color_seek_font.postInvalidate()
        }
    }

    inner class TextAlphaOption {
        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_alpha, null)

        init {
            mOptionView.lsq_editor_component_text_alpha_back.setOnClickListener {
                backToMain()
            }
            (mOptionView.lsq_editor_text_alpha_seek_font as TuSeekBar).setDelegate { seekBar, progress ->
                mCurrentItemView?.updateTextAlpha(progress.toDouble())
            }
        }

        public fun getOptionView(): View {
            (mOptionView.lsq_editor_text_alpha_seek_font as TuSeekBar).progress = if (mCurrentItemView == null) {
                1.0f
            } else {
                mCurrentItemView!!.getTextAlpha().toFloat()
            }
            return mOptionView
        }
    }

    inner class TextStrokeOption {

        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_stroke, null)

        init {
            mOptionView.lsq_editor_component_text_stroke_back.setOnClickListener {
                backToMain()
            }
            (mOptionView.lsq_editor_text_stroke_alpha_seek as TuSeekBar).setDelegate { seekBar, progress ->
                mCurrentItemView?.updateStrokeWidth((MAX_STROKE_WIDTH * progress).toDouble())
            }
            val colorView = mOptionView.lsq_editor_text_stroke_color
            colorView.setCircleRadius(8)
            colorView.setOnColorChangeListener(object : ColorView.OnColorChangeListener {
                override fun changeColor(colorId: Int) {
                    mCurrentItemView?.updateStrokeColor(colorId)
                }

                override fun changePosition(percent: Float) {

                }

            })
        }

        public fun getOptionView(): View {
            return mOptionView
        }
    }

    inner class TextBackgroundOption {
        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_background, null)

        init {
            mOptionView.lsq_editor_component_text_bg_back.setOnClickListener {
                backToMain()
            }
            val alphaSeek: TuSeekBar = mOptionView.lsq_editor_text_bg_seek as TuSeekBar
            alphaSeek.setDelegate { seekBar, progress ->
                mCurrentItemView?.updateBackgroundAlpha((255 * progress).toInt())
            }
            val colorView = mOptionView.lsq_editor_text_bg_color
            colorView.setCircleRadius(8)
            colorView.setOnColorChangeListener(object : ColorView.OnColorChangeListener {
                override fun changeColor(colorId: Int) {
                    mCurrentItemView?.updateBackgroundColor(colorId)
                }

                override fun changePosition(percent: Float) {
                    if (percent > 0){
                        alphaSeek.enabled = true
                        alphaSeek.progress = 1f
                    } else {
                        alphaSeek.enabled = false
                        alphaSeek.progress = 0f
                    }

                }
            })
            if (mCurrentItemView != null){
                colorView.findColorInt(mCurrentItemView!!.getBackgroundColor())
            }

        }

        public fun getOptionView(): View {
            return mOptionView
        }

        public fun findColor(color : Int){
            if (color == Color.TRANSPARENT){
                (mOptionView.lsq_editor_text_bg_seek as TuSeekBar).enabled = false
            }
            val alpha = Color.alpha(color)
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            mOptionView.lsq_editor_text_bg_color.findColorInt(color)
            mOptionView.lsq_editor_text_bg_color.postInvalidate()
            (mOptionView.lsq_editor_text_bg_seek as TuSeekBar).progress = (alpha / 255f).toFloat()

        }


    }

    inner class TextSpacingOption {
        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_spacing, null)

        init {
            mOptionView.lsq_editor_component_text_spacing_back.setOnClickListener {
                backToMain()
            }
            (mOptionView.lsq_editor_text_row_seek as TuSeekBar).setDelegate { seekBar, progress ->
                mCurrentItemView?.updateLineSpacing(progress.toDouble())
            }
            (mOptionView.lsq_editor_text_word_seek as TuSeekBar).setDelegate { seekBar, progress ->
                mCurrentItemView?.updateWordSpacing(progress.toDouble())
            }
        }

        public fun getOptionView(): View {
            return mOptionView
        }
    }

    inner class TextAlignOption {
        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_align, null)

        init {
            mOptionView.lsq_editor_component_text_align_back.setOnClickListener {
                backToMain()
            }
            mOptionView.lsq_editor_text_leftalignment.setOnClickListener {
                mThreadPool.execute {
                    mCurrentItemView?.updateTextAlign(Text2DClip.Alignment.LEFT)
                }
            }
            mOptionView.lsq_editor_text_centeralignment.setOnClickListener {
                mThreadPool.execute {
                    mCurrentItemView?.updateTextAlign(Text2DClip.Alignment.CENTER)

                }
            }
            mOptionView.lsq_editor_text_rightalignment.setOnClickListener {
                mThreadPool.execute {
                    mCurrentItemView?.updateTextAlign(Text2DClip.Alignment.RIGHT)

                }
            }
        }

        public fun getOptionView(): View {
            return mOptionView
        }
    }

    inner class TextArrayOption {
        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_array, null)

        private var isLeft = true

        init {
            mOptionView.lsq_editor_component_text_array_back.setOnClickListener {
                backToMain()
            }
            val lIcon = mOptionView.lsq_btn_left_to_right
            lIcon.setDefaultColor(Color.WHITE)
            lIcon.setSelectedColor(Color.parseColor("#f4a11a"))

            val rIcon = mOptionView.lsq_btn_right_to_left
            rIcon.setDefaultColor(Color.WHITE)
            rIcon.setSelectedColor(Color.parseColor("#f4a11a"))

            lIcon.isSelected = true
            rIcon.isSelected = false
            if (mCurrentItemView != null){
                if (mCurrentItemView!!.isTextReverse()){
                    lIcon.isSelected = true
                    rIcon.isSelected = false
                }
            }


            mOptionView.lsq_editor_text_lefttoright.setOnClickListener {
                if (!mCurrentItemView!!.isTextReverse()) {
                    mThreadPool.execute {
                        mCurrentItemView?.textReverse(true)
                    }
                    lIcon.isSelected = true
                    rIcon.isSelected = false
                }
            }

            mOptionView.lsq_editor_text_righttoleft.setOnClickListener {
                if (mCurrentItemView!!.isTextReverse()) {
                    isLeft = false
                    mThreadPool.execute {
                        mCurrentItemView?.textReverse(false)
                    }
                    lIcon.isSelected = false
                    rIcon.isSelected = true
                }
            }
        }

        public fun getOptionView(): View {
            return mOptionView
        }

        public fun setTextReverse(isLeft : Boolean){
            this.isLeft = isLeft
            val lIcon = mOptionView.lsq_btn_left_to_right
            val rIcon = mOptionView.lsq_btn_right_to_left

            if (isLeft){
                lIcon.isSelected = true
                rIcon.isSelected = false
            } else {
                lIcon.isSelected = false
                rIcon.isSelected = true
            }
        }
    }

    inner class TextStyleOption {
        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_style, null)

        private var isUnderLine = false

        init {
            mOptionView.lsq_editor_component_text_font_back.setOnClickListener {
                backToMain()
            }
            mOptionView.lsq_editor_text_underline.setOnClickListener {
                isUnderLine = !isUnderLine
                var res : Future<Boolean> = mThreadPool.submit(Callable<Boolean> {
                    mCurrentItemView?.updateTextStyle(if (isUnderLine) {
                        Text2DClip.Style.UNDERLINE
                    } else {
                        Text2DClip.Style.NORMAL
                    })

                    true
                })
                res.get()
                updateUnderline(isUnderLine)
            }

            mOptionView.lsq_editor_text_normal.setOnClickListener {
                mCurrentItemView?.updateTextStyle(Text2DClip.Style.NORMAL)
                (mOptionView.lsq_editor_text_underline.getChildAt(0) as ImageView).setImageResource(R.drawable.t_ic_underline_nor)
                (mOptionView.lsq_editor_text_underline.getChildAt(1) as TextView).setTextColor(Color.WHITE)
            }
        }

        public fun updateUnderline(isunderline : Boolean) {
            isUnderLine = isunderline
            if (isunderline) {
                (mOptionView.lsq_editor_text_underline.getChildAt(0) as ImageView).setImageResource(R.drawable.t_ic_underline_sel)
                (mOptionView.lsq_editor_text_underline.getChildAt(1) as TextView).setTextColor(Color.parseColor("#f6a623"))
            } else {
                (mOptionView.lsq_editor_text_underline.getChildAt(0) as ImageView).setImageResource(R.drawable.t_ic_underline_nor)
                (mOptionView.lsq_editor_text_underline.getChildAt(1) as TextView).setTextColor(Color.WHITE)
            }
        }

        public fun getOptionView(): View {
            return mOptionView
        }


    }

    inner class TextFontOption {
        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_font, null)

        init {
            mOptionView.lsq_editor_component_text_font_back.setOnClickListener {
                backToMain()
            }
        }

        public fun getOptionView(): View {
            return mOptionView
        }
    }

    inner class TextBlendOption {
        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_blend, null)

        private var mBlendAdapter: BlendAdapter? = null

        init {
            mOptionView.lsq_editor_component_text_font_back.setOnClickListener {
                backToMain()
            }
            val blendModes = mutableListOf<String>(
                    Layer.BLEND_MODE_Default,
                    Layer.BLEND_MODE_Normal,
                    Layer.BLEND_MODE_Add,
                    Layer.BLEND_MODE_Substract,
                    Layer.BLEND_MODE_Negation,
                    Layer.BLEND_MODE_Average,
                    Layer.BLEND_MODE_Multiply,
                    Layer.BLEND_MODE_Difference,
                    Layer.BLEND_MODE_Screen,
                    Layer.BLEND_MODE_Softlight,
                    Layer.BLEND_MODE_Hardlight,
                    Layer.BLEND_MODE_Lighten,
                    Layer.BLEND_MODE_Darken,
                    Layer.BLEND_MODE_Reflect,
                    Layer.BLEND_MODE_Exclusion
            )
            val blendAdapter = BlendAdapter(blendModes, this@TextStickerActivity)
            blendAdapter.setOnItemClickListener(object : OnItemClickListener<String, BlendAdapter.BlendViewHolder> {
                override fun onItemClick(pos: Int, holder: BlendAdapter.BlendViewHolder, item: String) {
                    mThreadPool.execute {
                        mCurrentItemView?.updateBlendMode(item)
                    }
                    blendAdapter.setCurrentPosition(pos)
                }

            })

            val layoutManager = LinearLayoutManager(this@TextStickerActivity)
            layoutManager.orientation = LinearLayoutManager.HORIZONTAL
            mOptionView.lsq_blend_list.layoutManager = layoutManager
            mOptionView.lsq_blend_list.adapter = blendAdapter

            mOptionView.lsq_blend_mix_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
            mBlendAdapter = blendAdapter
            if (mCurrentItemView != null){
                mBlendAdapter?.findMode(mCurrentItemView!!.getBlendMode())
                mOptionView.lsq_blend_mix_bar.progress = (mCurrentItemView!!.getBlendMix() * 100).toInt()
            }
        }

        public fun getOptionView(): View {
            return mOptionView
        }

        public fun findBlendMode(mode :String){
            mBlendAdapter?.findMode(mode)
        }

        public fun clearMode(){
            mBlendAdapter?.setCurrentPosition(-1)
        }

        public fun setBlendMix(mix : Double){
            mOptionView.lsq_blend_mix_bar.progress = (mix * 100).toInt()
        }
    }


}