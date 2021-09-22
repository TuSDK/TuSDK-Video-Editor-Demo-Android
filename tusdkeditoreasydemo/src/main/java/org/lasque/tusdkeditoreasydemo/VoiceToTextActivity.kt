/**
 *  TuSDK
 *  android-ve-demo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2021/8/4$ 16:07$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.include_title_layer.*
import kotlinx.android.synthetic.main.voice_to_text_activity.*
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
import org.lasque.tusdkeditoreasydemo.base.BaseActivity
import org.lasque.tusdkpulse.core.TuSdk
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.utils.FileHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.ThreadHelper
import java.io.File
import java.io.FileInputStream

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * android-ve-demo
 *
 * @author        H.ys
 * @Date        2021/8/4  16:07
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class VoiceToTextActivity : BaseActivity(){

    private var mCurrentType = 0;

    private final val TAG = "MainActivity"

    private var mVoiceApiUrl = "http://59.111.57.55:8888/tts"

    val JSON: MediaType = "application/json; charset=UTF-8".toMediaType()

    private var mCurrentVoiceSavePath = ""

    data class VoiceData(val sessionId: String, val voiceName: Int, val text: String)

    private fun getVoiceFile(text : String,type : Int,sessionId : String) : Flow<String> = flow {
        val voiceData = VoiceData(sessionId,type,text)

        val requestGson = Gson()

        val requestString = requestGson.toJson(voiceData)

        val client = OkHttpClient()
        val requestBody = requestString.toRequestBody(JSON)

        val request = Request.Builder().url(mVoiceApiUrl).header("Content-Type", "application/json").post(requestBody).build()

        val response = client.newCall(request).execute()

        TLog.e("Response Message ${response.message}")

        if (response.code == 503 || response.body == null){

            emit("")
        } else {
            val inputStream = response.body!!.byteStream()

            val saveDirs = File(TuSdk.getAppDownloadPath(),"voice")

            saveDirs.mkdirs()

            val saveFile = File(saveDirs,"${System.currentTimeMillis()}.pcm")

            FileHelper.saveFile(saveFile,inputStream)

            emit(saveFile.absolutePath)
        }
    }


    /** -----------------------------------------------View-------------------------------- */

    companion object{
        private var mTypeMap = object : HashMap<Int, Int>() {
            init {
                put(R.id.lsq_voice_to_text_type_man,0)
                put(R.id.lsq_voice_to_text_type_woman,1)
                put(R.id.lsq_voice_to_text_type_boy,2)
                put(R.id.lsq_voice_to_text_type_girl,3)
                put(R.id.lsq_voice_to_text_type_xiao_xin,4)
                put(R.id.lsq_voice_to_text_type_psyduck,5)
                put(R.id.lsq_voice_to_text_type_siri,6)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.voice_to_text_activity)

        lsq_title.setText("文字转语音")

        val voiceTypeOnClickListener = object : View.OnClickListener{
            override fun onClick(v: View?) {
                for (id in mTypeMap.keys){
                    val view = findViewById<TextView>(id)
                    if (id == v!!.id){
                        view.setBackgroundResource(R.drawable.voice_to_text_type_select_bg)
                        view.setTextColor(Color.RED)
                        mCurrentType = mTypeMap.get(id)!!
                    } else {
                        view.setBackgroundColor(Color.WHITE)
                        view.setTextColor(Color.BLACK)
                    }
                }
            }
        }

        lsq_voice_to_text_commit.setOnClickListener {
            lsq_loading_mask_title.setText("生成中.....")
            lsq_loading_mask.visibility = View.VISIBLE
            val text = lsq_voice_to_text_edit.text.toString()
            val postJob = GlobalScope.launch {
                getVoiceFile(text,mCurrentType,"abcd1234").collect {
                    val isSuccess = !TextUtils.isEmpty(it)
                    runOnUiThread {
                        if (isSuccess){
                            mCurrentVoiceSavePath = it
                            lsq_loading_mask_title.setText("生成完成，请点试听按钮")
                            lsq_voice_to_text_play.visibility = View.VISIBLE
                        } else {
                            lsq_loading_mask_title.setText("生成失败，请稍后再试")
                            lsq_voice_to_text_play.visibility = View.GONE
                        }

                        ThreadHelper.postDelayed({
                                                 lsq_loading_mask.visibility = View.GONE
                        },2000)
                    }
                }
            }

            postJob.start()

        }

        lsq_voice_to_text_play.setOnClickListener {
            if (TextUtils.isEmpty(mCurrentVoiceSavePath)) return@setOnClickListener
            audioPlay(mCurrentVoiceSavePath)
        }

        for (id in mTypeMap.keys){
            val view = findViewById<TextView>(id)
            view.setOnClickListener(voiceTypeOnClickListener)
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

}