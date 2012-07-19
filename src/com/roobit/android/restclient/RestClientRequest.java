package com.roobit.android.restclient;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import android.net.Uri;

import com.roobit.android.restclient.RestClient.Operation;

public interface RestClientRequest {

	public RestResult synchronousExecute(Operation op, Uri uri);

	public RestResult synchronousExecute(Operation op, Uri uri, Properties httpHeaders);

	public RestResult synchronousExecute(Operation op, Uri uri, Properties httpHeaders, Properties parameters);

	public RestResult synchronousExecute(Operation op, Uri uri, Properties httpHeaders, Properties parameters, ByteArrayOutputStream postData);

	public void cancel();
}
