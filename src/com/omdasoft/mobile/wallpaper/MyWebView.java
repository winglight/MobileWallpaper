package com.omdasoft.mobile.wallpaper;

import java.util.HashMap;
import java.util.Map;
import net.yihabits.mobile.wallpaper.R;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class MyWebView extends WebView {

	public MyWebView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public MyWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public MyWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void loadUrl(String url) {
		// TODO Auto-generated method stub
		if(url.startsWith("http://")){
			//TODO:save the whole page to sdcard
				String tmp = new DownloadUtil((Activity) getContext(), url, false).saveHtml(url);
				if(tmp != null){
					 url = tmp;
					
					}
		}
		super.loadUrl(url);
	}

}
