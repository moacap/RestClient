package com.roobit.android.restclient;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Properties;

import android.net.Uri;

import com.roobit.android.restclient.RestClientRequestTask.RestClientRequestListener;

public class RestClient implements RestClientRequestListener {

	public interface OnCompletionListener {
		public void processSuccessfulResult(RestClient client, RestResult result);
		public void success(RestClient client, RestResult result);
		public void failedWithError(RestClient restClient, int responseCode, RestResult result);
		public void requestCancelled(RestClient restClient);
	}

	public enum Operation { GET, POST, PUT, DELETE, PATCH }

	String baseUrl;
	String resource;
	LinkedHashMap<String, String> queryParameters = null;
	Properties httpHeaders = null;
	Properties parameters = null;
	ByteArrayOutputStream postData;
	Operation operation;
	OnCompletionListener completionListener;
	private RestClientRequestTask requestTask;

	public static RestClient clientWithBaseUrl(String baseUrl) {
		return new RestClient(baseUrl);
	}

	protected RestClient(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getUrl() {
		return buildUri().toString();
	}

	private Uri buildUri() {
		Uri.Builder builder = Uri.parse(getBaseUrl())
			.buildUpon()
			.appendEncodedPath(getResource());

		if (queryParameters != null && !queryParameters.isEmpty()) {
			Iterator<Entry<String, String>> iter = queryParameters.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, String> entry = iter.next();
				builder.appendQueryParameter(entry.getKey(), entry.getValue());
			}
		}
		return builder.build();
	}

	private String getResource() {
		return resource;
	}

	public RestClient setResource(String resource) {
		this.resource = resource;
		return this;
	}

	public RestClient setQueryParameters(LinkedHashMap<String,String> queryParameters) {
		this.queryParameters = queryParameters;
		return this;
	}

	public RestClient execute(OnCompletionListener completionListener) {
		this.completionListener = completionListener;
		requestTask = new RestClientRequestTask(this);
		requestTask.execute(getOperation(), buildUri(), httpHeaders, parameters, postData);
		return this;
	}

	public void setOnCompletionListener(OnCompletionListener listener) {
		this.completionListener = listener;
	}

	public void cancelRequest() {
		if(requestTask != null && !requestTask.isCancelled()) {
			requestTask.cancelRequest();
		}
	}

	private Operation getOperation() {
		if (operation == null) {
			operation = Operation.GET;
		}
		return operation;
	}

	public RestClient get() {
		operation = Operation.GET;
		return this;
	}

	public RestClient get(Properties headers) {
		get();
		if(httpHeaders != null) {
			httpHeaders.putAll(headers);
		} else {
			setHttpHeaders(httpHeaders);
		}
		return this;
	}

	public RestClient get(Properties headers, Properties queryParams) {
		get(headers);
		setParameters(queryParams);
		return this;
	}

	public RestClient post() {
		operation = Operation.POST;
		return this;
	}

	public RestClient post(Properties httpHeaders) {
		post();
		if(httpHeaders != null) {
			httpHeaders.putAll(httpHeaders);
		} else {
			setHttpHeaders(httpHeaders);
		}
		return this;
	}

	public RestClient post(ByteArrayOutputStream postData, String contentType, Properties httpHeaders) {
		post(httpHeaders == null ? new Properties() : httpHeaders);
		setPostData(postData);
		setContentType(contentType);
		return this;
	}

	public RestClient postForm(Properties parameters) {
		post();
		setParameters(parameters);
		return this;
	}

	public RestClient patch() {
		operation = Operation.PATCH;
		if(httpHeaders == null) {
			setHttpHeaders(new Properties());
		}
		return this;
	}

	public Properties getHttpHeaders() {
		if(httpHeaders == null) {
			httpHeaders = new Properties();
		}
		return httpHeaders;
	}

	private void setHttpHeaders(Properties httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	private void setParameters(Properties parameters) {
		this.parameters = parameters;
	}

	private void setContentType(String contentType) {
		if(contentType != null && !httpHeaders.contains("content-type")) {
			httpHeaders.put("content-type", contentType);
		}
	}

	private void setPostData(ByteArrayOutputStream postData) {
		this.postData = postData;
	}

	@Override
	public void requestStarted() {
		// TODO Auto-generated method stub
	}

	@Override
	public void requestCancelled() {
		if(completionListener != null) {
			completionListener.requestCancelled(this);
		}
	}

	@Override
	public void requestFinished(RestResult result) {
		if (completionListener != null) {
			if(result.isSuccess()) {
				completionListener.success(this, result);
			} else {
				completionListener.failedWithError(this, result.getResponseCode(), result);
			}
		}
	}

	@Override
	public void requestFinishedPreprocess(RestResult result) {
		if(completionListener != null) {
			if(result.isSuccess()) {
				completionListener.processSuccessfulResult(this, result);
			}
		}
	}

}
