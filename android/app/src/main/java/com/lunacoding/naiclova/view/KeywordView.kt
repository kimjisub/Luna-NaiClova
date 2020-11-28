package com.lunacoding.naiclova.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnLongClickListener
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.lunacoding.naiclova.R

class KeywordView : RelativeLayout {
	var RL_root: RelativeLayout? = null
	var LL_background: LinearLayout? = null
	var TV_textview: TextView? = null
	var isChecked = false
		set(value) {
			field = value
			LL_background!!.background =
					resources.getDrawable(if (value) R.drawable.word_on else R.drawable.word_off)
			TV_textview!!.setTextColor(if (value) -0x555556 else -0x1)
		}

	private fun initView() {
		val infService = Context.LAYOUT_INFLATER_SERVICE
		val li = getContext().getSystemService(infService) as LayoutInflater
		val v = li.inflate(R.layout.view_keyword, this, false)
		addView(v)

		// set view
		RL_root = findViewById(R.id.root)
		LL_background = findViewById(R.id.background)
		TV_textview = findViewById(R.id.textview)

		// event


		RL_root?.setOnClickListener {
			onViewClick()
		}
		RL_root?.setOnLongClickListener {
			onViewLongClick()
			false
		}
	}

	constructor(context: Context) : super(context) {
		initView()
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		initView()
	}

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
			context,
			attrs,
			defStyleAttr
	) {
		initView()
	}

	constructor(
			context: Context,
			attrs: AttributeSet?,
			defStyleAttr: Int,
			defStyleRes: Int
	) : super(context, attrs, defStyleAttr, defStyleRes) {
		initView()
	}

	var text: String?
		get() = TV_textview!!.text.toString()
		set(msg) {
			TV_textview!!.text = msg
		}

	// =========================================================================================
	private var onEventListener: OnEventListener? = null

	interface OnEventListener {
		fun onViewClick(v: KeywordView)
		fun onViewLongClick(v: KeywordView?)
	}

	fun setOnEventListener(listener: OnEventListener?): KeywordView {
		onEventListener = listener
		return this
	}

	fun onViewClick() {
		if (onEventListener != null) onEventListener!!.onViewClick(this)
	}

	fun onViewLongClick() {
		if (onEventListener != null) onEventListener!!.onViewLongClick(this)
	}
}