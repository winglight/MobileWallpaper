package com.omdasoft.mobile.wallpaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.yihabits.mobile.wallpaper.R;
import org.apache.commons.io.IOUtils;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MobileWallpaperActivity extends Activity {
	private String categoryUrl = "file:///android_asset/mobile_wallpaper_category_zh.html";
	private String language;
	private MyWebView mainWeb;
	private String LOGTAG = "MobileWallpaperActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// full screen
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);
		
		// ad initialization
		// Create the adView
		AdView adView = new AdView(this, AdSize.BANNER, "a14de4c9326897d");
		// Lookup your LinearLayout assuming it��s been given
		// the attribute android:id="@+id/mainLayout"
		LinearLayout layout = (LinearLayout) findViewById(R.id.ad_layout);
		// Add the adView to it
		layout.addView(adView);
		// Initiate a generic request to load it with an ad
		adView.loadAd(new AdRequest());
		
		//get locale language
		String locale = this.getResources().getConfiguration().locale.getLanguage();
		if(!locale.startsWith("zh")){
			language = "zh";
			categoryUrl = "file:///android_asset/mobile_wallpaper_category.html";
		}else{
			language = "en";
		}

		mainWeb = (MyWebView) findViewById(R.id.mainWebView);
		registerForContextMenu(mainWeb);
		mainWeb.getSettings().setJavaScriptEnabled(false);
		mainWeb.setWebViewClient(new WebViewClient() {
			
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if(url.startsWith("http://")){
					view.loadUrl(url);
					return true;
				}else{
					return false;
				}
			}
			
			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				// TODO Auto-generated method stub
				super.onReceivedError(view, errorCode, description, "");
			}
		}); 

				mainWeb.loadUrl(categoryUrl);
	}

	@Override
	public boolean onKeyDown(int keyCoder, KeyEvent event) {
		int action = event.getAction();
	    int keyCode = event.getKeyCode();
	        switch (keyCode) {
	        case KeyEvent.KEYCODE_VOLUME_UP:
	            if (action == KeyEvent.ACTION_DOWN) {
	               mainWeb.pageUp(false);
	               return true;
	            }
	            
	        case KeyEvent.KEYCODE_VOLUME_DOWN:
	            if (action == KeyEvent.ACTION_DOWN) {
	            	mainWeb.pageDown(false);
	            	return true;
	            }
	        case KeyEvent.KEYCODE_BACK:
	        	if(mainWeb.canGoBack()){
	        		mainWeb.goBack();
	        		return true;
	        	}else{
	        		this.finish();
	        	}
	        default:
	            return false;
	        }
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  super.onConfigurationChanged(newConfig);
	  // We do nothing here. We're only handling this to keep orientation
	  // or keyboard hiding from causing the WebView activity to restart.
	}

	@Override
	protected void onStart() {
		super.onStart();

		//initialize download folders
		(new DownloadUtil(this, "", false)).initBaseDir();
		
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
				// judge if jump to release notes
				String sureKey = getString(R.string.pref_release_show);
				boolean releaseflag = prefs.getBoolean(sureKey, true);
				String url = "";
				if (releaseflag) {
					url = getString(R.string.pref_release_note);
					// don't show release note for this version ever
					savePrefRelease(false);

				} else {
					// go to last visit url
					sureKey = getString(R.string.pref_last_page);
					url = prefs.getString(sureKey, categoryUrl);
				}
				mainWeb.loadUrl(url);


	}

	@Override
	protected void onStop() {

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = prefs.edit();
		String sureKey = getString(R.string.pref_last_page);
		editor.putString(sureKey, mainWeb.getUrl());
		editor.commit();

		super.onStop();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		// super.onCreateContextMenu(menu, v, menuInfo);
		WebView webview = (WebView) v;
		WebView.HitTestResult result = webview.getHitTestResult();
		if (result == null) {
			return;
		}

		int type = result.getType();
		if (type == WebView.HitTestResult.UNKNOWN_TYPE) {
			Log.w(LOGTAG,
					"We should not show context menu when nothing is touched");
			return;
		}
		if (type == WebView.HitTestResult.EDIT_TEXT_TYPE) {
			// let TextView handles context menu
			return;
		}

		// Note, http://b/issue?id=1106666 is requesting that
		// an inflated menu can be used again. This is not available
		// yet, so inflate each time (yuk!)
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);

		// Show the correct menu group
		final String extra = result.getExtra();
		// menu.setGroupVisible(R.id.PHONE_MENU,
		// type == WebView.HitTestResult.PHONE_TYPE);
		 menu.setGroupVisible(R.id.EMAIL_MENU,
		 type == WebView.HitTestResult.EMAIL_TYPE);
		// menu.setGroupVisible(R.id.GEO_MENU,
		// type == WebView.HitTestResult.GEO_TYPE);
		menu.setGroupVisible(R.id.IMAGE_MENU,
				type == WebView.HitTestResult.IMAGE_TYPE
						|| type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
		menu.setGroupVisible(R.id.ANCHOR_MENU,
				type == WebView.HitTestResult.SRC_ANCHOR_TYPE);
		

		// Setup custom handling depending on the type
		switch (type) {
		// case WebView.HitTestResult.PHONE_TYPE:
		// menu.setHeaderTitle(Uri.decode(extra));
		// menu.findItem(R.id.dial_context_menu_id).setIntent(
		// new Intent(Intent.ACTION_VIEW, Uri
		// .parse(WebView.SCHEME_TEL + extra)));
		// Intent addIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		// addIntent.putExtra(Insert.PHONE, Uri.decode(extra));
		// addIntent.setType(Contacts.People.CONTENT_ITEM_TYPE);
		// menu.findItem(R.id.add_contact_context_menu_id).setIntent(
		// addIntent);
		// menu.findItem(R.id.copy_phone_context_menu_id).setOnMenuItemClickListener(
		// new Copy(extra));
		// break;

		case WebView.HitTestResult.EMAIL_TYPE:
			menu.findItem(R.id.menu_send_email).setIntent(
					new Intent(Intent.ACTION_VIEW, Uri
							.parse(WebView.SCHEME_MAILTO + extra)));
			// menu.findItem(R.id.menu_copy_email_addr).setOnMenuItemClickListener(
			// new Copy(extra));
//			menu.add(0, android.R.id.copy, 1, android.R.string.copy);
			break;

		// case WebView.HitTestResult.GEO_TYPE:
		// menu.setHeaderTitle(extra);
		// menu.findItem(R.id.map_context_menu_id).setIntent(
		// new Intent(Intent.ACTION_VIEW, Uri
		// .parse(WebView.SCHEME_GEO
		// + URLEncoder.encode(extra))));
		// menu.findItem(R.id.copy_geo_context_menu_id).setOnMenuItemClickListener(
		// new Copy(extra));
		// break;
		case WebView.HitTestResult.SRC_ANCHOR_TYPE:
			// download the page
			menu.findItem(R.id.menu_save_this_link).setOnMenuItemClickListener(
					new DownloadUtil(this, extra, false));
			// download the page with content
			menu.findItem(R.id.menu_save_next_level_link)
					.setOnMenuItemClickListener(
							new DownloadUtil(this, extra, true));
			break;
		// otherwise fall through to handle image part
		case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
		case WebView.HitTestResult.IMAGE_TYPE:
			menu.findItem(R.id.menu_download_this_image)
					.setOnMenuItemClickListener(
							new DownloadUtil(this, extra, false));
			menu.findItem(R.id.menu_download_all_image)
					.setOnMenuItemClickListener(
							new DownloadUtil(this, mainWeb.getUrl(), true));
			menu.findItem(R.id.menu_send_image_mms)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {

				@Override
				public boolean onMenuItemClick(MenuItem item) {
					if(extra != null && extra.startsWith("file://")){
						String tmp = extra.substring(7);
						Intent sendIntent = new Intent(Intent.ACTION_SEND); 
						sendIntent.putExtra("sms_body", MobileWallpaperActivity.this.getString(R.string.sendMmsBody)); 
						sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(tmp));
						sendIntent.setType("image/jpeg");
						MobileWallpaperActivity.this.startActivity(sendIntent);
					}
					return true;
				}
				
			});
			menu.findItem(R.id.menu_set_as_wallpaper)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MobileWallpaperActivity.this);
					Editor editor = prefs.edit();
					String sureKey = getString(R.string.pref_wallpaper);
					String fileName = extra.substring(7);
					File file = new File(fileName);
					if(file.exists()){
						String newFileName = fileName.replace("temp", "wallpaper");
						try {
							IOUtils.copy(new FileInputStream(file), new FileOutputStream(new File(newFileName)));
							editor.putString(sureKey, newFileName);
						} catch (IOException e) {
							editor.putString(sureKey, fileName);
						}
						
						editor.commit();
					}
					
//					Bitmap bitmap = BitmapFactory.decodeFile(extra.substring(7));
//					Intent myIntent = new Intent();
//					myIntent.putExtra("bitmap", extra.substring(7));
//					myIntent.setClass(MobileWallpaperActivity.this, MyLiveWallpaper.class);
//					startService(myIntent);
//					try {
//						MobileWallpaperActivity.this.setWallpaper(bitmap);
//						 WallpaperManager.getInstance(MobileWallpaperActivity.this).setBitmap(bitmap);
//						toastMsg(MobileWallpaperActivity.this.getString(R.string.setWallpaperSuccess));
//					} catch (IOException e) {
						// TODO Auto-generated catch block
//						e.printStackTrace();
//						toastMsg(MobileWallpaperActivity.this.getString(R.string.setWallpaperFail));
//					}
					toastMsg(MobileWallpaperActivity.this.getString(R.string.setWallpaperSuccess));
					return true;
				}
			});
			break;

		default:
			Log.w(LOGTAG, "We should not get here.");
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_home:
			mainWeb.loadUrl(categoryUrl);
			return true;
		case R.id.menu_favorite:
			mainWeb.loadUrl("file://" + DownloadUtil.getIndexPath());
			return true;
		case R.id.menu_help:
			// popup the about window
			mainWeb.loadUrl("file:///android_asset/help.html");
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	private void savePrefRelease(boolean flag) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = prefs.edit();
		String sureKey = getString(R.string.pref_release_show);
		editor.putBoolean(sureKey, flag);
		editor.commit();
	}
	
	public void toastMsg(final String msg){
		runOnUiThread(new Runnable() {

	        @Override
	        public void run() {
	            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
	        }
	    });
	}

	public String getLanguage() {
		return language;
	}


}