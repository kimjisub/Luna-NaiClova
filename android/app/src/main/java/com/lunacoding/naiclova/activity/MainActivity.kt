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
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
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
import com.lunacoding.naiclova.databinding.ActivityMainBinding
import com.lunacoding.naiclova.firestore.Command
import com.lunacoding.naiclova.recognizer.AudioWriterPCM
import com.lunacoding.naiclova.view.KeywordView
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
	var b: ActivityMainBinding? = null
	private val sttIntent: Intent by lazy { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH) }
	private val stt: SpeechRecognizer by lazy { SpeechRecognizer.createSpeechRecognizer(this) }

	private var writer: AudioWriterPCM? = null
	var IVs_status: Array<ImageView> = arrayOf()
	private var mResult: String? = null
	fun initVar() {
		IVs_status = arrayOf(
				b!!.IVStatus1Waiting,
				b!!.IVStatus2Start,
				b!!.IVStatus3Listning
		)
		for (v in IVs_status) v.setAlpha(0)
		IVs_status[0].setAlpha(1)
	}

	// =========================================================================================
	var tts: TextToSpeech? = null
	val WAITING = 0
	val START = 1
	val LISTENING = 2

	// firebase
	var firestore = FirebaseFirestore.getInstance()
	var firebase = FirebaseDatabase.getInstance().reference

	//
	var mapData: MutableMap<String, Command> = HashMap()
	var waitingKeys = ArrayList<String?>()

	// ========================================================================================= Start
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		b = DataBindingUtil.setContentView(this, R.layout.activity_main)
		initVar()

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
		b!!.button.setOnClickListener { view: View? ->
			if (AL_keywordSelected.size == 0) startVoice() else {
				var str = ""
				for (s in AL_keywordSelected) str += "$s "
				MessageViewer(this@MainActivity, b!!.LLList, MessageViewer.Type.IN).changeText(str)
				sendMsg(str)
				initKeyword()
			}
		}
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
												addOutputMessage(command.response)
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
					startVoice()
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
		var found = false
		var command: Command? = null
		for (i in 0 until mapData.size) {
			command = mapData[mapData.keys.toTypedArray()[i]]
			var num = 0
			for (keyword in command!!.keyword!!) if (msg.contains(keyword!!)) num++
			if (num == command.keyword!!.size) {
				found = true
				break
			}
		}
		if (found) {
			if (!command!!.responseStandby) {
				addOutputMessage(command.response)
				firebase.child("queue").push().setValue(command.command)
			} else {
				addOutputMessage("잠시만 기다려주세요...")
				firebase.child("queue").push().setValue(command.command)
				waitingKeys.add(command.key)
			}
		} else addOutputMessage("알아듣지 못했어요 ㅠㅠ")
	}

	// ========================================================================================= Status
	var prevIndex = 0
	var nowIndex = 0
	fun changeStatus(changeIndex: Int) {
		prevIndex = nowIndex
		nowIndex = changeIndex
		IVs_status[prevIndex].animate().alpha(0f).setDuration(500).start()
		IVs_status[nowIndex].animate().alpha(1f).setDuration(500).start()
		when (changeIndex) {
			START -> findViewById<View>(R.id.V_back).animate().alpha(1f).setDuration(500).start()
			WAITING -> findViewById<View>(R.id.V_back).animate().alpha(0f).setDuration(500).start()
		}
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
		b!!.keywordList.removeAllViews()
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
		b!!.keywordList.addView(keywordView)
	}

	fun updateCheckedKeyword() {
		AL_keywordSelected.clear()
		for (keywordView in AL_keywordView) {
			if (keywordView.isChecked) AL_keywordSelected.add(keywordView.text)
		}
		updateKeyword()
	}

	// ========================================================================================= Add message
	class MessageViewer(var context: Context, parent: LinearLayout, type: Type) {
		enum class Type {
			IN, OUT
		}

		var view: LinearLayout
		var textView: TextView
		var message: String? = null
		fun changeText(message: String?) {
			this.message = message
			textView.text = message
		}

		init {
			view = View.inflate(
					context,
					if (type == Type.IN) R.layout.message_in else R.layout.message_out,
					null
			) as LinearLayout
			textView = view.findViewById(R.id.textview)
			parent.addView(view)
		}
	}

	fun addInputMessage(msg: String?) {
		val linearLayout =
				View.inflate(this@MainActivity, R.layout.message_in, null) as LinearLayout
		(linearLayout.findViewById<View>(R.id.textview) as TextView).text = msg
		b!!.LLList.addView(linearLayout)
		//SV_scrollView.post(new Runnable() { @Override public void run() { SV_scrollView.smoothScrollBy(0, 10000); } });
	}

	fun addOutputMessage(msg: String?) {
		val linearLayout =
				View.inflate(this@MainActivity, R.layout.message_out, null) as LinearLayout
		(linearLayout.findViewById<View>(R.id.textview) as TextView).text = msg
		tts(msg)
		b!!.LLList.addView(linearLayout)
		b!!.SVScrollView.post { b!!.SVScrollView.smoothScrollBy(0, 10000) }
	}

	// =========================================================================================
	fun tts(msg: String?) {
		val utteranceId = this.hashCode().toString() + ""
		tts!!.speak(msg, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
	}

	fun startVoice() {
		if (!sttRuning)
			stt.startListening(sttIntent)
		else
			stt.stopListening()
//		if (naverRecognizer?.speechRecognizer?.isRunning!!) {
//			naverRecognizer!!.recognize()
//		} else {
//			naverRecognizer?.speechRecognizer?.stop()
//		}
	}

	fun log(msg: String?) {
		Log.e("com.lunacoding.log", msg!!)
	}

	override fun onKeyDown(keycode: Int, event: KeyEvent): Boolean {
		when (keycode) {
			KeyEvent.KEYCODE_VOLUME_DOWN -> startVoice()
			KeyEvent.KEYCODE_VOLUME_UP -> startVoice()
			else -> super.onKeyDown(keycode, event)
		}
		return true
	}

	var messageViewer: MessageViewer? = null
//	private fun handleMessage(msg: Message) {
//		when (msg.what) {
//			R.id.clientReady -> {
//				log("clientReady")
//				writer =
//						AudioWriterPCM(Environment.getExternalStorageDirectory().absolutePath + "/NaverSpeechTest")
//				writer!!.open("Test")
//				changeStatus(START)
//				messageViewer = MessageViewer(this@MainActivity, b!!.LLList, MessageViewer.Type.IN)
//			}
//			R.id.audioRecording ->                //log("audioRecording");
//				writer!!.write(msg.obj as ShortArray)
//			R.id.partialResult -> {
//				log("partialResult")
//				mResult = msg.obj as String
//				log(mResult)
//				messageViewer!!.changeText(mResult)
//				changeStatus(LISTENING)
//			}
//			R.id.finalResult -> {
//				log("finalResult")
//				val speechRecognitionResult = msg.obj as SpeechRecognitionResult
//				val results = speechRecognitionResult.results
//				val result = results[0]
//				log("onResults: $result")
//				messageViewer!!.changeText(mResult)
//				sendMsg(result)
//			}
//			R.id.recognitionError -> {
//				log("recognitionError")
//				if (writer != null) {
//					writer!!.close()
//				}
//				changeStatus(WAITING)
//			}
//			R.id.clientInactive -> {
//				log("clientInactive")
//				if (writer != null) {
//					writer!!.close()
//				}
//				changeStatus(WAITING)
//			}
//		}
//	}

	var sttRuning = false
	val recognitionListener = object : RecognitionListener {
		override fun onReadyForSpeech(params: Bundle?) {
		}

		override fun onBeginningOfSpeech() {
			sttRuning = true
		}

		override fun onRmsChanged(rmsdB: Float) {
		}

		override fun onBufferReceived(buffer: ByteArray?) {
		}

		override fun onEndOfSpeech() {
			sttRuning = false
		}

		override fun onError(error: Int) {
		}

		override fun onResults(results: Bundle?) {
			var result = ""
			val matches: ArrayList<String> = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
			for (str in matches) {
				Log.d("TEST/RESULTS", str)
				result += str
			}

			log("onResults: $result")
			messageViewer!!.changeText(mResult)
			sendMsg(result)
		}

		override fun onPartialResults(partialResults: Bundle?) {
		}

		override fun onEvent(eventType: Int, params: Bundle?) {
		}

	}

	companion object {
		private const val CLIENT_ID = "wz1il84vga"
	}
}