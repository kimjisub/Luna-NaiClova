package com.lunacoding.naiclova.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.util.*

class Command {
	var key = ""
	var command: String?
	var keyword: ArrayList<String>?
	var response: String?
	var responseStandby: Boolean
	var responseTimestamp: Timestamp?

	constructor() {
		command = ""
		keyword = ArrayList()
		response = ""
		responseStandby = false
		responseTimestamp = Timestamp(Date())
	}

	constructor(command: String?, keyword: ArrayList<String>?, response: String?, responseStandby: Boolean, responseTimestamp: Timestamp?) {
		this.command = command
		this.keyword = keyword
		this.response = response
		this.responseStandby = responseStandby
		this.responseTimestamp = responseTimestamp
	}

	constructor(document: QueryDocumentSnapshot) {
		command = document.getString("command")
		keyword = document["keyword"] as ArrayList<String>?
		response = document.getString("response")
		responseStandby = document.getBoolean("responseStandby")!!
		responseTimestamp = document.getTimestamp("responseTimestamp")
	}

	constructor(key: String, document: QueryDocumentSnapshot) {
		this.key = key
		command = document.getString("command")
		keyword = document["keyword"] as ArrayList<String>?
		response = document.getString("response")
		responseStandby = document.getBoolean("responseStandby") ?: false
		responseTimestamp = document.getTimestamp("responseTimestamp")
	}

	constructor(map: Map<*, *>) {
		command = map["command"] as String?
		keyword = map["keyword"] as ArrayList<String>?
		response = map["response"] as String?
		responseStandby = map["responseStandby"] as Boolean
		responseTimestamp = map["responseTimestamp"] as Timestamp?
	}

	private fun toMap(): Map<String, Any?> {
		return mapOf(
				"command" to command,
				"keyword" to keyword,
				"response" to response,
				"responseStandby" to responseStandby,
				"responseTimestamp" to responseTimestamp,
		)
	}

	override fun toString(): String {
		return toMap().toString()
	}
}