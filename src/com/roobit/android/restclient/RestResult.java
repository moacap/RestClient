package com.roobit.android.restclient;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class RestResult {

	int responseCode;
	String response;
	Exception exception;
	private Map<String, List<String>> headers;

	public RestResult() {
		responseCode = 0;
		response = "";
	}
	
	public boolean isSuccess() {
		return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED || responseCode == HttpURLConnection.HTTP_CREATED;
	}
		
	public String getResponse() {
		return response;
	}
	
	public void setResponse(String response) {
		this.response = response;
	}
	
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}
	
	public Exception getException() {
		return exception;
	}
	
	public boolean hasException() {
		return getException() != null;
	}
	
	public void setHeaders(Map<String,List<String>> headers) {
		this.headers = headers;
	}
	
	public Map<String,List<String>> getHeaders() {
		return headers;
	}

}
