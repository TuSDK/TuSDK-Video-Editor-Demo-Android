/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/11/11$ 10:31$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import androidx.core.util.TimeUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.tusdk.pulse.Config
import com.tusdk.pulse.Engine
import com.tusdk.pulse.Player
import com.tusdk.pulse.Producer
import com.tusdk.pulse.editor.*
import com.tusdk.pulse.editor.clips.VideoFileClip
import com.tusdk.pulse.editor.effects.TusdkParticleEffect
import com.tusdk.pulse.editor.effects.TusdkParticleEffect.PropertyHolder.PosInfo
import kotlinx.android.synthetic.main.api_activity.*
import kotlinx.android.synthetic.main.api_activity.lsq_api_displayView
import kotlinx.android.synthetic.main.api_activity.lsq_player_duration
import kotlinx.android.synthetic.main.include_sticker_view.*
import kotlinx.android.synthetic.main.include_title_layer.*
import kotlinx.android.synthetic.main.particle_activity.*
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.base.BaseActivity
import org.lasque.tusdkeditoreasydemo.base.VideoItem
import kotlinx.android.synthetic.main.particle_activity.seekBar
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.DateHelper
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlHelper
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import org.lasque.tusdkeditoreasydemo.album.AlbumInfo
import org.lasque.tusdkeditoreasydemo.album.AlbumItemType
import org.lasque.tusdkeditoreasydemo.base.DraftItem
import org.lasque.tusdkeditoreasydemo.base.OnItemClickListener
import org.lasque.tusdkeditoreasydemo.base.views.ColorView
import org.lasque.tusdkeditoreasydemo.utils.Constants
import java.io.File
import java.util.*
import java.util.concurrent.*
import kotlinx.android.synthetic.main.api_activity.lsq_editor_cut_load as lsq_editor_cut_load1
import kotlinx.android.synthetic.main.api_activity.lsq_editor_cut_load_parogress as lsq_editor_cut_load_parogress1

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/11/11  10:31
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class ParticleActivity : BaseActivity() {

    private var mEditor: VideoEditor = VideoEditor()

    private val mThreadPool: ExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var mPlayer: VideoEditor.Player? = null

    private var mCurrentState = Player.State.kREADY

    private var isSeekBarTouch = false

    private var mParticleEffect: Effect? = null

    private var particleList: ArrayList<PosInfo> = ArrayList()

    private var mCurrentDuration = 0L

    private var mParticlePropertyBuilder: TusdkParticleEffect.PropertyBuilder = TusdkParticleEffect.PropertyBuilder()

    private var mParticlePosBuilder: TusdkParticleEffect.ParticlePosPropertyBuilder = TusdkParticleEffect.ParticlePosPropertyBuilder()

    private var mVideoItem: VideoItem? = null

    private var mVideoLayer: ClipLayer? = null

    private var mAudioLayer: ClipLayer? = null

    private var mParticleId = 300

    private var mMaxDuration = 1L

    private var mCurrentCode = ""

    private var mVideoRect: Rect? = null

    private var mParticleSize = 0f

    private var mTouchStartTime = 0L

    private var mLastTouchTime = 0L

    private var mSemaphore: Semaphore? = null

    private var mOutputWidth = 800

    private var mOutputHeight = 800

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.particle_activity)

        lsq_back.setOnClickListener {
            finish()
        }

        lsq_title.setText(FunctionType.ParticleEffect.mTitleId)

        lsq_particle_displayView.init(Engine.getInstance().mainGLContext)


        val modelPath = intent.getStringExtra(DraftItem.Draft_Path_Key)

        if (TextUtils.isEmpty(modelPath)) {
            mThreadPool.execute {
                val openConfig = VideoEditor.OpenConfig()
                openConfig.width = mOutputWidth
                openConfig.height = mOutputHeight
                if (!mEditor.create(openConfig)) {
                    TLog.e("Editor Create failed")
                }
            }

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


        val particleAdapter = ParticleAdapter(Constants.PARTICLE_CODES.toMutableList(), this)
        particleAdapter.setOnItemClickListener(object : OnItemClickListener<String, ParticleAdapter.ParticleViewHolder> {
            override fun onItemClick(pos: Int, holder: ParticleAdapter.ParticleViewHolder, item: String) {
                mCurrentCode = item
                particleAdapter.setCurrentPosition(pos)
            }
        })
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        lsq_particle_list.layoutManager = layoutManager
        lsq_particle_list.adapter = particleAdapter
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
        lsq_particle_play.setOnClickListener {
            if (mCurrentState != Player.State.kPLAYING) {
                playerPlay()
            } else {
                playerPause()
            }
        }
        lsq_particle_layer.setOnTouchListener { view, motionEvent ->
//            lsq_particle_layer.performClick()
            if (TextUtils.isEmpty(mCurrentCode)) return@setOnTouchListener false
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    var res: Future<Boolean> = mThreadPool.submit(Callable<Boolean> {
                        if (TextUtils.isEmpty(mCurrentCode)) return@Callable false
                        mPlayer!!.lock()
                        val particleConfig = Config()
                        particleConfig.setString(TusdkParticleEffect.CONFIG_NAME, mCurrentCode)
                        mParticleEffect = Effect(mEditor.context, TusdkParticleEffect.TYPE_NAME)
                        mParticleEffect?.setConfig(particleConfig)
                        val ret = mVideoItem!!.mVideoClip.effects().add(mParticleId++, mParticleEffect)
                        mParticlePropertyBuilder!!.holder.trajectory.clear()
                        mParticlePropertyBuilder!!.holder.begin = mCurrentDuration
                        mParticlePropertyBuilder!!.holder.end = mMaxDuration
                        mParticlePropertyBuilder!!.holder.tint = lsq_particle_color_bar.selectColor
                        mParticleEffect?.setProperty(TusdkParticleEffect.PROP_PARAM, mParticlePropertyBuilder!!.makeProperty())
                        mTouchStartTime = System.currentTimeMillis();
//                        mSemaphore = Semaphore(0)
                        if (!mEditor!!.build()) {

                        }
                        mPlayer!!.unlock()
                        mPlayer!!.seekTo(mParticlePropertyBuilder!!.holder.begin)
                        mPlayer!!.play()
                        runOnUiThread {
                            lsq_particle_play.setImageResource(R.mipmap.edit_ic_pause)
                        }
                        ret
                    })
                    res.get()
                }
                MotionEvent.ACTION_MOVE -> {

                    var res: Future<Boolean> = mThreadPool.submit(Callable<Boolean> {
                        if (TextUtils.isEmpty(mCurrentCode)) return@Callable false
                        val point = convertedPoint(motionEvent.x, motionEvent.y)
                        mParticlePropertyBuilder!!.holder.trajectory.add(point)
                        mParticlePosBuilder.posX = point.x
                        mParticlePosBuilder.posY = point.y
                        mParticlePosBuilder.tint = mParticlePropertyBuilder!!.holder.tint
                        mParticlePosBuilder.scale = mParticlePropertyBuilder!!.holder.scale
                        var ret = mParticleEffect!!.setProperty(TusdkParticleEffect.PROP_PARTICLE_POS, mParticlePosBuilder.makeProperty())
                        ret
                    })

                    res.get()
                    mLastTouchTime = System.currentTimeMillis()
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    var res : Future<Boolean> = mThreadPool.submit(Callable<Boolean> {
                        if (TextUtils.isEmpty(mCurrentCode)) return@Callable false
                        if (mPlayer!!.pause()) {
                        }
                        mParticlePropertyBuilder!!.holder.end = mCurrentDuration
                        var ret = mParticleEffect!!.setProperty(TusdkParticleEffect.PROP_PARAM, mParticlePropertyBuilder!!.makeProperty())
                        mParticlePropertyBuilder!!.holder.trajectory.clear()
                        runOnUiThread {
                            lsq_particle_play.setImageResource(R.mipmap.edit_ic_play)
                        }

                        ret
                    })
                    res.get()
                }
            }
            return@setOnTouchListener true
        }

        lsq_particle_color_bar.setOnColorChangeListener(object : ColorView.OnColorChangeListener {
            override fun changeColor(colorId: Int) {
//                mThreadPool?.execute {
//                    if (mParticlePropertyBuilder == null) return@execute
//                    mParticlePropertyBuilder!!.holder.tint = lsq_particle_color_bar.selectColor
//                    mParticleEffect!!.setProperty(TusdkParticleEffect.PROP_PARAM, mParticlePropertyBuilder!!.makeProperty())
//                }
            }

            override fun changePosition(percent: Float) {

            }

        })

        lsq_particle_size_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (!p2) return
                mParticleSize = p1.toFloat() / 100f
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
//                mThreadPool.execute {
//                    if (mParticlePropertyBuilder == null) return@execute
//
//                    mParticlePropertyBuilder!!.holder.scale = mParticleSize.toDouble()
//                    mParticleEffect!!.setProperty(TusdkParticleEffect.PROP_PARAM, mParticlePropertyBuilder!!.makeProperty())
//                }
            }

        })

        lsq_particle_displayView.viewTreeObserver.addOnGlobalLayoutListener {
            mVideoRect = lsq_particle_displayView.getInteractionRect(mOutputWidth, mOutputHeight)
            lsq_particle_layer.layoutParams.width = mVideoRect!!.width()
            lsq_particle_layer.layoutParams.height = mVideoRect!!.height()
//            lsq_particle_layer.y = mVideoRect!!.top.toFloat()
//            lsq_particle_layer.x = mVideoRect!!.left.toFloat()
        }
    }

    private fun saveVideo() {
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
            return
        }
        setEnable(false)
        if (!producer.start()) {
            TLog.e("[Error] EditorProducer Start failed")

        }
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

            val currentModule = FunctionType.ParticleEffect

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


    private fun playerPlay() {
        mThreadPool.execute {
            mPlayer!!.play()
            runOnUiThread {
                lsq_particle_play.setImageResource(R.mipmap.edit_ic_pause)
            }
        }
    }

    private fun playerPause() {
        mThreadPool.execute {
            mPlayer!!.pause()
            runOnUiThread {
                lsq_particle_play.setImageResource(R.mipmap.edit_ic_play)
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
        if (!mEditor.build()) {

        }
        mPlayer = mEditor.newPlayer()
        mPlayer?.setListener { state, ts ->
            mCurrentState = state
            if (state == Player.State.kDO_PLAY || state == Player.State.kDO_PAUSE) {
                runOnUiThread {
                    val durationMS = mPlayer!!.duration
                    mMaxDuration = durationMS
                    seekBar.max = durationMS.toInt()
                }
                if (state == Player.State.kDO_PAUSE) {
//                    mSemaphore?.release()
                }
            } else if (state == Player.State.kEOS) {
                runOnUiThread {
                    lsq_particle_play.setImageResource(R.mipmap.edit_ic_play)
                    mThreadPool.execute {
                        mPlayer!!.previewFrame(0)
                    }
                }
            } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW) {
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

        if (!mPlayer!!.open()) {
            TLog.e("Editor Player Open failed")
        }

        runOnUiThread {
            lsq_particle_displayView.attachPlayer(mPlayer)
            val dur = mPlayer!!.duration.toInt()
            seekBar.max = dur
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
        }
        mPlayer!!.previewFrame(0)
    }

    private fun restoreLayer() {
        val videoLayer = mEditor.videoComposition().allLayers[11] as ClipLayer
        val audioLayer = mEditor.audioComposition().allLayers[11] as ClipLayer

        val videoId = videoLayer.allClips.keys.first()
        val videoClip = videoLayer.allClips.values.first()
        val audioClip = audioLayer.allClips.values.first()

        val videoPath = videoClip.config.getString(VideoFileClip.CONFIG_PATH)

        mVideoItem = VideoItem(videoPath, videoId.toLong(), videoClip, audioClip)

        mParticleId = videoClip.effects().all.keys.last() + 1

        val duration = mVideoItem!!.mVideoClip.streamInfo.duration
        mVideoLayer = videoLayer
        mAudioLayer = audioLayer
        mMaxDuration = duration
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
    }

    private fun convertedPoint(x: Float, y: Float): PosInfo {
        val topOffset = if (mVideoRect!!.top != 0) {
            mVideoRect!!.top
        } else {
            TuSdkViewHelper.locationInWindowTop(lsq_particle_layer)
        }
        val previewSize: TuSdkSize = TuSdkSize(lsq_particle_layer.measuredWidth, lsq_particle_layer.measuredHeight)
        val screenSize = previewSize

        val previewRectF = RectF(
                0f,
                0f,
                previewSize.width.toFloat(),
                previewSize.height.toFloat()
        )
        if (!previewRectF.contains(x, y)) {
            return PosInfo(-1, (-1).toDouble(), (-1).toDouble())
        }

        var pointX = -1f
        var pointY = -1f

        pointX = x / previewSize.width.toFloat()
        pointY = (y) / previewSize.height.toFloat()

        return PosInfo(System.currentTimeMillis() - mTouchStartTime, pointX.toDouble(), pointY.toDouble())
    }

    override fun onDestroy() {
        super.onDestroy()
        lsq_particle_displayView.release()
        val res = mThreadPool.submit(Callable<Boolean> {

            mPlayer?.close()
            TLog.e("release")
            mEditor.destroy()
            mThreadPool.shutdownNow()
             true

        })
        res.get()
    }


}