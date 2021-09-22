/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2021/8/11$ 14:39$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.graphics.Color
import android.media.*
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Base64
import android.view.View
import android.widget.TextView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.include_title_layer.*
import kotlinx.android.synthetic.main.voice_api_effect_activity.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.anko.toast
import org.lasque.tusdkeditoreasydemo.base.BaseActivity
import org.lasque.tusdkeditoreasydemo.utils.PCMUtils
import org.lasque.tusdkpulse.core.TuSdk
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.ThreadHelper
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/8/11  14:39
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class VoiceAPIEffectActivity : BaseActivity() {

    companion object{
        private var mTypeMap = object : HashMap<Int, Int>() {
            init {
                put(R.id.lsq_voice_effect_man_1,5)
                put(R.id.lsq_voice_effect_man_2,6)
                put(R.id.lsq_voice_effect_man_3,7)
                put(R.id.lsq_voice_effect_man_4,8)
                put(R.id.lsq_voice_effect_man_5,9)

                put(R.id.lsq_voice_effect_woman_1,0)
                put(R.id.lsq_voice_effect_woman_2,1)
                put(R.id.lsq_voice_effect_woman_3,2)
                put(R.id.lsq_voice_effect_woman_4,3)
                put(R.id.lsq_voice_effect_woman_5,4)
            }
        }
    }

    val JSON: MediaType = "application/json; charset=UTF-8".toMediaType()

    data class TransVoiceData(val sessionId : String,val targetVoiceId : Int,val audio : String)

    data class TransVoiceResponseData(val targetAudio : String,val errMsg : String = "")

    private var mVCApiUrl = "http://59.111.57.55:8889/vc";

    private var mCurrentBase64Code = "";

    private var mCurrentType = 0

    private var mCurrentOriginAudioPath = ""

    private var mCurrentVCAudioPath = ""

    private fun getVoiceFile(audioFile : String,targetVoiceId : Int,sessionId : String) : Flow<String> = flow {
        val voiceData = TransVoiceData(sessionId,targetVoiceId,audioFile)

        val requestGson = Gson()

        val requestString = requestGson.toJson(voiceData)

        val client = OkHttpClient.Builder().callTimeout(10,TimeUnit.SECONDS).connectTimeout(15,TimeUnit.SECONDS).readTimeout(10,TimeUnit.SECONDS).build()
        val requestBody = requestString.toRequestBody(JSON)

        val request = Request.Builder().url(mVCApiUrl).header("Content-Type", "application/json").post(requestBody).build()

        try {
            val response = client.newCall(request).execute()
            TLog.e("Response Message ${response.message}")

            if (response.code == 503 || response.body == null){
                emit("")
            } else {

                val transResponse = requestGson.fromJson<TransVoiceResponseData>(response.body!!.string(),TransVoiceResponseData::class.java)

                if (TextUtils.isEmpty(transResponse.targetAudio)){

                    emit("")

                } else{
                    val saveDirs = File(TuSdk.getAppDownloadPath(),"voice")

                    saveDirs.mkdirs()

                    val saveFile = File(saveDirs,"${System.currentTimeMillis()}.pcm")

                    val outputStream = FileOutputStream(saveFile)

                    val bufferOutputStream = BufferedOutputStream(outputStream)

                    val inputBuffer = java.util.Base64.getDecoder().decode(transResponse.targetAudio)

                    bufferOutputStream.write(inputBuffer)

                    bufferOutputStream.close()

                    emit(saveFile.absolutePath)
                }
            }

        } catch (e : java.lang.Exception){
            TLog.e(e)
            emit("")
        }
    }


    private fun audioPlay(audioPath : String) {
        val channels =
            AudioFormat.CHANNEL_OUT_MONO

        val bufferSize = AudioTrack.getMinBufferSize(
            22050,
            channels,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            22050,
            channels,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        val audioTrackJob = GlobalScope.launch {
            audioTrack.play()

            var buffer = ByteArray(bufferSize)

            val voiceFile = File(audioPath)
            val inputStream = FileInputStream(voiceFile)
            var len = 0

            while (inputStream.read(buffer).also { len = it } != -1) {
                audioTrack.write(buffer, 0, len)
            }

            audioTrack.stop()
            audioTrack.release()

        }

        audioTrackJob.start()
    }

    private var mBlockByteLength = 0

    private var mBufferSize = 0

    private var mRecordingStart = 0L

    private var mAudioRecord : AudioRecord? = null

    private fun read(record: AudioRecord, bys: ByteArray): Int {

        var result = 0
        val recBufferBytePtr = 0
        try {
            val buffer = ByteArray(bys.size)
            result = record.read(buffer, recBufferBytePtr, mBlockByteLength)
            PCMUtils.amplifyPCMData(buffer,buffer.size,bys,22.05, PCMUtils.factor.toFloat())
        } catch (e: Exception) {
        }
        if (result < 0) {
        }
        return result
    }

    private fun audioRecord(function : (duration : Int)->Unit) : String{
        val audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        val sampleRate = 22050
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioBitWidth = AudioFormat.ENCODING_PCM_16BIT
        val channelCount = 1
        val bitWidth = 16

        mBlockByteLength = 1024 * channelCount * (bitWidth / 8)

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioBitWidth)
        val inputBufferSize = minBufferSize * 4
        val frameSizeInBytes = channelCount * 2
        mBufferSize = inputBufferSize / frameSizeInBytes * frameSizeInBytes

        if (mBufferSize < 1) {
            return ""
        }

        mAudioRecord =
            AudioRecord(audioSource, sampleRate, channelConfig, audioBitWidth, mBufferSize)



        if(mAudioRecord!!.state != AudioRecord.STATE_INITIALIZED){
            return ""
        }

        val tempSaveDirs = File(TuSdk.getAppTempPath(),"record")

        tempSaveDirs.mkdirs()

//        val outputSaveDir = File(TuSdk.getAppTempPath(),"outputPcm")
//
//        outputSaveDir.mkdirs()
//
//        val mp3TempDirs = File(TuSdk.getAppTempPath(),"mp3temp")
//
//        mp3TempDirs.mkdirs()

        val tempSaveFile = File(tempSaveDirs,"record_${System.currentTimeMillis()}.pcm")

//        val outputSaveFile = File(outputSaveDir,"output_pcm_${System.currentTimeMillis()}.pcm")

//        val tempMp3File = File(mp3TempDirs,"temp_${System.currentTimeMillis()}.mp3")

        val fileOutputStream = FileOutputStream(tempSaveFile)

        mAudioRecord!!.startRecording()

        val recordStartTime = System.currentTimeMillis();

        while (mAudioRecord!!.recordingState == AudioRecord.RECORDSTATE_RECORDING){
            val buffer = ByteArray(mBufferSize)
            val total = read(mAudioRecord!!,buffer)

            fileOutputStream.write(buffer,0,total)

            val current = System.currentTimeMillis();
            val duration = (current - recordStartTime) / 1000
            runOnUiThread {
                function(duration.toInt())
            }
            if (duration >= 10){
                mAudioRecord!!.stop()
            }

        }

        fileOutputStream.close()

//        PCMUtils.pcmToMp3(tempSaveFile,tempMp3File)

//        PCMUtils.mp3ToPcm(tempMp3File,outputSaveFile)

        return tempSaveFile.absolutePath;
    }

    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.voice_api_effect_activity)
        lsq_title.setText("音色转换")

        val typeClickListener = object : View.OnClickListener{
            override fun onClick(v: View?) {
                for((id,_)in mTypeMap){
                    val view = findViewById<TextView>(id)
                    if (id == v!!.id){
                        view.setTextColor(Color.RED)
                        mCurrentType = mTypeMap.get(id)!!
                    } else {
                        view.setTextColor(Color.BLACK)
                    }
                }
            }

        }

        for((id,_)in mTypeMap){
            val view = findViewById<TextView>(id)
            view.setOnClickListener(typeClickListener)
        }

        lsq_voice_effect_record.setOnClickListener {
            if (isRecording){
                return@setOnClickListener
            }
            val recordJob = GlobalScope.launch {
                val outputPath = audioRecord {
                    lsq_voice_effect_record.setText("录音中....")
                    lsq_voice_effect_record_duration.setText(it.toString())
                }

                if (TextUtils.isEmpty(outputPath)){
                    runOnUiThread {
                        lsq_voice_effect_original_play.visibility = View.GONE
                    }
                } else {
                    mCurrentOriginAudioPath = outputPath
                    runOnUiThread {
                        lsq_voice_effect_original_play.visibility = View.VISIBLE
                    }
                }
                runOnUiThread {
                    lsq_voice_effect_record.setText("录音")
                }
                isRecording = false
            }

            recordJob.start()
            isRecording = true

        }

        lsq_voice_effect_commit.setOnClickListener {
            val commitJob = GlobalScope.launch {

                val fileBytes = Files.readAllBytes(File(mCurrentOriginAudioPath).toPath())
                mCurrentBase64Code = java.util.Base64.getEncoder().encodeToString(fileBytes)
                getVoiceFile(mCurrentBase64Code,mCurrentType,"vc_abcd1234").collect {

                    runOnUiThread {
                        if (TextUtils.isEmpty(it)){
                            lsq_voice_effect_play.visibility = View.GONE
                            lsq_loading_mask_title.setText("生成失败,请稍后再试")
                        } else {
                            lsq_voice_effect_play.visibility = View.VISIBLE
                            lsq_loading_mask_title.setText("生成成功,可以试听")
                            mCurrentVCAudioPath = it
                        }

                        ThreadHelper.postDelayed({lsq_loading_mask.visibility = View.GONE},2000)
                    }
                }
            }

            lsq_loading_mask_title.setText("生成中.....")
            lsq_loading_mask.visibility = View.VISIBLE
            commitJob.start()
        }

        lsq_voice_effect_original_play.setOnClickListener {
            if (TextUtils.isEmpty(mCurrentOriginAudioPath)){return@setOnClickListener}
            val originalPlayJob = GlobalScope.launch {
                audioPlay(mCurrentOriginAudioPath)
            }
            originalPlayJob.start()
        }

        lsq_voice_effect_play.setOnClickListener {
            if (TextUtils.isEmpty(mCurrentVCAudioPath)){
                TLog.e("mCurrentVCAudioPath is empty")
                return@setOnClickListener
            }
            TLog.e("current vc path ${mCurrentVCAudioPath}")
            val playJob = GlobalScope.launch {
                audioPlay(mCurrentVCAudioPath)
                runOnUiThread {
                    toast("播放结束")
                }
            }
            playJob.start()
            toast("开始播放....")
        }


    }

}