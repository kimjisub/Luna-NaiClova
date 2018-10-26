package com.lunacoding.stellar;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kimjisub on 2018. 3. 31..
 */

@IgnoreExtraProperties
public class fbData {
	public int callNum = 0;
	public int code = 0;
	public long connectionTime = 0;
	public String ip = "";
	public boolean isCalled = false;
	public String name = "";
	public boolean status = false;
	
	public fbData() {
	}
	
	public fbData(int callNum, int code, long connectionTime, String ip, boolean isCalled, String name, boolean status) {
		this.callNum = callNum;
		this.code = code;
		this.connectionTime = connectionTime;
		this.ip = ip;
		this.isCalled = isCalled;
		this.name = name;
		this.status = status;
	}
	
	@Exclude
	public Map<String, Object> toMap() {
		HashMap<String, Object> result = new HashMap<>();
		result.put("callNum", callNum);
		result.put("code", code);
		result.put("connectionTime", connectionTime);
		result.put("ip", ip);
		result.put("isCalled", isCalled);
		result.put("name", name);
		result.put("status", status);
		
		return result;
	}
}