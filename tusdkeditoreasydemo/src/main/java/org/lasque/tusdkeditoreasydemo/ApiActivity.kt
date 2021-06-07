/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/10/22$ 16:44$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DCIM
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.tusdk.pulse.Engine
import com.tusdk.pulse.Player
import com.tusdk.pulse.Producer
import com.tusdk.pulse.editor.EditorModel
import com.tusdk.pulse.editor.VideoEditor
import kotlinx.android.synthetic.main.api_activity.*
import kotlinx.android.synthetic.main.include_title_layer.*
import org.jetbrains.anko.contentView
import org.jetbrains.anko.startActivityForResult
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.utils.DateHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.AlbumHelper
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlHelper
import org.lasque.tusdkeditoreasydemo.album.AlbumActivity
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.apis.*
import org.lasque.tusdkeditoreasydemo.base.*
import org.lasque.tusdkpulse.core.utils.FileHelper
import java.io.File
import java.lang.Math.ceil
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.collections.ArrayList

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/22  16:44
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class ApiActivity : BaseActivity() {

    private var mAlbumList = ArrayList<AlbumInfo>()

    private var mCurrentFuncType = FunctionType.Null

    private var mCurrentFragment: BaseFragment? = null

    private var mEditor: VideoEditor = VideoEditor()

    private var mEvaThreadPool: ExecutorService = Executors.newSingleThreadExecutor()

    private var mPlayer: VideoEditor.Player? = null

    private var mCurrentState = Player.State.kREADY

    private var isSeekBarTouch = false

    private var mPlayerContext: EditorPlayerContext = EditorPlayerContext(mEditor)

    private var isPlayerInit = false

    private var isFromModel = false

    private var mPlayerStateUpdateListener = object : OnPlayerStateUpdateListener {
        override fun onDurationUpdate() {
            runOnUiThread {
//                TLog.e("duration ${mPlayer!!.duration}")
                seekBar.max = mPlayer!!.duration.toInt()
                refreshDuration(mPlayerContext.currentFrame)
            }
        }

        override fun onPlayerPlay() {
            playerPlay()
        }

        override fun onPlayerPause() {
            playerPause()
        }

        override fun onRefreshFrame() {
            if (mPlayerContext.state != Player.State.kPLAYING)
                mPlayerContext.refreshFrame()
        }
    }

    private var mLifecycleObserver = object : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onFragmentResume() {
            if (mPlayer == null) {
                onPlayerInit()
                isPlayerInit = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 1) and (resultCode == 1)) {
            var albumBundle = data!!.getBundleExtra("select")
            var albumList = albumBundle?.getSerializable("select") as ArrayList<AlbumInfo>
            if (albumList != null) {
                mAlbumList.addAll(albumList)
            }
            initFragment()
        } else if ((resultCode == 1)) {
            mCurrentFragment?.onActivityResult(requestCode, resultCode, data)
        } else {
            if (!isPlayerInit)
                finish()
        }


    }

    private fun initFragment() {
        when (mCurrentFuncType) {
            FunctionType.MoiveCut -> {
                mCurrentFragment = MovieCutFragment()
            }
            FunctionType.VideoStitching -> {
                mCurrentFragment = VideoStitchingFragment(FunctionType.VideoStitching)
            }
            FunctionType.VideoImageStitching -> {
                mCurrentFragment = VideoStitchingFragment(FunctionType.VideoImageStitching)
            }
            FunctionType.ImageStitching -> {
                mCurrentFragment = ImageStitchingFragment()
            }
            FunctionType.VideoAudioMix -> {
                mCurrentFragment = VideoAudioMixFragment()
            }
            FunctionType.FilterEffect -> {
                mCurrentFragment = FilterFragment()
            }
            FunctionType.MVEffect -> {
                mCurrentFragment = MVFragment()
            }
            FunctionType.SceneEffect -> {
                mCurrentFragment = SceneFragment()
            }
            FunctionType.ReverseEffect -> {
                mCurrentFragment = ReverseFragment()
            }
            FunctionType.SlowEffect -> {
                mCurrentFragment = SlowFragment()
            }
            FunctionType.RepeatEffect -> {
                mCurrentFragment = RepeatFragment()
            }
            FunctionType.TransitionsEffect -> {
                mCurrentFragment = TransitionsFragment()
            }
            FunctionType.VideoRatio -> {
                mCurrentFragment = RatioFragment()
            }
            FunctionType.Cover -> {
                mCurrentFragment = CoverFragment()
                lsq_output_video.visibility = View.INVISIBLE
            }
            FunctionType.Speed -> {
                mCurrentFragment = SpeedFragment()
            }
            FunctionType.AudioMix -> {
                mCurrentFragment = AudioMixFragment()
            }
            FunctionType.ColorAdjust -> {
                mCurrentFragment = ColorAdjustFragment()
            }
            FunctionType.Crop -> {
                mCurrentFragment = CropFragment()
            }
            FunctionType.Transform -> {
                mCurrentFragment = TransformFragment()
            }
            FunctionType.CanvasBackgroundType -> {
                mCurrentFragment = CanvasBackgroundFragment()
            }
            FunctionType.VideoSegmentation -> {
                mCurrentFragment = VideoSegmentationFragment()
            }
            FunctionType.AudioPitch->{
                mCurrentFragment = AudioPitchFragment()
            }
            FunctionType.Freeze->{
                mCurrentFragment = FreezeFragment()
            }
        }
        mCurrentFragment?.setCurrentThreadPool(mEvaThreadPool)
        mCurrentFragment?.setVideoEditor(mEditor)
        mCurrentFragment?.setVideoList(mAlbumList)
        mCurrentFragment?.setAudioList(mAlbumList)
        mCurrentFragment?.setPlayerStateUpdateListener(mPlayerStateUpdateListener)
        mCurrentFragment?.setPlayerContext(mPlayerContext)
        mCurrentFragment!!.lifecycle.addObserver(mLifecycleObserver)
        supportFragmentManager.beginTransaction().add(R.id.lsq_api_panel, mCurrentFragment!!).commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.api_activity)

        lsq_back.setOnClickListener {
            finish()
        }

        lsq_api_displayView.init(Engine.getInstance().mainGLContext)
        lsq_api_displayView.setOnClickListener {
            mEvaThreadPool.execute {
                if (mCurrentState == Player.State.kPLAYING) {
                    playerPause()
                } else {
                    playerPlay()
                }
            }
        }
        lsq_editor_play.setOnClickListener {
            mEvaThreadPool.execute {
                if (mCurrentState == Player.State.kPLAYING) {
                    playerPause()
                } else {
                    playerPlay()
                }
            }
        }
        val modelPath = intent.getStringExtra(DraftItem.Draft_Path_Key)
        if (TextUtils.isEmpty(modelPath)){
            mEvaThreadPool.execute {
                val openConfig: VideoEditor.OpenConfig = VideoEditor.OpenConfig()
                openConfig.width = 800
                openConfig.height = 800
                if (!mEditor.create(openConfig)) {
                    //todo 异常处理
                    TLog.e("Editor Create failed")
                }
            }
        } else {
            isFromModel = true
            mEvaThreadPool.execute {
                val editorModel = EditorModel(modelPath)
                if (!mEditor.create(editorModel)){
                    TLog.e("Editor Create failed")
                }
                if (!mEditor.build()){
                    TLog.e("Editor Build failed")
                }
            }
        }



        mCurrentFuncType = intent.extras!!["function"] as FunctionType
        lsq_title.setText(mCurrentFuncType.mTitleId)

        if (TextUtils.isEmpty(modelPath)){
            when (mCurrentFuncType) {
                FunctionType.Null -> {
                    return
                }
                FunctionType.MoiveCut -> {
                    openAlbum(1, false, true)
                }
                FunctionType.VideoStitching -> {
                    openAlbum(9, false, true)
                }
                FunctionType.VideoImageStitching -> {
                    openAlbum(9, false, false)
                }
                FunctionType.ImageStitching -> {
                    openAlbum(-1, true, false)
                }
                FunctionType.VideoAudioMix -> {
                    openAlbum(1, false, false)
                }
                FunctionType.FilterEffect -> {
                    openAlbum(1, false, false)
                }
                FunctionType.MVEffect -> {
                    openAlbum(1, false, false)
                }
                FunctionType.SceneEffect -> {
                    openAlbum(1, false, false)
                }
                FunctionType.ReverseEffect -> {
                    openAlbum(1, false, true)
                }
                FunctionType.SlowEffect -> {
                    openAlbum(1, false, true)
                }
                FunctionType.RepeatEffect -> {
                    openAlbum(1, false, true)
                }
                FunctionType.TransitionsEffect -> {
                    openAlbum(2, false, false, min = 2)
                }
                FunctionType.VideoRatio -> {
                    openAlbum(1, false, false)
                }
                FunctionType.Cover -> {
                    openAlbum(1, false, true)
                }
                FunctionType.Speed -> {
                    openAlbum(1, false, true)
                }
                FunctionType.AudioMix -> {
                    openAlbum(1, false, false)
                }
                FunctionType.ColorAdjust -> {
                    openAlbum(1, false, false)
                }
                FunctionType.Crop -> {
                    openAlbum(1, false, false)
                }
                FunctionType.Transform -> {
                    openAlbum(1, false, false)
                }
                FunctionType.CanvasBackgroundType -> {
                    lsq_api_panel_scroll.isNestedScrollingEnabled = false
                    openAlbum(1, false, false)
                }
                FunctionType.VideoSegmentation -> {
                    openAlbum(1, false, true)
                }
                FunctionType.AudioPitch -> {
                    openAlbum(1, false, true)
                }
                FunctionType.Freeze->{
                    openAlbum(1, false, true)
                }
            }
        } else {
            initFragment()
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
    }

    private fun saveDraft(){
        mEvaThreadPool.execute {
            val outputFileName = "editor_draft${System.currentTimeMillis()}"
            val outputFile = "${externalCacheDir!!.absolutePath}/draft"
            val file = File(outputFile)
            if (file.mkdirs()){

            }
            val outputFilePath = "${outputFile}/${outputFileName}"
            TLog.e("draft path ${outputFilePath}")
            mEditor!!.model.save(outputFilePath)

            val currentModule = mCurrentFuncType

            val sp = getSharedPreferences("Tu-Draft-list", Context.MODE_PRIVATE)
            val cal = Calendar.getInstance()
            val draftItem = DraftItem(currentModule.ordinal,outputFilePath,DateHelper.format(cal,"yyyy/MM/dd HH:mm:ss"),outputFileName)
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

    private var mCurrentProducer : VideoEditor.Producer? = null

    private var mCurrentSavePath = ""

    private var isNeedSave = true

    private fun saveVideo() {
        mEvaThreadPool.execute {
            isNeedSave = true
            val producer = mEditor.newProducer()
    //                val outputFilePath = "${TuSdkContext.context().getExternalFilesDir(DIRECTORY_DCIM)!!.absolutePath}/editor_output${System.currentTimeMillis()}.mp4"

            val outputFilePath = "${Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).absolutePath}/editor_output${System.currentTimeMillis()}.mp4"
            TLog.e("output path $outputFilePath")
            mCurrentSavePath = outputFilePath

            val config = Producer.OutputConfig()
            config.watermark = BitmapHelper.getRawBitmap(this, R.raw.sample_watermark)
            config.watermarkPosition = 1

            producer.setOutputConfig(config)
            producer.setListener { state, ts ->
                if (state == Producer.State.kEND) {
                    mEvaThreadPool.execute {
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

    public fun playerPlay() {
        if (mPlayer?.play()!!) {
            runOnUiThread {
                lsq_editor_play.setImageResource(R.mipmap.edit_ic_pause)
            }
        }

    }

    private fun onPlayerInit() {
        mEvaThreadPool.execute {
            if (!isFromModel){
                if (!mEditor.build()) {

                }
            }
            mPlayer = mEditor.newPlayer()
            mPlayer?.setListener { state, ts ->
                TLog.e("current state ${state}")
                mCurrentState = state
                mPlayerContext.state = state
                if (state == Player.State.kDO_PLAY) {
                    val durationMS = mPlayer!!.duration
                    runOnUiThread {

                        seekBar.max = durationMS.toInt()
                    }
                } else if (state == Player.State.kEOS) {
                    runOnUiThread {
                        lsq_editor_play.setImageResource(R.mipmap.edit_ic_play)
                        mEvaThreadPool.execute {
                            mPlayer!!.previewFrame(0)
                        }
                    }
                } else if (state == Player.State.kDO_PAUSE) {
                    runOnUiThread {
                        lsq_editor_play.setImageResource(R.mipmap.edit_ic_play)
                    }
                } else if (state == Player.State.kDO_PLAY) {
                    runOnUiThread {
                        lsq_editor_play.setImageResource(R.mipmap.edit_ic_pause)
                    }
                } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW || state == Player.State.kDO_SEEK) {
                    refreshDuration(ts)
                }
            }

            if (!mPlayer!!.open()) {
                TLog.e("Editor Player Open failed")
            }

            runOnUiThread {
                lsq_api_displayView.attachPlayer(mPlayer)
                val dur = mPlayer!!.duration.toInt()
                seekBar.max = dur
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        if (!p2) return
                        mEvaThreadPool.execute {
                            mPlayer?.previewFrame(p1.toLong())
                        }
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        isSeekBarTouch = true
                        mEvaThreadPool.execute {
                            playerPause()
                        }
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        isSeekBarTouch = false
//                        mEvaThreadPool.execute {
//                            mPlayer?.play()
//                            runOnUiThread {
//                                lsq_editor_play.visibility = View.GONE
//                            }
//                        }
                    }
                })
            }
            mPlayer!!.previewFrame(0)
        }
    }

    private fun refreshDuration(ts: Long) {
        mPlayerContext.currentFrame = ts
        val currentVideoHour = ts / 3600000
        val currentVideoMinute = (ts % 3600000) / 60000

        val currentVideoSecond = ceil(ts % 60000.00 / 1000).toLong()

        val durationMS = mPlayer!!.duration
        val durationVideoHour = durationMS / 3600000

        val durationVideoMinute = (durationMS % 3600000) / 60000

        val durationVideoSecond = ceil(durationMS % 60000.00 / 1000).toLong()
        runOnUiThread {
            if (!isSeekBarTouch) {
                seekBar.progress = ts.toInt()
            }
            lsq_player_duration.text = "${currentVideoHour}:$currentVideoMinute:$currentVideoSecond/$durationVideoHour:$durationVideoMinute:$durationVideoSecond"
        }
    }

    public fun playerPause() {
        if (mPlayer?.pause()!!) {
            runOnUiThread {
                lsq_editor_play.setImageResource(R.mipmap.edit_ic_play)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!Engine.getInstance().checkEGL()) {
            throw Exception("dsssssssssssssssss");
        } else {
            mEvaThreadPool?.execute { mPlayerContext.refreshFrame() }
        }
    }

    override fun onPause() {
        super.onPause()

    }

    override fun onStop() {
        super.onStop()
        mEvaThreadPool.execute {
            if (mCurrentProducer != null){
                isNeedSave = false
                mCurrentProducer?.cancel()
                FileHelper.delete(File(mCurrentSavePath))
                mCurrentProducer = null
            }

            mPlayer?.pause()
            runOnUiThread {
//                lsq_editor_play.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mCurrentFragment != null)
            supportFragmentManager.beginTransaction().remove(mCurrentFragment!!)
        VideoItem.resetID()
        var res : Future<Boolean> = mEvaThreadPool.submit(Callable<Boolean> {
            lsq_api_displayView.release()

            mPlayer?.close()
            mEditor.destroy()
            mEvaThreadPool.shutdownNow()

            true
        })

        res.get()
    }
}
