package com.lunacoding.naiclova.recognizer

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.annotation.WorkerThread
import com.lunacoding.naiclova.R
import com.naver.speech.clientapi.*
import com.naver.speech.clientapi.SpeechConfig.EndPointDetectType

class NaverRecognizer(context: Context?, private val mHandler: Handler, clientId: String?) :
    SpeechRecognitionListener {
    var speechRecognizer: SpeechRecognizer? = null
    fun recognize() {
        try {
            speechRecognizer!!.recognize(
                SpeechConfig(
                    SpeechConfig.LanguageType.KOREAN,
                    EndPointDetectType.AUTO
                )
            )
        } catch (e: SpeechRecognitionException) {
            e.printStackTrace()
        }
    }

    @WorkerThread
    override fun onInactive() {
        Log.d(TAG, "Event occurred : Inactive")
        val msg = Message.obtain(mHandler, R.id.clientInactive)
        msg.sendToTarget()
    }

    @WorkerThread
    override fun onReady() {
        Log.d(TAG, "Event occurred : Ready")
        val msg = Message.obtain(mHandler, R.id.clientReady)
        msg.sendToTarget()
    }

    @WorkerThread
    override fun onRecord(speech: ShortArray) {
        Log.d(TAG, "Event occurred : Record")
        val msg = Message.obtain(mHandler, R.id.audioRecording, speech)
        msg.sendToTarget()
    }

    @WorkerThread
    override fun onPartialResult(result: String) {
        Log.d(TAG, "Partial Result!! ($result)")
        val msg = Message.obtain(mHandler, R.id.partialResult, result)
        msg.sendToTarget()
    }

    @WorkerThread
    override fun onEndPointDetected() {
        Log.d(TAG, "Event occurred : EndPointDetected")
    }

    @WorkerThread
    override fun onResult(result: SpeechRecognitionResult) {
        Log.d(TAG, "Final Result!! (" + result.results[0] + ")")
        val msg = Message.obtain(mHandler, R.id.finalResult, result)
        msg.sendToTarget()
    }

    @WorkerThread
    override fun onError(errorCode: Int) {
        Log.d(TAG, "Error!! (" + Integer.toString(errorCode) + ")")
        val msg = Message.obtain(mHandler, R.id.recognitionError, errorCode)
        msg.sendToTarget()
    }

    @WorkerThread
    override fun onEndPointDetectTypeSelected(epdType: EndPointDetectType) {
        Log.d(
            TAG,
            "EndPointDetectType is selected!! (" + Integer.toString(epdType.toInteger()) + ")"
        )
        val msg = Message.obtain(mHandler, R.id.endPointDetectTypeSelected, epdType)
        msg.sendToTarget()
    }

    companion object {
        private val TAG = NaverRecognizer::class.java.simpleName
    }

    init {
        try {
            speechRecognizer = SpeechRecognizer(context, clientId)
        } catch (e: SpeechRecognitionException) {
            // 예외가 발생하는 경우는 아래와 같습니다.
            //   1. activity 파라미터가 올바른 MainActivity의 인스턴스가 아닙니다.
            //   2. AndroidManifest.xml에서 package를 올바르게 등록하지 않았습니다.
            //   3. package를 올바르게 등록했지만 과도하게 긴 경우, 256바이트 이하면 좋습니다.
            //   4. clientId가 null인 경우
            //
            // 개발하면서 예외가 발생하지 않았다면 실서비스에서도 예외는 발생하지 않습니다.
            // 개발 초기에만 주의하시면 됩니다.
            e.printStackTrace()
        }
        speechRecognizer!!.setSpeechRecognitionListener(this)
    }
}