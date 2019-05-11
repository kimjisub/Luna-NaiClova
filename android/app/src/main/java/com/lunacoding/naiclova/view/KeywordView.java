package com.lunacoding.naiclova.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lunacoding.naiclova.R;

public class KeywordView extends RelativeLayout {
	Context context;
	
	RelativeLayout RL_root;
	LinearLayout LL_background;
	TextView TV_textview;
	
	boolean isChecked = false;
	
	private void initView(Context context) {
		this.context = context;
		
		String infService = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(infService);
		View v = li.inflate(R.layout.keyword, this, false);
		addView(v);
		
		// set view
		RL_root = findViewById(R.id.root);
		LL_background = findViewById(R.id.background);
		TV_textview = findViewById(R.id.textview);
		
		// event
		RL_root.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				onViewClick();
			}
		});
		RL_root.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				onViewLongClick();
				return false;
			}
		});
	}
	
	public KeywordView(Context context) {
		super(context);
		initView(context);
	}
	
	public KeywordView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}
	
	public KeywordView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView(context);
	}
	
	public KeywordView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initView(context);
	}
	
	public void setChecked(boolean b) {
		isChecked = b;
		
		LL_background.setBackground(getResources().getDrawable(b ? R.drawable.word_on : R.drawable.word_off));
		TV_textview.setTextColor(b ? 0xffaaaaaa : 0xffffffff);
	}
	
	public boolean isChecked() {
		return isChecked;
	}
	
	public void setText(String msg) {
		TV_textview.setText(msg);
	}
	
	public String getText(){
		return TV_textview.getText().toString();
	}
	
	// =========================================================================================
	
	private OnEventListener onEventListener = null;
	
	public interface OnEventListener {
		
		void onViewClick(KeywordView v);
		
		void onViewLongClick(KeywordView v);
	}
	
	public KeywordView setOnEventListener(OnEventListener listener) {
		this.onEventListener = listener;
		return this;
	}
	
	public void onViewClick() {
		if (onEventListener != null) onEventListener.onViewClick(this);
	}
	
	public void onViewLongClick() {
		if (onEventListener != null) onEventListener.onViewLongClick(this);
	}
}
