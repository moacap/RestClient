package com.roobit.android.restclient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Properties;

import android.net.Uri;
import android.util.Log;

import com.roobit.android.restclient.RestClient.Operation;

public class RestClientRequest_Gingerbread implements RestClientRequest {

	public enum StreamingMode { CHUNKED, FIXED };
	HttpURLConnection _connection;
	volatile private boolean _cancelled = false;


	static final String TAG = "RestClientRequest";
	static StreamingMode streamingMode = StreamingMode.CHUNKED;

	@Override
	public RestResult synchronousExecute(Operation op, Uri uri) {
		return synchronousExecute(op, uri, null);
	}

	@Override
	public RestResult synchronousExecute(Operation op, Uri uri, Properties httpHeaders) {
		return synchronousExecute(op, uri, httpHeaders, null);
	}

	@Override
	public RestResult synchronousExecute(Operation op, Uri uri, Properties httpHeaders, Properties parameters) {
		return synchronousExecute(op, uri, httpHeaders, parameters, null);
	}

	@Override
	public RestResult synchronousExecute(Operation op,
			Uri uri,
			Properties httpHeaders,
			Properties parameters,
			ByteArrayOutputStream postData) {

		Log.d(TAG, "Executing " + op.toString() + " to " + uri.toString());

		streamingMode = (parameters == null && postData == null)  ? StreamingMode.CHUNKED : StreamingMode.FIXED;

		RestResult result = new RestResult();
		_connection = null;
		try {
			_connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
			if(_cancelled) {
				return result;
			}
			setRequestMethod(_connection, op, httpHeaders);
			setRequestHeaders(_connection, httpHeaders);
			if(postData != null) {
				setPostData(_connection, postData);
			} else if(parameters != null){
				setRequestParameters(_connection, parameters);
			}
			if(_cancelled) {
				return result;
			}
			result.setResponseCode(_connection.getResponseCode());
			if(_cancelled) {
				return result;
			}
			Log.d(TAG, " - received response code [" + _connection.getResponseCode() + "]");
			if(_connection.getResponseCode() > 0 && _connection.getResponseCode() < 400) {
				result.setHeaders(_connection.getHeaderFields());
				result.setResponse(convertStreamToString(new BufferedInputStream(_connection.getInputStream())));
			} else {
				result.setResponse(convertStreamToString(new BufferedInputStream(_connection.getErrorStream())));
			}
		} catch (Exception e) {
			result.setException(e);
			e.printStackTrace();
		} finally {
			synchronized (this) {
				if (_connection != null) {
					_connection.disconnect();
					_connection = null;
				}
			}
		}

		return result;
	}

	@Override
	public void cancel() {
		synchronized (this) {
			_cancelled = true;
			if(_connection != null) {
				_connection.disconnect();
			}
		}
	}


	private static final int BUFFER_SIZE = 512;
	private static void setPostData(HttpURLConnection urlConnection, ByteArrayOutputStream postData) {
		byte[] buffer = new byte[BUFFER_SIZE];
		urlConnection.setFixedLengthStreamingMode(postData.size());

		OutputStream os = null;
		try {
			os = new BufferedOutputStream(urlConnection.getOutputStream());
			ByteArrayInputStream bais = new ByteArrayInputStream(postData.toByteArray());
			int count;
			while((count = bais.read(buffer, 0, BUFFER_SIZE)) > 0) {
				os.write(buffer, 0, count);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (os != null) {
				try {
					os.flush();
					os.close();
				} catch (Exception e) {}
			}
		}
	}

	private static void setRequestMethod(HttpURLConnection urlConnection, Operation op, Properties httpProperties) {
		if (op == Operation.POST || op == Operation.PATCH) {
			urlConnection.setDoOutput(true);

			if (streamingMode == StreamingMode.CHUNKED) {
				urlConnection.setChunkedStreamingMode(0);
			}

			if (op == Operation.PATCH) {
				httpProperties.put("X-HTTP-Method-Override", "PATCH");
			}
		}

		// TODO: Handle OPTIONS, HEAD, PUT, DELETE and TRACE
	}

	private static void setRequestHeaders(HttpURLConnection urlConnection, Properties httpHeaders) {
		if (httpHeaders == null) {
			return;
		}

		Iterator<Object> iter = httpHeaders.keySet().iterator();
		while(iter.hasNext()) {
			String name = (String) iter.next();
			urlConnection.addRequestProperty(name, httpHeaders.getProperty(name));
		}
	}

	private static void setRequestParameters(HttpURLConnection urlConnection, Properties parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return;
		}

		String params = buildParameterString(parameters);
		urlConnection.setFixedLengthStreamingMode(params.getBytes().length);

		OutputStream os = null;
		try {
			os = new BufferedOutputStream(urlConnection.getOutputStream());
			os.write(params.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (os != null) {
				try {
					os.flush();
					os.close();
				} catch (Exception e) {}
			}
		}
	}

	private static String buildParameterString(Properties parameters) {
		StringBuilder sb = new StringBuilder();
		boolean prependAmp = false;

		Iterator<Object> iter = parameters.keySet().iterator();
		while(iter.hasNext()) {
			if (prependAmp) {
				sb.append("&");
			}
			prependAmp = true;

			try {
				String name = (String) iter.next();
				sb.append(name).append('=').append(URLEncoder.encode(parameters.getProperty(name), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	private static String convertStreamToString(InputStream is) throws IOException {
		final char[] buffer = new char[0x10000];
		StringBuilder sb = new StringBuilder();
		Reader in = new InputStreamReader(is, "UTF-8");

		int read;
		do {
			read = in.read(buffer, 0, buffer.length);
			if (read>0) {
				sb.append(buffer, 0, read);
			}
		} while (read>=0);

		return sb.toString();
	}

}
