package org.digitalcampus.mtrain.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.digitalcampus.mtrain.R;
import org.digitalcampus.mtrain.application.DbHelper;
import org.digitalcampus.mtrain.application.MTrain;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class SubmitTrackerTask extends AsyncTask<Payload, Object, Payload> {

	public final static String TAG = "SubmitTrackerTask";
	public final static int SUBMIT_LOG_TASK = 1001;
	
	private Context ctx;
	private SharedPreferences prefs;
	
	public SubmitTrackerTask(Context c){
		this.ctx = c;
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	}
	
	@Override
	protected Payload doInBackground(Payload... params) {
		Payload payload = params[0];
		
		DbHelper db = new DbHelper(ctx);
		
		for (org.digitalcampus.mtrain.model.Log l: (org.digitalcampus.mtrain.model.Log[]) payload.data) {
			
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, Integer.parseInt(prefs.getString("prefServerTimeoutConnection", ctx.getString(R.string.prefServerTimeoutConnection))));
			HttpConnectionParams.setSoTimeout(httpParameters, Integer.parseInt(prefs.getString("prefServerTimeoutResponse", ctx.getString(R.string.prefServerTimeoutResponseDefault))));
			DefaultHttpClient client = new DefaultHttpClient(httpParameters);
			
			String url = prefs.getString("prefServer" , ctx.getString(R.string.prefServerDefault)) + MTrain.TRACKER_PATH;
			HttpPost httpPost = new HttpPost(url);
			try {
				// update progress dialog
				Log.d(TAG,"Sending log...." + l.id);
				
				// add post params
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("username", prefs.getString("prefUsername", "")));
				nameValuePairs.add(new BasicNameValuePair("password", prefs.getString("prefPassword", "")));
				nameValuePairs.add(new BasicNameValuePair("content", l.content));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// make request
				HttpResponse execute = client.execute(httpPost);
				
				// read response
				InputStream content = execute.getEntity().getContent();
				BufferedReader buffer = new BufferedReader(new InputStreamReader(content),4096);
				String response = "";
				String s = "";
				
				while ((s = buffer.readLine()) != null) {
					response += s;
				}
				Log.d(TAG,response);
				JSONObject json = new JSONObject(response);
				if(json.has("result")){
					if(json.getBoolean("result")){
						Log.d(TAG,l.digest+ " marked as submitted");
						db.markLogSubmitted(l.id);
					}
				}
				
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				publishProgress("Error connecting to server");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				publishProgress("Error connecting to server");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				publishProgress("Invalid response from server");
			} finally {
				
			}
		}
		db.close();
		return null;
	}
	

	protected void onProgressUpdate(String... obj) {
		Log.d(TAG,obj[0]);
		
	}
	
}