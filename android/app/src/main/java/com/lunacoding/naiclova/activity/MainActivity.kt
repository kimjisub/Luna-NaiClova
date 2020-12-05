package com.lunacoding.naiclova.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.service.voice.AlwaysOnHotwordDetector.EventPayload
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.lunacoding.naiclova.HotwordService
import com.lunacoding.naiclova.HotwordService.MyBinder
import com.lunacoding.naiclova.HotwordService.OnHotwordListener
import com.lunacoding.naiclova.R
import com.lunacoding.naiclova.firestore.Command
import com.lunacoding.naiclova.manager.MessageItem
import com.lunacoding.naiclova.manager.MessageManager
import com.lunacoding.naiclova.recognizer.AudioWriterPCM
import com.lunacoding.naiclova.view.KeywordView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    // Message Manager
    val mm: MessageManager by lazy { MessageManager(this, LL_list) }


    // STT

    private val sttIntent: Intent by lazy {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        intent
    }
    private val stt: SpeechRecognizer by lazy { SpeechRecognizer.createSpeechRecognizer(this) }

    enum class SttStatus {
        UNAVAILABLE, READY, LISTENING
    }

    private var sttStatus: SttStatus = SttStatus.UNAVAILABLE
        set(value) {
            if (value != field)
                when (value) {
                    SttStatus.UNAVAILABLE -> {
                    }
                    SttStatus.READY -> {
                        circle.alpha = 1f
                        circle.scaleX = 1f
                        circle.scaleY = 1f
                        circle.animate()
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .setDuration(200)
                            .alpha(0f)
                            .scaleX(0.3f)
                            .scaleY(0.3f)

                    }
                    SttStatus.LISTENING -> {
                        SV_scrollView.post { SV_scrollView.smoothScrollBy(0, 10000) }

                        circle.scaleX = 0.3f
                        circle.scaleY = 0.3f
                        circle.alpha = 0f
                        circle.scaleX = 0.3f
                        circle.scaleY = 0.3f
                        circle.animate()
                            .setDuration(200)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                    }
                }

            field = value
        }


    enum class TypeModes {
        TEXT, MIC, KEYWORD
    }

    private var typeMode: TypeModes = TypeModes.MIC
        set(value) {
            field = value
            when (field) {
                TypeModes.TEXT -> {
                    send_mic.animate().alpha(0f).scaleX(0.7f).scaleY(0.7f).setDuration(300)
                    send_text.animate().alpha(1f).scaleX(1f).scaleY(1f)
                }
                TypeModes.MIC -> {
                    send_mic.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    send_text.animate().alpha(0f).scaleX(0.7f).scaleY(0.7f)
                }
            }
        }

    private var writer: AudioWriterPCM? = null

    // =========================================================================================
    var tts: TextToSpeech? = null

    // firebase
    var firestore = FirebaseFirestore.getInstance()
    var firebase = FirebaseDatabase.getInstance().reference

    //
    var mapData: MutableMap<String, Command> = HashMap()
    var waitingKeys = ArrayList<String?>()

    // ========================================================================================= Start
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        TedPermission.with(this)
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    start()
                }

                override fun onPermissionDenied(deniedPermissions: List<String>) {
                    finish()
                }
            })
            .setRationaleMessage("음성인식을 위해서 오디오 녹음 권한이 필요합니다.\n해당 권한에 대한 허가를 받습니다.")
            .setDeniedMessage("오디오 녹음 장치에 액세스해야합니다.\n[설정] > [권한]에서 권한을 켜십시오.")
            .setPermissions(Manifest.permission.RECORD_AUDIO)
            .check()
    }

    fun start() {
        //startService (Intent(this@MainActivity, HotwordService::class.java))
        //bindService(Intent(this@MainActivity, HotwordService::class.java), sconn, BIND_AUTO_CREATE)
        tts = TextToSpeech(applicationContext) { status: Int ->
            if (status != TextToSpeech.ERROR) {
                tts!!.language = Locale.KOREAN
            }
        }
        stt.setRecognitionListener(recognitionListener)

        send_group.setOnClickListener { v ->
            when (typeMode) {
                TypeModes.TEXT -> {
                    sendMsg(editOne.text.toString())
                    editOne.setText("")
                }
                TypeModes.MIC -> toggleVoice()
                TypeModes.KEYWORD -> {

                    if (AL_keywordSelected.size == 0) toggleVoice() else {
                        var str = ""
                        for (s in AL_keywordSelected) str += "$s "
                        sendMsg(str)
                        initKeyword()
                    }
                }
            }
        }

        editOne.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (sttStatus != SttStatus.LISTENING)
                    typeMode = if (s?.length != 0) TypeModes.TEXT else TypeModes.MIC
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        firestore.collection("command")
            .addSnapshotListener { snapshots: QuerySnapshot?, e: FirebaseFirestoreException? ->
                if (e != null) {
                    log("listen:error$e")
                    return@addSnapshotListener
                }
                for (dc in snapshots!!.documentChanges) {
                    val document = dc.document
                    val key = document.id
                    try {
                        var command: Command
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                command = Command(key, document)
                                log("New: $key")
                                log(command.toString())
                                mapData[key] = command
                                updateKeyword()
                            }
                            DocumentChange.Type.MODIFIED -> {
                                command = Command(key, document)
                                log("Modified: $key")
                                log(command.toString())
                                val oldCommand = mapData[key]
                                if (command.responseTimestamp!!.seconds != oldCommand!!.responseTimestamp!!.seconds) {
                                    for (waitingKey in waitingKeys) {
                                        if (waitingKey == key) {
                                            addOutputMessage(command.response!!)
                                            waitingKeys.remove(key)
                                        }
                                    }
                                }
                                mapData[key] = command
                                updateKeyword()
                            }
                            DocumentChange.Type.REMOVED -> {
                                command = Command(key, document)
                                log("Removed: $key")
                                log(command.toString())
                                mapData.remove(key)
                                updateKeyword()
                            }
                        }
                    } catch (e1: Exception) {
                        log("error: $key")
                        e1.printStackTrace()
                    }
                }
            }
    }

    var mService: HotwordService? = null
    var sconn: ServiceConnection = object : ServiceConnection {
        //서비스가 실행될 때 호출
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = (service as MyBinder).service
            mService!!.onHotwordListener = object : OnHotwordListener {
                override fun onRecognitionResumed() {}
                override fun onRecognitionPaused() {}
                override fun onError() {}
                override fun onDetected(eventPayload: EventPayload?) {
                    toggleVoice()
                }

                override fun onAvailabilityChanged(status: Int) {}
            }
        }

        //서비스가 종료될 때 호출
        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
        }
    }

    // ========================================================================================= Recognition
    fun sendMsg(msg: String) {
        addInputMessage(msg)
        var found = false
        var command: Command? = null
        for (i in 0 until mapData.size) {
            command = mapData[mapData.keys.toTypedArray()[i]]
            var num = 0
            for (keyword in command!!.keyword!!) if (msg.contains(keyword)) num++
            if (num == command.keyword!!.size) {
                found = true
                break
            }
        }
        if (found) {
            if (!command!!.responseStandby) {
                addOutputMessage(command.response!!)
                firebase.child("queue").push().setValue(command.command)
            } else {
                addOutputMessage("잠시만 기다려주세요...")
                firebase.child("queue").push().setValue(command.command)
                waitingKeys.add(command.key)
            }
        } else addOutputMessage("알아듣지 못했어요 ㅠㅠ")
    }

    // ========================================================================================= Keyword
    var AL_keywordView = ArrayList<KeywordView>()
    var AL_keywordSelected = ArrayList<String?>()
    fun initKeyword() {
        AL_keywordSelected.clear()
        updateKeyword()
    }

    fun updateKeyword() {
        AL_keywordView.clear()
        keyword_list.removeAllViews()
        val keywords = ArrayList<String?>()
        val keywords1 = ArrayList<String?>()
        val keywords2 = ArrayList<String?>()
        for (i in 0 until mapData.size) {
            val command = mapData[mapData.keys.toTypedArray()[i]]
            var doit = false
            if (AL_keywordSelected.size == 0) doit = true else {
                var num = 0
                for (keyword1 in command!!.keyword!!) {
                    for (keyword2 in AL_keywordSelected) if (keyword1 == keyword2) num++
                }
                if (num == AL_keywordSelected.size) doit = true
            }
            if (doit) {
                val num = 0
                for (keyword in command!!.keyword!!) {
                    var found = false
                    for (s in keywords) {
                        if (keyword == s) {
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        var found2 = false
                        for (s in AL_keywordSelected) if (keyword == s) {
                            found2 = true
                            break
                        }
                        if (found2) {
                            keywords.add(keyword)
                            keywords1.add(keyword)
                        } else {
                            keywords.add(keyword)
                            keywords2.add(keyword)
                        }
                    }
                }
            }
        }
        for (keyword in keywords1) addKeyword(keyword, true)
        for (keyword in keywords2) addKeyword(keyword, false)
    }

    fun addKeyword(keyword: String?, check: Boolean) {
        val keywordView = KeywordView(this@MainActivity)
        keywordView.text = keyword
        keywordView.isChecked = check
        keywordView.setOnEventListener(object : KeywordView.OnEventListener {
            override fun onViewClick(v: KeywordView) {
                v.isChecked = !v.isChecked
                updateCheckedKeyword()
            }

            override fun onViewLongClick(v: KeywordView?) {}
        })
        AL_keywordView.add(keywordView)
        keyword_list.addView(keywordView)
    }

    fun updateCheckedKeyword() {
        AL_keywordSelected.clear()
        for (keywordView in AL_keywordView) {
            if (keywordView.isChecked) AL_keywordSelected.add(keywordView.text)
        }
        updateKeyword()
    }

    // ========================================================================================= Add message

    fun addInputMessage(msg: String) {
        mm.newMessage(msg, MessageItem.Type.IN)
        //SV_scrollView.post { SV_scrollView.smoothScrollBy(0, 10000) }
    }

    fun addOutputMessage(msg: String) {
        mm.newMessage(msg, MessageItem.Type.OUT)
        SV_scrollView.post { SV_scrollView.smoothScrollBy(0, 10000) }
    }

    // =========================================================================================
    fun tts(msg: String?) {
        val utteranceId = this.hashCode().toString() + ""
        tts!!.speak(msg, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun toggleVoice() {
        if (sttStatus != SttStatus.LISTENING)
            stt.startListening(sttIntent)
        else
            stt.stopListening()
    }

    fun log(msg: String?) {
        Log.e("com.lunacoding.log", msg!!)
    }

    override fun onKeyDown(keycode: Int, event: KeyEvent): Boolean {
        when (keycode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> toggleVoice()
            KeyEvent.KEYCODE_VOLUME_UP -> toggleVoice()
            else -> super.onKeyDown(keycode, event)
        }
        return true
    }

    val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            log("onReadyForSpeech")
            sttStatus = SttStatus.LISTENING
        }

        override fun onBeginningOfSpeech() {
            log("onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
        }

        override fun onBufferReceived(buffer: ByteArray?) {
        }

        override fun onEndOfSpeech() {
            log("onEndOfSpeech")
            sttStatus = SttStatus.READY
        }

        override fun onError(error: Int) {
            log("onError")
            sttStatus = SttStatus.READY
        }

        override fun onResults(results: Bundle) {
            log("onResults")
            sttStatus = SttStatus.READY
            var result = ""
            val matches: ArrayList<String> =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
            for (str in matches) {
                result += str
            }

            log("onResults: $result")
            //
            // messageViewer!!.changeText(result)

            editOne.text = "".toEditable()
            sendMsg(result)
        }


        override fun onPartialResults(partialResults: Bundle) {
            var result = ""
            val matches: ArrayList<String> =
                partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
            for (str in matches) {
                result += str
            }

            editOne.text = result.toEditable()
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
        }

    }

    fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
}