package com.lunacoding.stellar;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.lunacoding.stellar.firestore.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
	
	Intent recognizerIntent;
	SpeechRecognizer mRecognizer;
	
	TextToSpeech tts;
	
	ScrollView SV_scrollView;
	LinearLayout LL_list;
	View V_button;
	ImageView[] IVs_status;
	
	
	final int WAITING = 0;
	final int START = 1;
	
	
	FirebaseFirestore firestore = FirebaseFirestore.getInstance();
	Map<String, Command> mapData;
	
	void initVar() {
		SV_scrollView = findViewById(R.id.SV_scrollView);
		LL_list = findViewById(R.id.LL_list);
		V_button = findViewById(R.id.V_button);
		IVs_status = new ImageView[]{
			findViewById(R.id.IV_status_1_waiting),
			findViewById(R.id.IV_status_2_start),
			findViewById(R.id.IV_status_3_listning)
		};
		
		for (ImageView v : IVs_status)
			v.setAlpha(0);
		IVs_status[0].setAlpha(1);
		
		mapData = new HashMap<>();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initVar();
		
		
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
		tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status != TextToSpeech.ERROR) {
					tts.setLanguage(Locale.KOREAN);
				}
			}
		});
		
		recognizerIntent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
		recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
		recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
		
		mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
		mRecognizer.setRecognitionListener(recognitionListener);
		
		
		V_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startVoice();
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
							
							switch (dc.getType()) {
								case ADDED:
									mapData.put(key, new Command(document));
									log("New: " + key);
									log(new Command(document).toString());
									break;
								case MODIFIED:
									mapData.put(key, new Command(document));
									log("Modified: " + key);
									log(new Command(document).toString());
									break;
								case REMOVED:
									mapData.remove(key);
									log("Removed: " + key);
									log(new Command(document).toString());
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
	
	private RecognitionListener recognitionListener = new RecognitionListener() {
		@Override
		public void onReadyForSpeech(Bundle bundle) {
			log("onReadyForSpeech");
			changeStatus(START);
			
			
		}
		
		@Override
		public void onBeginningOfSpeech() {
			log("onBeginningOfSpeech");
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
				addOutputMessage(command.response);
			} else {
				addOutputMessage("알아듣지 못했어요 ㅠㅠ");
			}
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
	
	void addInputMessage(String msg) {
		LinearLayout linearLayout = (LinearLayout) View.inflate(MainActivity.this, R.layout.message_in, null);
		((TextView) linearLayout.findViewById(R.id.textview)).setText(msg);
		
		LL_list.addView(linearLayout);
		//SV_scrollView.post(new Runnable() { @Override public void run() { SV_scrollView.smoothScrollBy(0, 10000); } });
	}
	
	void addOutputMessage(String msg) {
		LinearLayout linearLayout = (LinearLayout) View.inflate(MainActivity.this, R.layout.message_out, null);
		((TextView) linearLayout.findViewById(R.id.textview)).setText(msg);
		tts(msg);
		
		LL_list.addView(linearLayout);
		SV_scrollView.post(new Runnable() { @Override public void run() { SV_scrollView.smoothScrollBy(0, 10000); } });
	}
	
	
	void tts(String msg) {
		String utteranceId = this.hashCode() + "";
		tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
	}
	
	void startVoice() {
		mRecognizer.startListening(recognizerIntent);
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
	
}
