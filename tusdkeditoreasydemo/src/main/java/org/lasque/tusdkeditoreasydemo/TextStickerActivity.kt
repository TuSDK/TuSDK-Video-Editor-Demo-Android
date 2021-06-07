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
import androidx.recyclerview.widget.LinearLayoutManager
import cn.bar.DoubleHeadedDragonBar
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.jaygoo.widget.OnRangeChangedListener
import com.jaygoo.widget.RangeSeekBar
import com.tusdk.pulse.Config
import com.tusdk.pulse.Engine
import com.tusdk.pulse.Player
import com.tusdk.pulse.Producer
import com.tusdk.pulse.editor.ClipLayer
import com.tusdk.pulse.editor.EditorModel
import com.tusdk.pulse.editor.Layer
import com.tusdk.pulse.editor.VideoEditor
import com.tusdk.pulse.editor.clips.AnimationTextClip
import kotlinx.android.synthetic.main.include_sticker_view.*
import kotlinx.android.synthetic.main.include_title_layer.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_align.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_alpha.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_anitext_in.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_anitext_out.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_anitext_out.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_array.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_background.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_blend.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_color.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_color.view.lsq_editor_component_text_font_back
import kotlinx.android.synthetic.main.lsq_editor_component_text_shadow.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_spacing.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_stroke.view.*
import kotlinx.android.synthetic.main.lsq_editor_component_text_style.view.*
import kotlinx.android.synthetic.main.repeat_fragment.view.*
import kotlinx.android.synthetic.main.text_sticker_activity.*
import org.jetbrains.anko.toast
import org.lasque.tusdkeditoreasydemo.TextStickerActivity.Companion.TextFunction.*
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.*
import org.lasque.tusdkeditoreasydemo.base.views.ColorView
import org.lasque.tusdkeditoreasydemo.base.views.stickers.LayerItemViewBase
import org.lasque.tusdkeditoreasydemo.base.views.stickers.LayerType
import org.lasque.tusdkeditoreasydemo.base.views.stickers.LayerViewDelegate
import org.lasque.tusdkeditoreasydemo.base.views.stickers.TextLayerItemView
import org.lasque.tusdkpulse.core.utils.DateHelper
import org.lasque.tusdkpulse.core.utils.FileHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlHelper
import org.lasque.tusdkpulse.impl.view.widget.TuSeekBar
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
        const val MAX_STROKE_WIDTH = 100

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
            ANITEXT(R.drawable.t_ic_animation,R.string.lsq_editor_text_anitext),
            SHADOW(R.drawable.t_ic_shadow,R.string.lsq_editor_text_shadow),
            ;

            public val iconId = iconId
            public val textId = textId
        }


        val Input_Animation_List = mutableListOf(
                AnimationItem("none",0),
                AnimationItem("BlurIn",1),
                AnimationItem("WaveIn",2),
                AnimationItem("RotateFlyIn",3),
                AnimationItem("CloseUp",4),
                AnimationItem("ShotIn",5),
                AnimationItem("FlipUpIn",6),
                AnimationItem("ElasticScaleIn",7),
                AnimationItem("SpringIn",8),
                AnimationItem("FadeIn",9),
                AnimationItem("GrowUpIn",10),
                AnimationItem("EnlargeSlightIn",11),
                AnimationItem("ShrinkIn",12),
                AnimationItem("EnlargeIn",13),
                AnimationItem("RandomFlyIn",14),
                AnimationItem("ScrewIn",15),
                AnimationItem("RotateCornerIn",16),
                AnimationItem("PrinterOneIn",17),
                AnimationItem("PrinterTwoIn",18),
                AnimationItem("PrinterThreeIn",19),
                AnimationItem("MoveLeftIn",20),
                AnimationItem("MoveRightIn",21),
                AnimationItem("MoveUpIn",22),
                AnimationItem("MoveDownIn",23),
                AnimationItem("LinearWipeLeftIn",24),
                AnimationItem("LinearWipeRightIn",25),
                AnimationItem("LinearWipeUpIn",26),
                AnimationItem("LinearWipeDownIn",27),
                AnimationItem("LinearWipeFeatherLeftIn",28),
                AnimationItem("LinearWipeFeatherRightIn",29),
                AnimationItem("LinearWipeFeatherMiddleHorInt",30),
                AnimationItem("LinearWipeFeatherMiddleVerInt",31),
                AnimationItem("RadialWipeLeftIn",32),
                AnimationItem("RadialWipeRightIn",33),
                AnimationItem("BounceIn",34),
                AnimationItem("ByteDanceLoveIn",84),
                AnimationItem("ByteDanceNoteIn",85)

        )

        val Output_Animation_List = mutableListOf(
                AnimationItem("none",0),
                AnimationItem("BlurOut",35),
                AnimationItem("WaveOut",36),
                AnimationItem("RotateFlyOut",37),
                AnimationItem("OpenUp",38),
                AnimationItem("ShotOut",39),
                AnimationItem("FlipUpOut",40),
                AnimationItem("ElasticScaleOut",41),
                AnimationItem("SpringOut",42),
                AnimationItem("FadeOut",43),
                AnimationItem("GrowUpOut",44),
                AnimationItem("EnlargeSlightOut",45),
                AnimationItem("ShrinkOut",46),
                AnimationItem("EnlargeOut",47),
                AnimationItem("RandomFlyOut",48),
                AnimationItem("ScrewOut",49),
                AnimationItem("RotateCornerOut",50),
                AnimationItem("PrinterOneOut",51),
                AnimationItem("PrinterTwoOut",52),
                AnimationItem("PrinterThreeOut",53),
                AnimationItem("MoveLeftOut",54),
                AnimationItem("MoveRightOut",55),
                AnimationItem("MoveUpOut",56),
                AnimationItem("MoveDownOut",57),
                AnimationItem("LinearWipeLeftOut",58),
                AnimationItem("LinearWipeRightOut",59),
                AnimationItem("LinearWipeUpOut",60),
                AnimationItem("LinearWipeDownOut",61),
                AnimationItem("LinearWipeFeatherLeftOut",62),
                AnimationItem("LinearWipeFeatherRightOut",63),
                AnimationItem("LinearWipeFeatherMiddleHorOut",64),
                AnimationItem("LinearWipeFeatherMiddleVerOut",65),
                AnimationItem("RadialWipeLeftOut",66),
                AnimationItem("RadialWipeRightOut",67),
                AnimationItem("BounceOut",68)
        )

        val Overall_Animation_List = mutableListOf(
                AnimationItem("none",0),
                AnimationItem("FlipHor",69),
                AnimationItem("FlipVer",70),
                AnimationItem("SubtitleHor",71),
                AnimationItem("SubtitleVer",72),
                AnimationItem("Waggle",73),
                AnimationItem("Swing",74),
                AnimationItem("Wiper",75),
                AnimationItem("Tricky",76),
                AnimationItem("EnlargeChar",77),
                AnimationItem("Heartbeat",78),
                AnimationItem("FaultFlicker",79),
                AnimationItem("Spark",80),
                AnimationItem("Rock",81),
                AnimationItem("Shake",82),
                AnimationItem("Bounce",83)

        )
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
            ADD, ANITEXT,SHADOW,COLOR, ALPHA, STROKE, BACKGROUND, SPACE, ALIGN, ARRAY, STYLE, FONT, BLEND
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

    private var mAnimationOption : TextAnimationOption? = null

    private var mShadowOption: TextShadowOption? = null

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
    private var mCurrentProducer : VideoEditor.Producer? = null

    private var mCurrentSavePath = ""

    private var isNeedSave = true

    private fun saveVideo() {
        mThreadPool.execute {
            isNeedSave = true

            val producer = mEditor.newProducer()
            //                val outputFilePath = "${TuSdkContext.context().getExternalFilesDir(DIRECTORY_DCIM)!!.absolutePath}/editor_output${System.currentTimeMillis()}.mp4"

            val outputFilePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath}/editor_output${System.currentTimeMillis()}.mp4"
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
                        mEditor!!.resetProducer()
                        mPlayer!!.seekTo(mPlayerContext.currentFrame)
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
                    val currentP = (ts / producer.duration.toFloat()) * 100f
                    runOnUiThread {
                        lsq_editor_cut_load.setVisibility(View.VISIBLE)
                        lsq_editor_cut_load_parogress.setValue(currentP)
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
            if (mPlayer != null){
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
    }

    private fun playerPause() {
        mThreadPool.execute {
            if (mPlayer != null){
                if (mPlayer!!.pause()) {
                    runOnUiThread {
                        lsq_sticker_play.setImageResource(R.mipmap.edit_ic_play)
                    }
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
            ANITEXT -> {
                if (mAnimationOption == null){
                    mAnimationOption = TextAnimationOption()
                }
                mAnimationOption?.getOptionView()
            }
            SHADOW -> {
                if (mShadowOption == null){
                    mShadowOption = TextShadowOption()
                }
                mShadowOption?.getOptionView()
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
                    mVideoItem = VideoItem.createVideoItem(item.path, mEditor, true, item.type == AlbumItemType.Video,item.audioPath)
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
                val duration = mPlayer!!.duration
                runOnUiThread {
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
                    if (mCurrentItemViewEnd - mCurrentItemViewStart < 1000) {
                        if (mCurrentItemViewStart < 1000){
                            mCurrentItemViewEnd = mCurrentItemViewStart + 1000
                        } else {
                            mCurrentItemViewStart = mCurrentItemViewEnd - 1000
                        }

                        val min = mCurrentItemViewStart / mMaxDuration.toFloat() * 100
                        val max = mCurrentItemViewEnd / mMaxDuration.toFloat() * 100

                        lsq_start_bar.minValue = min.toInt()
                        lsq_start_bar.maxValue = max.toInt()
                        lsq_start_bar.invalidate()

                        toast("文字时长最小为1秒")
                    }
                    mThreadPool?.execute {
                        mCurrentItemView?.setClipDuration(mCurrentItemViewStart, mCurrentItemViewEnd)
                        mPlayer?.previewFrame(mCurrentItemViewStart)

                        runOnUiThread {
                            mAnimationOption?.refreshDurationRange()
                        }
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
                    mAlphaOption?.setAlphaPercent(mCurrentItemView!!.getTextAlpha())
                    mStrokeOption?.setStrokeColor(mCurrentItemView!!.getStrokeColor())
                    mStrokeOption?.setStrokeWidth(mCurrentItemView!!.getStrokeWidth())
                    mAlignOption?.updateAlign(mCurrentItemView!!.getTextAlign())
                    mSpacingOption?.updateLineSpacing(mCurrentItemView!!.getLineSpacing())
                    mSpacingOption?.updateWordSpacing(mCurrentItemView!!.getWordSpacing())
                    var currentInputAnimation : AnimationItem? = mCurrentItemView!!.getCurrentInputAnimator()
                    mAnimationOption?.findAnimation(currentInputAnimation?.itemIndex ?: 0,AnimationType.Input)
                    var currentOutputAnimation : AnimationItem? = mCurrentItemView!!.getCurrentOutputAnimator()
                    mAnimationOption?.findAnimation(currentOutputAnimation?.itemIndex ?: 0,AnimationType.Output)
                    var currentOverallAnimation : AnimationItem? = mCurrentItemView!!.getCurrentOverallAnimator()
                    mAnimationOption?.findAnimation(currentOverallAnimation?.itemIndex ?: 0,AnimationType.Overall)
                    lsq_text_input.setText(mCurrentItemView!!.getCurrentText())
                    if (mCurrentItemView!!.getTextShadow() != null){
                        mShadowOption?.setShadowValue(mCurrentItemView!!.getTextShadow()!!)
                    }
                }

                override fun onItemViewReleased(type: LayerType, view: LayerItemViewBase) {
                    val currentText = mCurrentItemView!!.getCurrentText()
                    lsq_text_input.setText(currentText)
                    if (isFirstCallSoftInput) {
                        isFirstCallSoftInput = false
                    } else {
//                        lsq_text_input_layer.visibility = View.VISIBLE
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

            if (mCurrentItemView!= null){
                setAlphaPercent(mCurrentItemView!!.getTextAlpha())
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

        public fun setAlphaPercent(alpha : Double){
            (mOptionView.lsq_editor_text_alpha_seek_font as TuSeekBar).progress = alpha.toFloat()
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

            if (mCurrentItemView != null){
                setStrokeColor(mCurrentItemView!!.getStrokeColor())

                setStrokeWidth(mCurrentItemView!!.getStrokeWidth())
            }


        }

        public fun getOptionView(): View {
            return mOptionView
        }

        public fun setStrokeColor(color : Int){
            runOnUiThread {
                mOptionView.lsq_editor_text_stroke_color.findColorInt(color)
            }
        }

        public fun setStrokeWidth(width : Double){
            runOnUiThread {
                (mOptionView.lsq_editor_text_stroke_alpha_seek as TuSeekBar).progress = (width / MAX_STROKE_WIDTH).toFloat()
            }
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
                mCurrentItemView?.updateBackgroundAlpha(progress.toDouble())
            }
            val colorView = mOptionView.lsq_editor_text_bg_color
            colorView.setCircleRadius(8)
            colorView.setOnColorChangeListener(object : ColorView.OnColorChangeListener {
                override fun changeColor(colorId: Int) {
                    mCurrentItemView?.updateBackgroundColor(colorId)
                }

                override fun changePosition(percent: Float) {
                }
            })
            alphaSeek.progress = 0F
            if (mCurrentItemView != null){
                findColor(mCurrentItemView!!.getBackgroundColor())
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
            (mOptionView.lsq_editor_text_bg_seek as TuSeekBar).progress = mCurrentItemView!!.getBackgroundOpacity().toFloat()

        }


    }

    inner class TextSpacingOption {
        private var mOptionView = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_spacing, null)
        
        private final val MAX_LINE_SPECING = 2.0

        private final val MIN_LINE_SPECING = 0.5

        init {
            mOptionView.lsq_editor_component_text_spacing_back.setOnClickListener {
                backToMain()
            }
            (mOptionView.lsq_editor_text_row_seek as TuSeekBar).setDelegate { seekBar, progress ->
                mCurrentItemView?.updateLineSpacing(MIN_LINE_SPECING + (progress.toDouble() * (MAX_LINE_SPECING - MIN_LINE_SPECING)))
            }
            (mOptionView.lsq_editor_text_word_seek as TuSeekBar).setDelegate { seekBar, progress ->
                mCurrentItemView?.updateWordSpacing(MIN_LINE_SPECING +(progress.toDouble() * (MAX_LINE_SPECING - MIN_LINE_SPECING)))
            }

            if (mCurrentItemView != null){
                updateLineSpacing(mCurrentItemView!!.getLineSpacing())
                updateWordSpacing(mCurrentItemView!!.getWordSpacing())
            }

        }

        public fun getOptionView(): View {
            return mOptionView
        }

        public fun updateLineSpacing(spacing : Double){
            val value = (spacing - MIN_LINE_SPECING) / (MAX_LINE_SPECING - MIN_LINE_SPECING)
            (mOptionView.lsq_editor_text_row_seek as TuSeekBar).progress = value.toFloat()
        }

        public fun updateWordSpacing(spacing : Double){
            val value = (spacing - MIN_LINE_SPECING)  / (MAX_LINE_SPECING - MIN_LINE_SPECING)
            (mOptionView.lsq_editor_text_word_seek as TuSeekBar).progress = value.toFloat()
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
                    mCurrentItemView?.updateTextAlign(AnimationTextClip.Alignment.LEFT)
                }

                updateAlign(AnimationTextClip.Alignment.LEFT)
            }
            mOptionView.lsq_editor_text_centeralignment.setOnClickListener {
                mThreadPool.execute {
                    mCurrentItemView?.updateTextAlign(AnimationTextClip.Alignment.CENTER)

                }

                updateAlign(AnimationTextClip.Alignment.CENTER)
            }
            mOptionView.lsq_editor_text_rightalignment.setOnClickListener {
                mThreadPool.execute {
                    mCurrentItemView?.updateTextAlign(AnimationTextClip.Alignment.RIGHT)
                }

                updateAlign(AnimationTextClip.Alignment.RIGHT)
            }

            if (mCurrentItemView!= null){
                updateAlign(mCurrentItemView!!.getTextAlign())
            }
        }

        public fun getOptionView(): View {
            return mOptionView
        }

        public fun updateAlign(alignment : AnimationTextClip.Alignment){
            mOptionView.leftalignment_icon.setImageResource(R.drawable.edit_text_ic_left)
            mOptionView.leftalignment_title.setTextColor(Color.WHITE)

            mOptionView.centeralignment_icon.setImageResource(R.drawable.edit_text_ic_center)
            mOptionView.centeralignment_title.setTextColor(Color.WHITE)

            mOptionView.rightalignment_icon.setImageResource(R.drawable.edit_text_ic_right)
            mOptionView.rightalignment_title.setTextColor(Color.WHITE)

            when(alignment){
                AnimationTextClip.Alignment.LEFT -> {
                    mOptionView.leftalignment_icon.setImageResource(R.drawable.t_ic_left_sel)
                    mOptionView.leftalignment_title.setTextColor(Color.parseColor("#f6a623"))
                }
                AnimationTextClip.Alignment.CENTER -> {
                    mOptionView.centeralignment_icon.setImageResource(R.drawable.t_ic_centered_sel)
                    mOptionView.centeralignment_title.setTextColor(Color.parseColor("#f6a623"))
                }
                AnimationTextClip.Alignment.RIGHT -> {
                    mOptionView.rightalignment_icon.setImageResource(R.drawable.t_ic_right_sel)
                    mOptionView.rightalignment_title.setTextColor(Color.parseColor("#f6a623"))
                }
            }

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
                if (mCurrentItemView!!.isTextReverse()) {
                    mThreadPool.execute {
                        mCurrentItemView?.textReverse(true)
                    }
                    lIcon.isSelected = true
                    rIcon.isSelected = false
                }
            }

            mOptionView.lsq_editor_text_righttoleft.setOnClickListener {
                if (!mCurrentItemView!!.isTextReverse()) {
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
                isUnderLine = true
                var res : Future<Boolean> = mThreadPool.submit(Callable<Boolean> {
                    mCurrentItemView?.updateTextStyle(isUnderLine)
                    true
                })
                res.get()
                updateUnderline(isUnderLine)
            }

            mOptionView.lsq_editor_text_normal.setOnClickListener {
                mCurrentItemView?.updateTextStyle(false)
                (mOptionView.lsq_editor_text_underline.getChildAt(0) as ImageView).setImageResource(R.drawable.t_ic_underline_nor)
                (mOptionView.lsq_editor_text_underline.getChildAt(1) as TextView).setTextColor(Color.WHITE)

                (mOptionView.lsq_editor_text_normal.getChildAt(0) as ImageView).setImageResource(R.drawable.t_ic_nor_sel)
                (mOptionView.lsq_editor_text_normal.getChildAt(1) as TextView).setTextColor(Color.parseColor("#f6a623"))
                isUnderLine = false
            }

            if (mCurrentItemView!= null){
                updateUnderline(mCurrentItemView!!.isUnderLine())
            }
        }

        public fun updateUnderline(isunderline : Boolean) {
            isUnderLine = isunderline
            if (isunderline) {
                (mOptionView.lsq_editor_text_underline.getChildAt(0) as ImageView).setImageResource(R.drawable.t_ic_underline_sel)
                (mOptionView.lsq_editor_text_underline.getChildAt(1) as TextView).setTextColor(Color.parseColor("#f6a623"))

                (mOptionView.lsq_editor_text_normal.getChildAt(0) as ImageView).setImageResource(R.drawable.t_ic_nor_nor)
                (mOptionView.lsq_editor_text_normal.getChildAt(1) as TextView).setTextColor(Color.WHITE)
            } else {
                (mOptionView.lsq_editor_text_underline.getChildAt(0) as ImageView).setImageResource(R.drawable.t_ic_underline_nor)
                (mOptionView.lsq_editor_text_underline.getChildAt(1) as TextView).setTextColor(Color.WHITE)

                (mOptionView.lsq_editor_text_normal.getChildAt(0) as ImageView).setImageResource(R.drawable.t_ic_nor_sel)
                (mOptionView.lsq_editor_text_normal.getChildAt(1) as TextView).setTextColor(Color.parseColor("#f6a623"))
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


    enum class AnimationType{
        Input,Output,Overall
    }

    inner class TextAnimationOption{



        private var mOptionView : View = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_anitext_out,null)

        private var mInputAnimationAdapter : AnimationAdapter? = null

        private var mOutputAnimationAdapter : AnimationAdapter? = null

        private var mOverallAnimationAdapter : AnimationAdapter? = null

        private var mInputAnimationStartPos = 0.0

        private var mInputAnimationEndPos = 0.1

        private var mOutputAnimationStartPos = 0.9

        private var mOutputAnimationEndPos = 1.0

        private var mOverallAnimationStartPos = 0.0

        private var mOverallAnimationEndPos = 1.0

        private var mInputAnimationItem : AnimationItem? = null

        private var mOutputAnimationItem : AnimationItem? = null

        private var mOverallAnimationItem : AnimationItem? = null

        public var mCurrentAnimationType : AnimationType = AnimationType.Input

        init {
            mOptionView.lsq_editor_component_text_font_back.setOnClickListener {
                backToMain()
            }

            val animationAdapter = AnimationAdapter(Output_Animation_List,this@TextStickerActivity)
            animationAdapter.setOnItemClickListener(object : OnItemClickListener<AnimationItem, AnimationAdapter.AnimationViewHolder>{
                override fun onItemClick(pos: Int, holder: AnimationAdapter.AnimationViewHolder, item: AnimationItem) {
                    mOutputAnimationItem = item
                    mCurrentItemView?.addOutputAnimator(mOutputAnimationStartPos,mOutputAnimationEndPos,item)
                    animationAdapter.setCurrentPosition(pos)
                }
            })

            val layoutManager = LinearLayoutManager(this@TextStickerActivity)
            layoutManager.orientation = LinearLayoutManager.HORIZONTAL
            mOptionView.lsq_anitext_out_list.layoutManager = layoutManager


            mOutputAnimationAdapter = animationAdapter

            val inputAnimationAdapter = AnimationAdapter(Input_Animation_List,this@TextStickerActivity)
            inputAnimationAdapter.setOnItemClickListener(object : OnItemClickListener<AnimationItem, AnimationAdapter.AnimationViewHolder>{
                override fun onItemClick(pos: Int, holder: AnimationAdapter.AnimationViewHolder, item: AnimationItem) {
                    mInputAnimationItem = item
                    mCurrentItemView?.addInputAnimator(mInputAnimationStartPos,mInputAnimationEndPos,item)
                    inputAnimationAdapter.setCurrentPosition(pos)
                }

            })
            mOptionView.lsq_anitext_out_list.adapter = inputAnimationAdapter

            mInputAnimationAdapter = inputAnimationAdapter

            val overallAnimationAdapter = AnimationAdapter(Overall_Animation_List,this@TextStickerActivity)
            overallAnimationAdapter.setOnItemClickListener(object : OnItemClickListener<AnimationItem, AnimationAdapter.AnimationViewHolder>{
                override fun onItemClick(pos: Int, holder: AnimationAdapter.AnimationViewHolder, item: AnimationItem) {
                    mOverallAnimationItem = item
                    mCurrentItemView?.addOverallAnimator(mOverallAnimationStartPos,mOverallAnimationEndPos,item)
                    overallAnimationAdapter.setCurrentPosition(pos)
                }

            })

            mOverallAnimationAdapter = overallAnimationAdapter

            mOptionView.lsq_anitext_input_title.setTextColor(Color.RED)

            mOptionView.lsq_anitext_input_title.setOnClickListener {
                mOptionView.lsq_anitext_out_list.adapter = mInputAnimationAdapter
                mCurrentAnimationType = AnimationType.Input


                mOptionView.lsq_anitext_input_title.setTextColor(Color.RED)
                mOptionView.lsq_anitext_output_title.setTextColor(Color.WHITE)
                mOptionView.lsq_anitext_all_title.setTextColor(Color.WHITE)

                mOptionView.lsq_anitext_out_duration_bar.seekBarMode = RangeSeekBar.SEEKBAR_MODE_RANGE

                mOptionView.lsq_anitext_out_duration_bar.progressDefaultColor = getColor(R.color.lsq_seek_value_color)
                mOptionView.lsq_anitext_out_duration_bar.progressColor = getColor(R.color.lsq_icon_set_color)

                setDuration(mInputAnimationEndPos,mOutputAnimationStartPos,mCurrentAnimationType)

                setDurationRange(mInputAnimationEndPos,mOutputAnimationStartPos)

                mOptionView.lsq_anitxt_duration_title.setText("进入/退出 :")




                refreshItems()

            }

            mOptionView.lsq_anitext_output_title.setOnClickListener {
                mOptionView.lsq_anitext_out_list.adapter = mOutputAnimationAdapter
                mCurrentAnimationType = AnimationType.Output


                mOptionView.lsq_anitext_input_title.setTextColor(Color.WHITE)
                mOptionView.lsq_anitext_output_title.setTextColor(Color.RED)
                mOptionView.lsq_anitext_all_title.setTextColor(Color.WHITE)

                mOptionView.lsq_anitext_out_duration_bar.seekBarMode = RangeSeekBar.SEEKBAR_MODE_RANGE

                mOptionView.lsq_anitext_out_duration_bar.progressDefaultColor = getColor(R.color.lsq_seek_value_color)
                mOptionView.lsq_anitext_out_duration_bar.progressColor = getColor(R.color.lsq_icon_set_color)

                setDuration(mInputAnimationEndPos,mOutputAnimationStartPos,mCurrentAnimationType)

                setDurationRange(mInputAnimationEndPos,mOutputAnimationStartPos)

                mOptionView.lsq_anitxt_duration_title.setText("进入/退出 :")




                refreshItems()
            }

            mOptionView.lsq_anitext_all_title.setOnClickListener {
                mOptionView.lsq_anitext_out_list.adapter = mOverallAnimationAdapter
                mCurrentAnimationType = AnimationType.Overall

                mOptionView.lsq_anitext_input_title.setTextColor(Color.WHITE)
                mOptionView.lsq_anitext_output_title.setTextColor(Color.WHITE)
                mOptionView.lsq_anitext_all_title.setTextColor(Color.RED)

                mOptionView.lsq_anitext_out_duration_bar.seekBarMode = RangeSeekBar.SEEKBAR_MODE_RANGE

                mOptionView.lsq_anitext_out_duration_bar.progressColor = getColor(R.color.lsq_seek_value_color)
                mOptionView.lsq_anitext_out_duration_bar.progressDefaultColor = getColor(R.color.lsq_icon_set_color)


                setDuration(mOverallAnimationStartPos,mOverallAnimationEndPos,mCurrentAnimationType)

                setDurationRange(mOverallAnimationStartPos,mOverallAnimationEndPos)

                mOptionView.lsq_anitxt_duration_title.setText("动画时长 :")



                refreshItems()

            }

            mOptionView.lsq_anitext_out_duration_bar.setRange(0f,100f)
            mOptionView.lsq_anitext_out_duration_bar.setOnRangeChangedListener(object : OnRangeChangedListener {
                override fun onRangeChanged(view: RangeSeekBar?, leftValue: Float, rightValue: Float, isFromUser: Boolean) {
                    if (!isFromUser) return
                    when(mCurrentAnimationType){
                        AnimationType.Input,AnimationType.Output -> {
                            mInputAnimationEndPos = (leftValue / 100f).toDouble()
                            if (mInputAnimationItem != null){
                                mCurrentItemView?.addInputAnimator(mInputAnimationStartPos,mInputAnimationEndPos,mInputAnimationItem!!)
                            }

                            mOutputAnimationStartPos = (rightValue / 100f).toDouble()
                            if (mOutputAnimationItem != null){
                                mCurrentItemView?.addOutputAnimator(mOutputAnimationStartPos,mOutputAnimationEndPos, mOutputAnimationItem!!)
                            }
                            setDurationRange(mInputAnimationEndPos,mOutputAnimationStartPos)
                        }
                        AnimationType.Overall -> {
                            mOverallAnimationStartPos = (leftValue / 100f).toDouble()
                            mOverallAnimationEndPos = (rightValue / 100f).toDouble()
                            if (mOverallAnimationItem != null){
                                mCurrentItemView?.addOverallAnimator(mOverallAnimationStartPos,mOverallAnimationEndPos, mOverallAnimationItem!!)
                            }
                            setDurationRange(mOverallAnimationStartPos,mOverallAnimationEndPos)
                        }
                    }
                }

                override fun onStartTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {

                }

                override fun onStopTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {

                }

            })

            mOptionView.lsq_anitext_out_duration_bar.seekBarMode = RangeSeekBar.SEEKBAR_MODE_RANGE
            mOptionView.lsq_anitext_out_duration_bar.progressDefaultColor = getColor(R.color.lsq_seek_value_color)
            mOptionView.lsq_anitext_out_duration_bar.progressColor = getColor(R.color.lsq_icon_set_color)

            if (mCurrentItemView != null){
                when(mCurrentAnimationType){
                    AnimationType.Input,AnimationType.Output -> {
                        mOptionView.lsq_anitext_out_duration_bar.setProgress(mInputAnimationEndPos.toFloat() * 100,mOutputAnimationStartPos.toFloat() * 100)
                        setDurationRange(mInputAnimationEndPos,mOutputAnimationStartPos)
                    }
                    AnimationType.Overall -> {
                        mOptionView.lsq_anitext_out_duration_bar.setProgress(mOverallAnimationStartPos.toFloat(), mOverallAnimationEndPos.toFloat())
                        setDurationRange(mOverallAnimationStartPos,mOverallAnimationEndPos)
                    }
                }
                mOptionView.lsq_anitext_out_duration_bar.invalidate()

                val list = mCurrentItemView!!.getAnimatorList()
                if (list.isNotEmpty()) for (item in list){
                    mInputAnimationAdapter!!.findAnimation(item.path)
                    var currentIndex = mInputAnimationAdapter!!.getCurrentPosition()
                    if (currentIndex != -1){
                        mInputAnimationStartPos = item.start
                        mInputAnimationEndPos = item.end
                        mInputAnimationItem = Input_Animation_List[currentIndex]
                        mCurrentItemView?.addInputAnimator(mInputAnimationStartPos,mInputAnimationEndPos,mInputAnimationItem!!)
                    }
                    mOutputAnimationAdapter!!.findAnimation(item.path)
                    currentIndex = mOutputAnimationAdapter!!.getCurrentPosition()
                    if (currentIndex != -1){
                        mOutputAnimationStartPos = item.start
                        mOutputAnimationEndPos = item.end
                        mOutputAnimationItem = Output_Animation_List[currentIndex]
                        mCurrentItemView?.addOutputAnimator(mOutputAnimationStartPos,mOutputAnimationEndPos, mOutputAnimationItem!!)
                    }
                    mOverallAnimationAdapter!!.findAnimation(item.path)
                    currentIndex = mOverallAnimationAdapter!!.getCurrentPosition()
                    if (currentIndex != -1){
                        mOverallAnimationStartPos = item.start
                        mOverallAnimationEndPos = item.end
                        mOverallAnimationItem = Overall_Animation_List[currentIndex]
                        mCurrentItemView?.addOverallAnimator(mOverallAnimationStartPos,mOverallAnimationEndPos, mOverallAnimationItem!!)
                    }
                }

                refreshItems()



            }
        }

        public fun refreshDurationRange(){
            when(mCurrentAnimationType){
                AnimationType.Input,AnimationType.Output -> {
                    setDurationRange(mInputAnimationEndPos,mOutputAnimationStartPos)
                }
                AnimationType.Overall -> {
                    setDurationRange(mOverallAnimationStartPos,mOverallAnimationEndPos)
                }
            }
        }

        private fun setDurationRange(start: Double,end: Double) {
            val maxDuration = mCurrentItemView!!.getClipDuration()

            val startTs = (start * maxDuration)

            val m = startTs / 1000 / 60

            val s = startTs / 1000 % 60

            val endTs = (end * maxDuration)

            val em = endTs / 1000 / 60

            val es = endTs / 1000 % 60

            val maxDm = maxDuration / 1000 / 60
            val maxDs = maxDuration / 1000 % 60

            when(mCurrentAnimationType){
                AnimationType.Input,AnimationType.Output -> {
                    mOptionView.lsq_animation_start_time.setText(
                            "进入动画 开始 : 00:00 结束 : ${String.format("%02d",m.toInt())}:${String.format("%02d",s.toInt())}"
                    )
                    mOptionView.lsq_animation_end_time.setText(
                            "退出动画 开始 : ${String.format("%02d",em.toInt())}:${String.format("%02d",es.toInt())} 结束 : ${String.format("%02d",maxDm.toInt())}:${String.format("%02d",maxDs.toInt())}"
                    )
                }
                AnimationType.Overall -> {
                    mOptionView.lsq_animation_start_time.setText(
                            "整体动画 开始 : ${String.format("%02d",m.toInt())}:${String.format("%02d",s.toInt())} 结束 : ${String.format("%02d",em.toInt())}:${String.format("%02d",es.toInt())}"
                    )
                    mOptionView.lsq_animation_end_time.setText("")
                }
            }


        }

        private fun refreshItems() {
            var currentOutputAnimation: AnimationItem? = mCurrentItemView!!.getCurrentOutputAnimator()
            mOutputAnimationAdapter?.findAnimation(currentOutputAnimation?.itemIndex ?: 0)

            var currentInputAnimation: AnimationItem? = mCurrentItemView!!.getCurrentInputAnimator()
            mInputAnimationAdapter?.findAnimation(currentInputAnimation?.itemIndex ?: 0)
            var currentOverallAnimation: AnimationItem? = mCurrentItemView!!.getCurrentOverallAnimator()
            mOverallAnimationAdapter?.findAnimation(currentOverallAnimation?.itemIndex ?: 0)
        }


        fun findAnimation(index : Int,type : AnimationType){
            when(type){
                AnimationType.Input ->
                {
                    mInputAnimationAdapter?.findAnimation(index)
                }
                AnimationType.Output ->
                {
                    mOutputAnimationAdapter?.findAnimation(index)
                }
                AnimationType.Overall ->
                {
                    mOverallAnimationAdapter?.findAnimation(index)
                }
            }

        }

        public fun getOptionView() : View{
            return mOptionView
        }

        public fun setDuration(start : Double,end : Double,type : AnimationType){

            var startPos = start
            var endPos = end


            when(type){
                AnimationType.Input,AnimationType.Output -> {
                    mInputAnimationEndPos = start
                    mOutputAnimationStartPos = end
                }
                AnimationType.Overall -> {
                    mOverallAnimationStartPos = start
                    mOverallAnimationEndPos = end
                }
            }
            when(mCurrentAnimationType){
                AnimationType.Input,AnimationType.Output -> {
                    mOptionView.lsq_anitext_out_duration_bar.setProgress(mInputAnimationEndPos.toFloat() * 100,mOutputAnimationStartPos.toFloat() * 100)
                }
                AnimationType.Overall -> {
                    mOptionView.lsq_anitext_out_duration_bar.setProgress(mOverallAnimationStartPos.toFloat() * 100, mOverallAnimationEndPos.toFloat() * 100)
                }
            }
            mOptionView.lsq_anitext_out_duration_bar.invalidate()
        }
    }

    inner class TextShadowOption{
        private var mOptionView : View = LayoutInflater.from(this@TextStickerActivity).inflate(R.layout.lsq_editor_component_text_shadow,null)

        private var shadowItem : AnimationTextClip.Shadow = AnimationTextClip.Shadow()

        init {
            mOptionView.lsq_editor_component_text_font_back.setOnClickListener {
                backToMain()
            }

            mOptionView.lsq_shadow_color_bar.setOnColorChangeListener(object : ColorView.OnColorChangeListener{
                override fun changeColor(colorId: Int) {
                    shadowItem.color = colorId
                    mCurrentItemView?.updateTextShadow(shadowItem)
                }

                override fun changePosition(percent: Float) {

                }

            })

            mOptionView.lsq_shadow_opacity_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    shadowItem.opacity = progress / 100.0
                    mCurrentItemView?.updateTextShadow(shadowItem)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }

            })

            mOptionView.lsq_shadow_blur_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    shadowItem.blur = progress / 100.0
                    mCurrentItemView?.updateTextShadow(shadowItem)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }

            })

            mOptionView.lsq_shadow_distance_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    shadowItem.distance = progress
                    mCurrentItemView?.updateTextShadow(shadowItem)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }

            })

            mOptionView.lsq_shadow_degree_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    shadowItem.degree = progress
                    mCurrentItemView?.updateTextShadow(shadowItem)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }

            })

            if (mCurrentItemView != null && mCurrentItemView!!.getTextShadow() != null){
                setShadowValue(mCurrentItemView!!.getTextShadow()!!)
            }
        }

        public fun getOptionView() : View{
            return mOptionView
        }

        public fun setShadowValue(shadow : AnimationTextClip.Shadow){
            mOptionView.lsq_shadow_color_bar.findColorInt(shadow.color)
            mOptionView.lsq_shadow_opacity_bar.progress = (shadow.opacity * 100).toInt()
            mOptionView.lsq_shadow_blur_bar.progress = (shadow.blur * 100).toInt()
            mOptionView.lsq_shadow_distance_bar.progress = shadow.distance
            mOptionView.lsq_shadow_degree_bar.progress = shadow.degree
        }
    }


    override fun onPause() {
        super.onPause()
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

    override fun onResume() {
        super.onResume()
        if (!Engine.getInstance().checkEGL()){
            throw Exception("dsssssssssssssssss");
        }
    }


}