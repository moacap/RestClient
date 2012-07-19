package com.roobit.android.restclient;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.net.Uri;

import com.roobit.android.restclient.RestClient.Operation;

public class RestClientRequest_Froyo implements RestClientRequest {

	private static HttpClient _client = null;
	private static final Lock lock = new ReentrantLock();
	private HttpRequestBase _requestBase;

	@Override
	public RestResult synchronousExecute(Operation op, Uri uri) {
		return synchronousExecute(op, uri, null, null, null);
	}

	@Override
	public RestResult synchronousExecute(Operation op, Uri uri, Properties httpHeaders) {
		return synchronousExecute(op, uri, httpHeaders, null, null);
	}

	@Override
	public RestResult synchronousExecute(Operation op, Uri uri, Properties httpHeaders, Properties parameters) {
		return synchronousExecute(op, uri, httpHeaders, parameters, null);
	}

	@Override
	public RestResult synchronousExecute(Operation op, Uri uri, Properties httpHeaders, Properties parameters, ByteArrayOutputStream postData) {
		lock.lock();
		if(_client == null) {
			HttpParams params = new BasicHttpParams();
			ConnManagerParams.setMaxTotalConnections(params, 10);
			ConnManagerParams.setTimeout(params, 2000);
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpConnectionParams.setConnectionTimeout(params, 10000);
			HttpConnectionParams.setSoTimeout(params, 10000);

			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

			ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
			_client = new DefaultHttpClient(cm, params);
		}
		lock.unlock();

		_requestBase = null;
		RestResult result = new RestResult();
		switch(op) {
			case DELETE:
				_requestBase = new HttpDelete(uri.toString());
				break;
			case GET:
				_requestBase = new HttpGet(uri.toString());
				break;
			case POST:
				HttpPost post = new HttpPost(uri.toString());
				StringEntity entity;
				try {
					entity = new StringEntity(postData.toString(HTTP.UTF_8), HTTP.UTF_8);
				} catch (UnsupportedEncodingException e1) {
					result.setException(e1);
					e1.printStackTrace();
					return result;
				}
				post.setEntity(entity);
				_requestBase = post;
				break;
		}
		if(httpHeaders != null && httpHeaders.size() > 0) {
			for(Object propertyKey : httpHeaders.keySet()) {
				_requestBase.addHeader((String)propertyKey, (String)httpHeaders.get(propertyKey));
			}
		}

		InputStream is = null;
		try {
			HttpResponse response = _client.execute(_requestBase);

			if(response != null) {
				result.setResponseCode(response.getStatusLine().getStatusCode());
				Map<String, List<String>> headers = new HashMap<String, List<String>>();
				for(Header header: response.getAllHeaders()) {
					List<String> values = new ArrayList<String>();
					values.add(header.getValue());
					headers.put(header.getName(), values);
				}
				result.setHeaders(headers);
				HttpEntity entity = response.getEntity();
				if(entity != null) {
					is = entity.getContent();
					result.setResponse(convertStreamToString(new BufferedInputStream(is)));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			result.setException(e);
		} finally {
			_requestBase = null;
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	@Override
	public void cancel() {
		if(_requestBase != null) {
			_requestBase.abort();
		}
	}

	private static String convertStreamToString(InputStream is) throws IOException {
		final char[] buffer = new char[0x10000];
		StringBuilder sb = new StringBuilder();
		Reader in = new InputStreamReader(is, "UTF-8");

		int read;
		do {
			read = in.read(buffer, 0, buffer.length);
			if(read > 0) {
				sb.append(buffer, 0, read);
			}
		} while(read >= 0);

		return sb.toString();
	}

}
