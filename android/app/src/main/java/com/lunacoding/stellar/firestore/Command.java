package com.lunacoding.stellar.firestore;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Command {
	public String command;
	public ArrayList<String> keyword;
	public String response;
	
	public Command() {
		this.command = "";
		this.keyword = new ArrayList<>();
		this.response = "";
	}
	
	public Command(String command, ArrayList<String> keyword, String response) {
		this.command = command;
		this.keyword = keyword;
		this.response = response;
	}
	
	public Command(QueryDocumentSnapshot document) {
		this.command = document.getString("command");
		this.keyword = (ArrayList<String>) document.get("keyword");
		this.response = document.getString("response");
	}
	
	public Command(Map map) {
		command = (String) map.get("command");
		keyword = (ArrayList<String>) map.get("keyword");
		response = (String) map.get("response");
	}
	
	public Map<String, Object> toMap() {
		Map map = new HashMap();
		map.put("command", command);
		map.put("keyword", keyword);
		map.put("response", response);
		
		return map;
	}
	
	public String toString(){
		return toMap().toString();
	}
}
