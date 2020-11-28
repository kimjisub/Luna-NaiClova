package com.lunacoding.naiclova

import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.service.voice.AlwaysOnHotwordDetector
import android.service.voice.AlwaysOnHotwordDetector.EventPayload
import android.service.voice.VoiceInteractionService
import android.util.Log
import com.lunacoding.naiclova.HotwordService
import java.util.*

class HotwordService : VoiceInteractionService() {
    private val mIBinder: IBinder = MyBinder()

    internal inner class MyBinder : Binder() {
        val service: HotwordService
            get() = this@HotwordService
    }

    // =========================================================================================
    private val LOG_TAG = "HotwordService"
    var locale = Locale.KOREAN
    var hotwordDetector: AlwaysOnHotwordDetector? = null
    override fun onCreate() {
        Log.i(LOG_TAG, "onCreate")
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(LOG_TAG, "onStartCommand")
        val args = Bundle()
        startSession(args)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    @JvmOverloads
    fun startSession(args: Bundle?, flags: Int = 0) {
        showSession(args, flags)
    }

    override fun onReady() {
        Log.i(LOG_TAG, "onReady")
        hotwordDetector = createAlwaysOnHotwordDetector("안녕", locale, callback)
        super.onReady()
    }

    var callback: AlwaysOnHotwordDetector.Callback = object : AlwaysOnHotwordDetector.Callback() {
        override fun onRecognitionResumed() {
            if (onHotwordListener != null) onHotwordListener!!.onRecognitionResumed()
        }

        override fun onRecognitionPaused() {
            if (onHotwordListener != null) onHotwordListener!!.onRecognitionPaused()
        }

        override fun onError() {
            if (onHotwordListener != null) onHotwordListener!!.onError()
        }

        override fun onDetected(eventPayload: EventPayload) {
            if (onHotwordListener != null) onHotwordListener!!.onDetected(eventPayload)
        }

        override fun onAvailabilityChanged(status: Int) {
            if (onHotwordListener != null) onHotwordListener!!.onAvailabilityChanged(status)
        }
    }

    interface OnHotwordListener {
        fun onRecognitionResumed()
        fun onRecognitionPaused()
        fun onError()
        fun onDetected(eventPayload: EventPayload?)
        fun onAvailabilityChanged(status: Int)
    }

    var onHotwordListener: OnHotwordListener? = object : OnHotwordListener {
        override fun onRecognitionResumed() {}
        override fun onRecognitionPaused() {}
        override fun onError() {}
        override fun onDetected(eventPayload: EventPayload?) {}
        override fun onAvailabilityChanged(status: Int) {}
    }
}