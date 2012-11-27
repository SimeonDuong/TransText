package edu.gvsu.masl.asynchttp;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import android.graphics.*;
import android.os.*;
import android.util.Log;

/**
 * Asynchronous HTTP connections
 * 
 * @author Greg Zavitz & Joseph Roth
 */
public class HttpConnection implements Runnable {

	public static final int DID_START = 0;
	public static final int DID_ERROR = 1;
	public static final int DID_SUCCEED = 2;
	public static final int DID_SUCCEED_4xx = 3;

	private static final int GET = 0;
	private static final int POST = 1;
	private static final int PUT = 2;
	private static final int DELETE = 3;
	private static final int BITMAP = 4;

	
	private static final long CACHE_DEFAULT_AGE = 60000;
	private static final int CACHE_SIZE = 100;

	
	public static final String URL_EMPTY_JSON_ARRAY_REQUEST = "url_empty_json_array_request";
	
	
	private static Map<String, Long> m_cacheAge = new HashMap<String, Long>();

	@SuppressWarnings("serial")
	//Use removeEldsetEntry to keep content cache from growing too large
	private static Map<String, String> m_cacheContent = new LinkedHashMap<String, String>(CACHE_SIZE/2, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<String, String> eldest) {
            return (size() > CACHE_SIZE);
        }
    };	

	
	
	private String m_url;
	private int m_method;
	private Handler m_handler;
	private String m_data;
	private long m_contentExpiration;

	private HttpClient httpClient;

	public HttpConnection() {
		this(new Handler());
	}

	public HttpConnection(Handler handler) {
		this(handler, CACHE_DEFAULT_AGE);
	}
	
	public HttpConnection(Handler handler, long contentExpiration) {
		m_handler = handler;
		m_contentExpiration = contentExpiration;
	}

	public void create(int method, String url, String data) {
		this.m_method = method;
		this.m_url = url;
		this.m_data = data;
		ConnectionManager.getInstance().push(this);
	}

	public void get(String url) {
		create(GET, url, null);
	}

	public void post(String url, String data) {
		create(POST, url, data);
	}

	public void put(String url, String data) {
		create(PUT, url, data);
	}

	public void delete(String url) {
		create(DELETE, url, null);
	}

	public void bitmap(String url) {
		create(BITMAP, url, null);
	}

	public void run() {
		
		
		//Signal that we've begun data retrieval
		sendMessage(HttpConnection.DID_START);
		Log.i("RUWT", "Starting connection...");
		
		
		
		//Is this a special request?
		boolean isStockResponse = false;
		if (m_url.startsWith(URL_EMPTY_JSON_ARRAY_REQUEST)) {
			sendMessage(DID_SUCCEED, "{\"results\":[]}");
			isStockResponse = true;
		}
		
		
		
		//Check cache for requested content, if this is a GET request.
		boolean foundRequestInCache = false;
		if (m_method == GET) {
			Long expiration;
			//Is content in cache?
			if (((expiration = m_cacheAge.get(m_url)) != null) && m_cacheContent.containsKey(m_url)) {
				//Has content expired?
				if (expiration > System.currentTimeMillis()) {
					//Content exists and is fresh.  Return cached copy.
					sendMessage(DID_SUCCEED, m_cacheContent.get(m_url));
					foundRequestInCache = true;
				}
			}
		}
		
		
		//If we couldn't find content in cache, retrieve it
		if (!isStockResponse && !foundRequestInCache) {
			httpClient = new DefaultHttpClient();
			HttpConnectionParams.setSoTimeout(httpClient.getParams(), 25000);
			HttpResponse response = null;
			try {
				
				switch (m_method) {
				case GET:
					HttpUriRequest request = new HttpGet(m_url);
					request.addHeader("Accept-Encoding", "gzip");
					response = httpClient.execute(request);
					break;
				case POST:
					HttpPost httpPost = new HttpPost(m_url);
					httpPost.setEntity(new StringEntity(m_data));
					response = httpClient.execute(httpPost);
					break;
				case PUT:
					HttpPut httpPut = new HttpPut(m_url);
					httpPut.setEntity(new StringEntity(m_data));
					response = httpClient.execute(httpPut);
					break;
				case DELETE:
					response = httpClient.execute(new HttpDelete(m_url));
					break;
				case BITMAP:
					response = httpClient.execute(new HttpGet(m_url));
					processBitmapEntity(response.getEntity());
					break;
				}
				if (m_method < BITMAP) {
					processEntity(response);
				}
			} catch (Exception e) {
				sendMessage(HttpConnection.DID_ERROR, e);
			}
		}
		
		
		Log.i("RUWT", (foundRequestInCache ? "HIT" : "MISS") + ": " + m_url);
		ConnectionManager.getInstance().didComplete(this);
		
		
	}

	private void processEntity(HttpResponse response) throws IllegalStateException, IOException {

		//Get content-encoding to check if the response is GZIP compressed
		Header contentEncoding = response.getFirstHeader("Content-Encoding");
		
		//Get content from request
		BufferedReader bufferedReader;
		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(response.getEntity().getContent())));
		} else {		
			bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		}
		
		StringBuilder responseText = new StringBuilder();
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			responseText.append(line);
		}

		//Put response in cache if request was a GET and response code was 200.
		//At some point, may want to cache other response codes as well.
		if ((m_method == GET) && (response.getStatusLine().getStatusCode() == 200)) {
			m_cacheAge.put(m_url, System.currentTimeMillis()+m_contentExpiration);
			m_cacheContent.put(m_url, responseText.toString());
		}
				
		//Send message back
		if (response.getStatusLine().getStatusCode() >= 500) {
			sendMessage(DID_ERROR, new RuntimeException("Server responded with " + response.getStatusLine().getStatusCode()));
		} else if (response.getStatusLine().getStatusCode() >= 400) {
			sendMessage(DID_SUCCEED_4xx, responseText.toString());
		} else {
			sendMessage(DID_SUCCEED, responseText.toString());
		}
	
	}

	private void processBitmapEntity(HttpEntity entity) throws IOException {
		BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
		Bitmap bm = BitmapFactory.decodeStream(bufHttpEntity.getContent());
		sendMessage(DID_SUCCEED, bm);
	}

	
	
	/**
	 * If called, this connection will continue to operate, but will
	 * not send any messages to the handler.
	 */
	public void mute() {
		m_handler = null;
	}
	
	
	/**
	 * Helper function to deliver messages to handler.
	 * 
	 * @param what
	 * @param obj
	 */
	private void sendMessage(int what, Object obj) {
		
		//If handler isn't set, we won't send messages
		if (m_handler == null) {
			return;
		}
		
		Message message;
		if (obj == null) {
			message = Message.obtain(m_handler, what);
		} else {
			message = Message.obtain(m_handler, what, obj);
		}
		
		m_handler.sendMessage(message);
		
	}
	
	private void sendMessage(int what) {
		sendMessage(what, null);
	}
	
	
}
