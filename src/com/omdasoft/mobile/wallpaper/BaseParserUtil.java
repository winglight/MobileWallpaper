package com.omdasoft.mobile.wallpaper;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public abstract class BaseParserUtil {
	protected String language;
	protected String path;
	protected String encode;
	
	public String getEncode() {
		return encode;
	}
	public void setEncode(String encode) {
		this.encode = encode;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	public abstract String getReleaseUrl();
	
	protected String getUrlContent(String url) {
		HttpClient httpclient = null;
		String result = "";
		try {
			httpclient = new DefaultHttpClient();

			HttpGet httpget = new HttpGet(url);

			HttpResponse response = httpclient.execute(httpget);

			int status = response.getStatusLine().getStatusCode();

			if (status == HttpStatus.SC_OK) {

				HttpEntity resEntity = response.getEntity();

				// interpret link in this page
				if (resEntity.getContentType() != null
						&& resEntity.getContentType().getValue()
								.startsWith("text")) {

					result = EntityUtils.toString(resEntity, "gb2312");

				} else {
					result = "URL type error: not text content.";
				}
			} else {
				result = "Network exception, please try it later.";
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (httpclient != null && httpclient.getConnectionManager() != null) {
				httpclient.getConnectionManager().shutdown();
			}
		}
		return result;
	}
	
	protected String addHeadEnd(String content, String title, String page, String encode) {
		content = "<html><head><title>"
				+ title
				+ "</title><meta http-equiv='Content-Type' content='text/html; charset=" + encode + "'>"
				+ "</head><body>" + getAd2Script()
				+ "<table id='cTable' style='display:block' border='0'>"
				+ content + "</table><table border='0'>" + page
				+ "</table>"+ getAd2Script() + "<br/><br/></body></html>";
		return content;
	}
	
	protected String addHeadEndNoAd(String content, String title, String page, String encode) {
		content = "<html><head><title>"
				+ title
				+ "</title><meta http-equiv='Content-Type' content='text/html; charset=" + encode + "'>"
				+ "</head><body>" 
				+ "<table id='cTable' style='display:block' border='0'>"
				+ content + "</table><table border='0'>" + page
				+ "</table>" + "<br/><br/></body></html>";
		return content;
	}
	
	protected String getAdScript() {
		return "<script type=\"text/javascript\">window.googleAfmcRequest = {    client: 'ca-mb-pub-2087650701843653',    "
				+ "format: '320x50_mb',    output: 'html',    slotname: '1566110887',  }; "
				+ "</script><script type=\"text/javascript\"    src=\"http://pagead2.googlesyndication.com/pagead/show_afmc_ads.js\"></script>";
	}

	protected String getAd2Script() {
		return "<script type=\"text/javascript\">google_ad_client = \"ca-pub-2087650701843653\";"
				+ "/* headbanner */google_ad_slot = \"8580855209\";google_ad_width = 468;google_ad_height = 60;"
				+ "</script><script type=\"text/javascript\"src=\"http://pagead2.googlesyndication.com/pagead/show_ads.js\"></script>";
	}

	public abstract String parseLine(String result) ;
	
}
