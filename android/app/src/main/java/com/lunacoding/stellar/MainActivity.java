package com.lunacoding.stellar;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.Inflater;


public class MainActivity extends AppCompatActivity {
	
	Intent recognizerIntent;
	SpeechRecognizer mRecognizer;
	
	TextToSpeech tts;
	
	LinearLayout LL_list;
	View V_button;
	ImageView[] IVs_status;
	
	
	final int WAITING = 0;
	final int START = 1;
	
	
	ArrayList arrayList;
	
	void initVar() {
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
		arrayList = new ArrayList();
		
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
			
			log("onResults: " + rs[0]);
			//TV_search.setText(rs[0]);
			addInputMessage(rs[0]);
			
			boolean found = false;
			int i = 0;
			fbData data = null;
			for (; i < arrayList.size(); i++) {
				data = (fbData) arrayList.get(i);
				if (rs[0].contains(data.name)) {
					DatabaseReference database = FirebaseDatabase.getInstance().getReference();
					database.child("data").child(data.code + "").child("isCalled").setValue(true);
					found = true;
					break;
				}
			}
			
			if (found) {
				if (data.status) {
					addOutputMessage(data.name + "을 찾았습니다.");
					
				} else {
					addOutputMessage(data.name + " 장치가 꺼져있습니다.");
				}
			} else {
				addOutputMessage("찾을 수 없습니다.");
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
	
	public static class GetDataList {
		FirebaseDatabase database;
		DatabaseReference myRef;
		
		private onDataListener dataListener = null;
		
		public interface onDataListener {
			void onAdd(fbData data);
			
			void onChange(fbData data);
		}
		
		
		public GetDataList setDataListener(onDataListener listener) {
			this.dataListener = listener;
			return this;
		}
		
		void onAdd(fbData data) {
			if (dataListener != null)
				dataListener.onAdd(data);
		}
		
		void onChange(fbData data) {
			if (dataListener != null)
				dataListener.onChange(data);
		}
		
		
		public void run() {
			database = FirebaseDatabase.getInstance();
			myRef = database.getReference("data");
			
			myRef.addChildEventListener(new ChildEventListener() {
				@Override
				public void onChildAdded(DataSnapshot dataSnapshot, String s) {
					try {
						fbData data = dataSnapshot.getValue(fbData.class);
						onAdd(data);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				@Override
				public void onChildChanged(DataSnapshot dataSnapshot, String s) {
					try {
						fbData data = dataSnapshot.getValue(fbData.class);
						onChange(data);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				@Override
				public void onChildRemoved(DataSnapshot dataSnapshot) {
				
				}
				
				@Override
				public void onChildMoved(DataSnapshot dataSnapshot, String s) {
				
				}
				
				@Override
				public void onCancelled(DatabaseError databaseError) {
				
				}
			});
		}
		
	}
	
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
		((TextView)linearLayout.findViewById(R.id.textview)).setText(msg);
		
		LL_list.addView(linearLayout);
	}
	
	void addOutputMessage(String msg) {
		LinearLayout linearLayout = (LinearLayout) View.inflate(MainActivity.this, R.layout.message_out, null);
		((TextView)linearLayout.findViewById(R.id.textview)).setText(msg);
		
		LL_list.addView(linearLayout);
	}
	
	
	void tts(String msg) {
		String utteranceId = this.hashCode() + "";
		tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
	}
	
	void startVoice() {
		mRecognizer.startListening(recognizerIntent);
	}
	
	void log(String msg) {
		Log.d("com.kimjisub.log", msg);
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
