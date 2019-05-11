package com.lunacoding.naiclova;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.VoiceInteractionService;
import android.util.Log;

import java.util.Locale;

public class HotwordService extends VoiceInteractionService {
	
	private IBinder mIBinder = new MyBinder();
	
	class MyBinder extends Binder {
		HotwordService getService() {
			return HotwordService.this;
		}
	}
	
	// =========================================================================================
	
	private final String LOG_TAG = "HotwordService";
	
	Locale locale = Locale.KOREAN;
	
	AlwaysOnHotwordDetector hotwordDetector;
	
	@Override
	public void onCreate() {
		Log.i(LOG_TAG, "onCreate");
		
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(LOG_TAG, "onStartCommand");
		
		
		Bundle args = new Bundle();
		
		startSession(args);
		stopSelf(startId);
		
		return START_NOT_STICKY;
	}
	
	public void startSession(Bundle args, int flags) {
		showSession(args, flags);
	}
	
	public void startSession(Bundle args) {
		startSession(args, 0);
	}
	
	@Override
	public void onReady() {
		Log.i(LOG_TAG, "onReady");
		hotwordDetector = createAlwaysOnHotwordDetector("안녕", locale, callback);
		
		super.onReady();
	}
	
	AlwaysOnHotwordDetector.Callback callback = new AlwaysOnHotwordDetector.Callback() {
		
		@Override
		public void onRecognitionResumed() {
			if (onHotwordListener != null)
				onHotwordListener.onRecognitionResumed();
		}
		
		@Override
		public void onRecognitionPaused() {
			if (onHotwordListener != null)
				onHotwordListener.onRecognitionPaused();
		}
		
		@Override
		public void onError() {
			if (onHotwordListener != null)
				onHotwordListener.onError();
		}
		
		@Override
		public void onDetected(AlwaysOnHotwordDetector.EventPayload eventPayload) {
			if (onHotwordListener != null)
				onHotwordListener.onDetected(eventPayload);
		}
		
		@Override
		public void onAvailabilityChanged(int status) {
			if (onHotwordListener != null)
				onHotwordListener.onAvailabilityChanged(status);
		}
		
	};
	
	interface OnHotwordListener {
		void onRecognitionResumed();
		
		void onRecognitionPaused();
		
		void onError();
		
		void onDetected(AlwaysOnHotwordDetector.EventPayload eventPayload);
		
		void onAvailabilityChanged(int status);
	}
	
	OnHotwordListener onHotwordListener = new OnHotwordListener() {
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
		
		}
		
		@Override
		public void onAvailabilityChanged(int status) {
		
		}
	};
	
	public void setOnHotwordListener(OnHotwordListener onHotwordListener) {
		this.onHotwordListener = onHotwordListener;
	}
}