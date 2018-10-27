package com.lunacoding.stellar.firestore;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Command {
	public String command;
	public ArrayList<String> keyword;
	public String response;
	public boolean responseStandby;
	public Timestamp responseTimestamp;
	
	public Command() {
		this.command = "";
		this.keyword = new ArrayList<>();
		this.response = "";
		this.responseStandby = false;
		this.responseTimestamp = new Timestamp(new Date());
	}
	
	public Command(String command, ArrayList<String> keyword, String response, boolean responseStandby, Timestamp responseTimestamp) {
		this.command = command;
		this.keyword = keyword;
		this.response = response;
		this.responseStandby = responseStandby;
		this.responseTimestamp = responseTimestamp;
	}
	
	public Command(QueryDocumentSnapshot document) {
		this.command = document.getString("command");
		this.keyword = (ArrayList<String>) document.get("keyword");
		this.response = document.getString("response");
		this.responseStandby = document.getBoolean("responseStandby");
		this.responseTimestamp = document.getTimestamp("responseTimestamp");
	}
	
	public Command(Map map) {
		command = (String) map.get("command");
		keyword = (ArrayList<String>) map.get("keyword");
		response = (String) map.get("response");
		responseStandby = (boolean) map.get("responseStandby");
		responseTimestamp = (Timestamp) map.get("responseTimestamp");
	}
	
	public Map<String, Object> toMap() {
		Map map = new HashMap();
		map.put("command", command);
		map.put("keyword", keyword);
		map.put("response", response);
		map.put("responseStandby", responseStandby);
		map.put("responseTimestamp", responseTimestamp);
		
		return map;
	}
	
	public String toString() {
		return toMap().toString();
	}
}
