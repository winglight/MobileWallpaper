package com.omdasoft.mobile.wallpaper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.yihabits.mobile.wallpaper.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

public class AddLinkToFavorites implements OnMenuItemClickListener {

	private String url;
	private String domain;
	private Activity activity;

	public AddLinkToFavorites(Activity activity, String url, String domain) {
		this.url = url;
		this.domain = domain;
		this.activity = activity;
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {

		Runnable saveUrl = new Runnable() {
			public void run() {
				addLink(AddLinkToFavorites.this.url);
			}
		};
		new Thread(saveUrl).start();
		return true;
	}


	private void addLink(String url) {
		String title="index";
		try {
			URLConnection connection = new URL(url).openConnection();

			String content = connection.getHeaderField("Content-Type");
			String encode = connection.getContentEncoding();
			Source source;
			
			if(content != null && content.startsWith("text")) {

				BufferedReader reader = new BufferedReader(new InputStreamReader(
						connection.getInputStream(), encode));

				
				String line;
				while ((line = reader.readLine()) != null) {
					source = new Source(line);
					List<Element> tlist = source.getAllElements(HTMLElementName.TITLE);
					if(tlist != null && tlist.size() > 0){
						title = source.getAllElements(HTMLElementName.TITLE).get(0).getContent().getTextExtractor().toString();
						title = new String(title.getBytes(), "UTF-8");
						break;
					}
				}
				reader.close();
			} 
		} catch (Exception e) {
			e.printStackTrace();
		} 
					
					//deal with url is within the same site - starts with domaain and contains /go/go2/
					if(url.contains(domain) && url.contains("/go/go2/")){
						url = url.substring(url.indexOf("/go/go2/"));
					}
					
					url = "http://" + domain + ":8080/go/go2/?d=fa&i=" + getUid() + "&t=" + title + "&p=" + url;
					
					boolean flag = updateFavorite(url);
					
					if(flag){
						//popup successful message
						((MobileWallpaperActivity) this.activity).toastMsg(this.activity.getString(R.string.addLinkFavoriteSuccess));
					}else{
						//popup failed message
						((MobileWallpaperActivity) this.activity).toastMsg(this.activity.getString(R.string.addLinkFavoriteFail));
					}

		
	}
	
	private boolean updateFavorite(String url){
		boolean res = false;
		HttpEntity resEntity = null;
		
		if(url.startsWith("/go/go2/")){
			url = "http://" + domain + ":8080" + url;
		}
		
		try {
			ApplicationEx app = (ApplicationEx) this.activity.getApplication();

			HttpClient httpclient = app.getHttpClient();

			HttpGet httpget = new HttpGet(url);

			HttpResponse response = httpclient.execute(httpget);

			int status = response.getStatusLine().getStatusCode();
			
			if (status == HttpStatus.SC_OK) {

				resEntity = response.getEntity();

				// interpret link in this page
				if(resEntity.getContentType() != null && resEntity.getContentType().getValue().startsWith("text")){
				
					String result = EntityUtils.toString(resEntity);
					// 1.parse the page and replace charset
					Source source = new Source(result);
					
					//get title;
					String title = "index";
					List<Element> tlist = source.getAllElements(HTMLElementName.TITLE);
					if(tlist != null && tlist.size() > 0){
						title = source.getAllElements(HTMLElementName.TITLE).get(0).getContent().getTextExtractor().toString();
						if(!"Error".equals(title)){
							return true;
						}
					}
					
				}else {

					//TODO: popup error toaster
				}

			} else {
				// TODO:throw an exception and alert the owner
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (resEntity != null) {
				try {
					resEntity.consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return res;
	}
	
	public String getUid(){
		return Settings.Secure.getString(this.activity.getContentResolver(), Settings.Secure.ANDROID_ID);
	}

}