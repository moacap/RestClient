package com.roobit.android.restclient;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import android.net.Uri;
import android.os.AsyncTask;

public class RestClientRequestTask extends AsyncTask<Object, Void, RestResult> {

	private static final int MAX_RETRIES = 3;
	public interface RestClientRequestListener {
		public void requestStarted();
		public void requestCancelled();
		public void requestFinishedPreprocess(RestResult result);
		public void requestFinished(RestResult result);
	}

	RestClientRequestListener listener;
	
	public RestClientRequestTask(RestClientRequestListener listener) {
		this.listener = listener;
	}

	@Override
	protected RestResult doInBackground(Object... args) {
		RestClient.Operation op = (RestClient.Operation) args[0];
		Uri uri = (Uri) args[1];
		Properties httpHeaders = (Properties) args[2];
		Properties parameters = (Properties) args[3];
		ByteArrayOutputStream postData = (ByteArrayOutputStream) args[4];
		int retries = 0;
		RestResult result = null;
		do {
			result = RestClientRequest.synchronousExecute(op, uri, httpHeaders, parameters, postData);
			if(result.getResponseCode() == -1 && !isCancelled()) {
				continue;
			} else {
				break;
			}
		} while(retries++ < MAX_RETRIES);
		if(listener != null) {
			listener.requestFinishedPreprocess(result);
		}
		return result;
	}

	
	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.requestStarted();
		}
	}

	@Override
	protected void onCancelled() {
		if (listener != null) {
			listener.requestCancelled();
		}
	}

	@Override
	protected void onPostExecute(RestResult result) {
		if (listener != null) {
			listener.requestFinished(result);
		}
	}	
}
