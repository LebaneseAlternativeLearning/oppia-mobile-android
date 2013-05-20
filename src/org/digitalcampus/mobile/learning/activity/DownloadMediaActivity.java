/* 
 * This file is part of OppiaMobile - http://oppia-mobile.org/
 * 
 * OppiaMobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OppiaMobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OppiaMobile. If not, see <http://www.gnu.org/licenses/>.
 */

package org.digitalcampus.mobile.learning.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.mobile.learning.listener.DownloadMediaListener;
import org.digitalcampus.mobile.learning.model.DownloadProgress;
import org.digitalcampus.mobile.learning.model.Media;
import org.digitalcampus.mobile.learning.task.DownloadMediaTask;
import org.digitalcampus.mobile.learning.task.Payload;
import org.digitalcampus.mobile.learning.utils.UIUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;

import com.bugsense.trace.BugSenseHandler;

public class DownloadMediaActivity extends AppActivity implements DownloadMediaListener{

	public static final String TAG = DownloadMediaActivity.class.getSimpleName();
	private ArrayList<Object> missingMedia = new ArrayList<Object>();
	private ProgressDialog pDialog;
	private DownloadMediaTask task;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download_media);
		this.drawHeader();
		
		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null) {
			missingMedia = (ArrayList<Object>) bundle.getSerializable(DownloadMediaActivity.TAG);
		}
		
		WebView wv = (WebView) findViewById(R.id.download_media_webview);
		String url = "file:///android_asset/www/en/download_media.html";
		wv.loadUrl(url);
		
		WebView wvml = (WebView) findViewById(R.id.download_media_list_webview);
		String strData = "<ul>";
		for(Object o: missingMedia){
			Media m = (Media) o;
			strData += "<li>"+m.getFilename()+"</li>";
		}
		strData += "</ul>";
		wvml.loadData(strData,"text/html","utf-8");
		Button downloadBtn = (Button) this.findViewById(R.id.download_media_btn);
		downloadBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				download();
			}
		});
		Button downloadViaPCBtn = (Button) this.findViewById(R.id.download_media_via_pc_btn);
		downloadViaPCBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				downloadViaPC();
			}
		});
	}

	private void download(){
		//check the user is on a wifi network connection
		ConnectivityManager conMan = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE); 
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo == null || netInfo.getType() != ConnectivityManager.TYPE_WIFI){
        	UIUtils.showAlert(this, R.string.warning, R.string.warning_wifi_required);
			return;
		}
		
		// show progress dialog
		pDialog = new ProgressDialog(this);
		pDialog.setTitle(R.string.downloading);
		pDialog.setMessage(getString(R.string.download_starting));
		pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pDialog.setProgress(0);
		pDialog.setMax(100);
		pDialog.setCancelable(false);
		pDialog.show();

		task = new DownloadMediaTask(this);
		Payload p = new Payload(0,missingMedia);
		task.setDownloadListener(this);
		task.execute(p);
	}
	
	private void downloadViaPC(){
		String filename = "mobile-learning-media.html";
		String strData = "<html>";
		strData += "<head><title>"+this.getString(R.string.download_via_pc_title)+"</title></head>";
		strData += "<body>";
		strData += "<h3>"+this.getString(R.string.download_via_pc_title)+"</h3>";
		strData += "<p>"+this.getString(R.string.download_via_pc_intro)+"</p>";
		strData += "<ul>";
		for(Object o: missingMedia){
			Media m = (Media) o;
			strData += "<li><a href='"+m.getDownloadUrl()+"'>"+m.getFilename()+"</a></li>";
		}
		strData += "</ul>";
		strData += "</body></html>";
		strData += "<p>"+this.getString(R.string.download_via_pc_final,"/digitalcampus/media/")+"</p>";
		
		File file = new File(Environment.getExternalStorageDirectory(),filename);
		try {
			FileOutputStream f = new FileOutputStream(file);
			Writer out = new OutputStreamWriter(new FileOutputStream(file));
			out.write(strData);
			out.close();
			f.close();
			UIUtils.showAlert(this, R.string.info, this.getString(R.string.download_via_pc_message,filename));
		} catch (FileNotFoundException e) {
			BugSenseHandler.sendException(e);
			e.printStackTrace();
		} catch (IOException e) {
			BugSenseHandler.sendException(e);
			e.printStackTrace();
		}
	}
	
	public void downloadStarting() {
		Log.d(TAG,"download starting");
	}

	public void downloadProgressUpdate(DownloadProgress msg) {
		pDialog.setMessage(msg.getMessage());
		pDialog.setProgress(msg.getProgress());
	}

	public void downloadComplete() {
		pDialog.cancel();
		this.finish();
	}
	
	
}
