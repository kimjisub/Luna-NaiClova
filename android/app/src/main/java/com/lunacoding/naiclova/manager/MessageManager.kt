package com.lunacoding.naiclova.manager

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.lunacoding.naiclova.R

class MessageManager(private val context: Context, private val parent: LinearLayout) {

    private val list: ArrayList<MessageItem> = ArrayList()

    fun newMessage(msg: String, type: MessageItem.Type): Int {
        val messageItem = MessageItem(context, msg, type)
        list.add(messageItem)
        parent.addView(messageItem.view)
        return list.size -1
    }

    fun editMessage(id: Int, msg: String){
        list[id].edit(msg)
    }
}

class MessageItem(context: Context, var msg: String, type: Type) {
    var view: View

    enum class Type {
        IN, OUT
    }

    init {
        view = View.inflate(
            context,
            if (type == Type.IN) R.layout.message_in else R.layout.message_out,
            null
        ) as LinearLayout

        edit(msg)
    }

    fun edit(msg: String) {
        this.msg = msg
        view.findViewById<TextView>(R.id.textview).text = msg
    }

}