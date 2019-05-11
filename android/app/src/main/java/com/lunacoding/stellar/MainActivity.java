package com.lunacoding.stellar;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.service.voice.AlwaysOnHotwordDetector;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.lunacoding.stellar.databinding.ActivityMainBinding;
import com.lunacoding.stellar.firestore.Command;
import com.lunacoding.stellar.recognizer.AudioWriterPCM;
import com.lunacoding.stellar.recognizer.NaverRecognizer;
import com.lunacoding.stellar.view.KeywordView;
import com.naver.speech.clientapi.SpeechRecognitionResult;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
	private static final String CLIENT_ID = "wz1il84vga";

	ActivityMainBinding b;

	private RecognitionHandler handler;
	private NaverRecognizer naverRecognizer;
	private AudioWriterPCM writer;

	ImageView[] IVs_status;

	private String mResult;

	void initVar() {
		IVs_status = new ImageView[]{
				b.IVStatus1Waiting,
				b.IVStatus2Start,
				b.IVStatus3Listning
		};

		for (ImageView v : IVs_status)
			v.setAlpha(0);
		IVs_status[0].setAlpha(1);
	}

	// =========================================================================================

	TextToSpeech tts;

	final int WAITING = 0;
	final int START = 1;
	final int LISTNING = 2;

	// firebase
	FirebaseFirestore firestore = FirebaseFirestore.getInstance();
	DatabaseReference firebase = FirebaseDatabase.getInstance().getReference();

	//
	Map<String, Command> mapData = new HashMap<>();
	ArrayList<String> waitingKeys = new ArrayList<>();


	// ========================================================================================= Start

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		b = DataBindingUtil.setContentView(this, R.layout.activity_main);
		initVar();

		handler = new RecognitionHandler(this);
		naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);

		TedPermission.with(this)
			.setPermissionListener(new PermissionListener() {
				@Override
				public void onPermissionGranted() {
					start();
				}

				@Override
				public void onPermissionDenied(List<String> deniedPermissions) {
					finish();
				}


			})
			.setRationaleMessage("음성인식을 위해서 오디오 녹음 권한이 필요합니다.\n해당 권한에 대한 허가를 받습니다.")
			.setDeniedMessage("오디오 녹음 장치에 액세스해야합니다.\n[설정] > [권한]에서 권한을 켜십시오.")
			.setPermissions(android.Manifest.permission.RECORD_AUDIO)
			.check();
	}

	void start() {
		//startService(new Intent(MainActivity.this, HotwordService.class));
		//bindService(new Intent(MainActivity.this, HotwordService.class), sconn, BIND_AUTO_CREATE);

		tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status != TextToSpeech.ERROR) {
					tts.setLanguage(Locale.KOREAN);
				}
			}
		});

		b.button.setOnClickListener(view -> {
			if (AL_keywordSelected.size() == 0)
				startVoice();
			else {
				String str = "";

				for (String s : AL_keywordSelected)
					str += s + " ";

				sendMsg(str);
				initKeyword();
			}

		});


		firestore.collection("command")
			.addSnapshotListener(new EventListener<QuerySnapshot>() {
				@Override
				public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
					if (e != null) {
						log("listen:error" + e);
						return;
					}

					for (DocumentChange dc : snapshots.getDocumentChanges()) {
						QueryDocumentSnapshot document = dc.getDocument();

						String key = document.getId();

						try {
							Command command;
							switch (dc.getType()) {
								case ADDED:
									command = new Command(key, document);
									log("New: " + key);
									log(command.toString());

									mapData.put(key, command);
									updateKeyword();
									break;
								case MODIFIED:
									command = new Command(key, document);
									log("Modified: " + key);
									log(command.toString());

									Command oldCommand = mapData.get(key);
									if (command.responseTimestamp.getSeconds() != oldCommand.responseTimestamp.getSeconds()) {
										for (String waitingKey : waitingKeys) {
											if (waitingKey.equals(key)) {
												addOutputMessage(command.response);
												waitingKeys.remove(key);
											}
										}
									}

									mapData.put(key, command);
									updateKeyword();
									break;
								case REMOVED:
									command = new Command(key, document);
									log("Removed: " + key);
									log(command.toString());

									mapData.remove(key);
									updateKeyword();
									break;
							}
						} catch (Exception e1) {
							log("error: " + key);
							e1.printStackTrace();
						}
					}

				}
			});

	}

	HotwordService mService;
	ServiceConnection sconn = new ServiceConnection() {
		@Override //서비스가 실행될 때 호출
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((HotwordService.MyBinder) service).getService();
			mService.setOnHotwordListener(new HotwordService.OnHotwordListener() {
				@Override
				public void onRecognitionResumed() {

				}

				@Override
				public void onRecognitionPaused() {

				}

				@Override
				public void onError() {

				}

				@Override
				public void onDetected(AlwaysOnHotwordDetector.EventPayload eventPayload) {
					startVoice();
				}

				@Override
				public void onAvailabilityChanged(int status) {

				}
			});
		}

		@Override //서비스가 종료될 때 호출
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};

	// ========================================================================================= Recognition

	private RecognitionListener recognitionListener = new RecognitionListener() {
		@Override
		public void onReadyForSpeech(Bundle bundle) {
			log("onReadyForSpeech");
			changeStatus(START);
		}

		@Override
		public void onBeginningOfSpeech() {
			log("onBeginningOfSpeech");
			changeStatus(LISTNING);
		}

		@Override
		public void onRmsChanged(float v) {
			log("onRmsChanged " + v);
		}

		@Override
		public void onBufferReceived(byte[] bytes) {
			log("onBufferReceived " + bytes);
		}

		@Override
		public void onEndOfSpeech() {
			log("onEndOfSpeech");
			changeStatus(WAITING);
		}

		@Override
		public void onError(int i) {
			//log("onError " + i);
		}

		@Override
		public void onResults(Bundle bundle) {
			ArrayList<String> mResult = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

			String[] rs = new String[mResult.size()];
			mResult.toArray(rs);
			String msg = rs[0];

			log("onResults: " + msg);
			sendMsg(msg);
		}

		@Override
		public void onPartialResults(Bundle bundle) {
			log("onPartialResults");
		}

		@Override
		public void onEvent(int i, Bundle bundle) {
			log("onEvent");
		}
	};

	void sendMsg(String msg) {
		addInputMessage(msg);

		boolean found = false;
		Command command = null;

		for (int i = 0; i < mapData.size(); i++) {
			final String key = (String) mapData.keySet().toArray()[i];
			command = mapData.get(key);


			int num = 0;
			for (String keyword : command.keyword)
				if (msg.contains(keyword))
					num++;

			if (num == command.keyword.size()) {
				found = true;
				break;
			}
		}

		if (found) {
			if (!command.responseStandby) {
				addOutputMessage(command.response);
				firebase.child("queue").push().setValue(command.command);
			} else {
				addOutputMessage("잠시만 기다려주세요...");
				firebase.child("queue").push().setValue(command.command);
				waitingKeys.add(command.key);
			}
		} else
			addOutputMessage("알아듣지 못했어요 ㅠㅠ");
	}

	// ========================================================================================= Status

	int prevIndex = 0;
	int nowIndex = 0;

	void changeStatus(int changeIndex) {
		prevIndex = nowIndex;
		nowIndex = changeIndex;

		IVs_status[prevIndex].animate().alpha(0).setDuration(500).start();
		IVs_status[nowIndex].animate().alpha(1).setDuration(500).start();

		switch (changeIndex) {
			case START:
				findViewById(R.id.V_back).animate().alpha(1).setDuration(500).start();
				break;
			case WAITING:
				findViewById(R.id.V_back).animate().alpha(0).setDuration(500).start();
				break;
		}
	}

	// ========================================================================================= Keyword

	ArrayList<KeywordView> AL_keywordView = new ArrayList<>();
	ArrayList<String> AL_keywordSelected = new ArrayList<>();

	void initKeyword() {
		AL_keywordSelected.clear();
		updateKeyword();
	}

	void updateKeyword() {
		AL_keywordView.clear();
		b.keywordList.removeAllViews();

		ArrayList<String> keywords = new ArrayList<>();
		ArrayList<String> keywords1 = new ArrayList<>();
		ArrayList<String> keywords2 = new ArrayList<>();

		for (int i = 0; i < mapData.size(); i++) {
			final String key = (String) mapData.keySet().toArray()[i];
			Command command = mapData.get(key);


			boolean doit = false;


			if (AL_keywordSelected.size() == 0)
				doit = true;
			else {

				int num = 0;
				for (String keyword1 : command.keyword) {
					for (String keyword2 : AL_keywordSelected)
						if (keyword1.equals(keyword2))
							num++;


				}
				if (num == AL_keywordSelected.size())
					doit = true;

			}

			if (doit) {
				int num = 0;
				for (String keyword : command.keyword) {
					boolean found = false;

					for (String s : keywords) {
						if (keyword.equals(s)) {
							found = true;
							break;
						}
					}
					if (!found) {
						boolean found2 = false;
						for (String s : AL_keywordSelected)
							if (keyword.equals(s)) {
								found2 = true;
								break;
							}
						if (found2) {
							keywords.add(keyword);
							keywords1.add(keyword);

						} else {
							keywords.add(keyword);
							keywords2.add(keyword);
						}
					}
				}
			}
		}

		for (String keyword : keywords1)
			addKeyword(keyword, true);
		for (String keyword : keywords2)
			addKeyword(keyword, false);
	}

	void addKeyword(String keyword, boolean check) {
		KeywordView keywordView = new KeywordView(MainActivity.this);
		keywordView.setText(keyword);
		keywordView.setChecked(check);
		keywordView.setOnEventListener(new KeywordView.OnEventListener() {
			@Override
			public void onViewClick(KeywordView v) {
				v.setChecked(!v.isChecked());
				updateCheckedKeyword();
			}

			@Override
			public void onViewLongClick(KeywordView v) {

			}
		});

		AL_keywordView.add(keywordView);
		b.keywordList.addView(keywordView);
	}

	void updateCheckedKeyword() {
		AL_keywordSelected.clear();

		for (KeywordView keywordView : AL_keywordView) {
			if (keywordView.isChecked())
				AL_keywordSelected.add(keywordView.getText());
		}
		updateKeyword();
	}


	// ========================================================================================= Add message

	void addInputMessage(String msg) {
		LinearLayout linearLayout = (LinearLayout) View.inflate(MainActivity.this, R.layout.message_in, null);
		((TextView) linearLayout.findViewById(R.id.textview)).setText(msg);

		b.LLList.addView(linearLayout);
		//SV_scrollView.post(new Runnable() { @Override public void run() { SV_scrollView.smoothScrollBy(0, 10000); } });
	}

	void addOutputMessage(String msg) {
		LinearLayout linearLayout = (LinearLayout) View.inflate(MainActivity.this, R.layout.message_out, null);
		((TextView) linearLayout.findViewById(R.id.textview)).setText(msg);
		tts(msg);

		b.keywordList.addView(linearLayout);
		b.SVScrollView.post(new Runnable() {
			@Override
			public void run() {
				b.SVScrollView.smoothScrollBy(0, 10000);
			}
		});
	}

	// =========================================================================================

	void tts(String msg) {
		String utteranceId = this.hashCode() + "";
		tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
	}

	void startVoice() {
		if (!naverRecognizer.getSpeechRecognizer().isRunning()) {
			// Start button is pushed when SpeechRecognizer's state is inactive.
			// Run SpeechRecongizer by calling recognize().
			/*mResult = "";
			txtResult.setText("Connecting...");
			btnStart.setText(R.string.str_stop);*/
			naverRecognizer.recognize();
		} else {
			/*Log.d(TAG, "stop and wait Final Result");
			btnStart.setEnabled(false);*/

			naverRecognizer.getSpeechRecognizer().stop();
		}
	}

	void log(String msg) {
		Log.e("com.lunacoding.log", msg);
	}

	public boolean onKeyDown(int keycode, KeyEvent event) {
		switch (keycode) {
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				startVoice();
				break;

			case KeyEvent.KEYCODE_VOLUME_UP:
				startVoice();
				break;
			default:
				super.onKeyDown(keycode, event);
				break;
		}
		return true;
	}

	private void handleMessage(Message msg) {
		switch (msg.what) {
			case R.id.clientReady:
				log("clientReady");

				writer = new AudioWriterPCM(Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
				writer.open("Test");

				changeStatus(START);
				break;

			case R.id.audioRecording:
				//log("audioRecording");

				writer.write((short[]) msg.obj);
				break;

			case R.id.partialResult:
				log("partialResult");

				mResult = (String) (msg.obj);
				log(mResult);


				changeStatus(LISTNING);
				break;

			case R.id.finalResult:
				log("finalResult");

				SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
				List<String> results = speechRecognitionResult.getResults();

				String result = results.get(0);

				log("onResults: " + result);
				sendMsg(result);
				break;

			case R.id.recognitionError:
				log("recognitionError");

				if (writer != null) {
					writer.close();
				}

				changeStatus(WAITING);
				/*mResult = "Error code : " + msg.obj.toString();
				txtResult.setText(mResult);
				btnStart.setText(R.string.str_start);
				btnStart.setEnabled(true);*/
				break;

			case R.id.clientInactive:
				log("clientInactive");
				if (writer != null) {
					writer.close();
				}

				changeStatus(WAITING);
				break;
		}
	}


	static class RecognitionHandler extends Handler {
		private final WeakReference<MainActivity> mActivity;

		RecognitionHandler(MainActivity activity) {
			mActivity = new WeakReference<>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity activity = mActivity.get();
			if (activity != null) {
				activity.handleMessage(msg);
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		naverRecognizer.getSpeechRecognizer().initialize();
	}

	@Override
	protected void onStop() {
		super.onStop();
		naverRecognizer.getSpeechRecognizer().release();
	}
}
