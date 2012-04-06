package com.omdasoft.mobile.wallpaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.yihabits.mobile.wallpaper.R;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

public class DownloadUtil implements OnMenuItemClickListener {

	private String url;
	private boolean isInclNextLevel;
	private Activity activity;
	private String basePath; // external storage path

	public DownloadUtil(Activity activity, String url, boolean isInclNextLevel) {
		this.url = url;
		this.isInclNextLevel = isInclNextLevel;
		this.activity = activity;
		this.basePath = initBaseDir();
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		int id = item.getItemId();
		final String dir = getDir(id);

		Runnable saveUrl = new Runnable() {
			public void run() {
				if (!DownloadUtil.this.isInclNextLevel) {
					// save one link
					saveUrl(DownloadUtil.this.url, dir, true);
				} else {
					// save link with next level links
					saveUrlWithNextLevel(DownloadUtil.this.url, dir);
				}
			}
		};
		new Thread(saveUrl).start();
		return true;
	}

	private void saveUrlWithNextLevel(String url, String dir) {
		HttpEntity resEntity = null;
		initDir(dir);
		try {
			ApplicationEx app = (ApplicationEx) this.activity.getApplication();

			HttpClient httpclient = app.getHttpClient();

			HttpGet httpget = new HttpGet(url);

			HttpResponse response = httpclient.execute(httpget);

			int status = response.getStatusLine().getStatusCode();

			if (status == HttpStatus.SC_OK) {

				resEntity = response.getEntity();

				String path = dir + parseFileNameByUrl(url);

				// interpret link in this page
				if (resEntity.getContentType() != null
						&& resEntity.getContentType().getValue()
								.startsWith("text")) {

					String result = EntityUtils.toString(resEntity);
					// 1.parse the page with all next level pages
					ArrayList<String> urlList = new ArrayList<String>();

					// 1) get all links and replace them by filename
					Source source = new Source(result);
					String encode = source.getEncoding();

					// save new record to index.html
					String title = "index";
					List<Element> tlist = source
							.getAllElements(HTMLElementName.TITLE);
					if (tlist != null && tlist.size() > 0) {
						title = source.getAllElements(HTMLElementName.TITLE)
								.get(0).getContent().getTextExtractor()
								.toString();
					}
					saveRecord2Index(title, "file://" + path);

					List<Element> list = source
							.getAllElements(HTMLElementName.A);
					for (Element ele : list) {
						String href = ele.getAttributeValue("href");
						urlList.add(href);
						String fileName = parseFileNameByUrl(href);
						result = result.replace(href, fileName);
					}

					// 2)deal with images
					list = source.getAllElements(HTMLElementName.IMG);
					for (Element ele : list) {

						// deal with images
						String img = ele.getAttributeValue("src");
						urlList.add(img);
						String fileName = parseFileNameByUrl(img);
						result = result.replace(img, fileName);
					}

					// 2. save to sdcard
					save2card(result, path, encode);

					// 3.save all sub-pages
					for (String subUrl : urlList) {
						saveUrl(subUrl, dir, false);
					}

				} else {

					// save to sdcard
					save2card(EntityUtils.toByteArray(resEntity), path);

					// save new record to index.html
					saveRecord2Index(parseFileNameByUrl(url), "file://" + path);

					// if the file is apk then send it to installer
					if (path.endsWith(".apk")) {
						sendApk2Installer(path);
					}
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
	}

	private void saveUrl(String url, String dir, boolean index) {
		HttpEntity resEntity = null;
		initDir(dir);
		try {
			ApplicationEx app = (ApplicationEx) this.activity.getApplication();

			HttpClient httpclient = app.getHttpClient();

			HttpGet httpget = new HttpGet(url);

			HttpResponse response = httpclient.execute(httpget);

			int status = response.getStatusLine().getStatusCode();

			String path = dir + parseFileNameByUrl(url);

			if (status == HttpStatus.SC_OK) {

				resEntity = response.getEntity();

				// interpret link in this page
				if (resEntity.getContentType() != null
						&& resEntity.getContentType().getValue()
								.startsWith("text")) {

					String result = EntityUtils.toString(resEntity);
					// 1.parse the page and replace charset
					Source source = new Source(result);
					String encode = source.getEncoding();

					// 1)save new record to index.html
					if (index) {
						String title = source
								.getAllElements(HTMLElementName.TITLE).get(0)
								.getContent().getTextExtractor().toString();
						saveRecord2Index(title, "file://" + path);
					}

					// 2. save to sdcard
					save2card(result, path, encode);

				} else {

					// save to sdcard
					save2card(EntityUtils.toByteArray(resEntity), path);

					// save new record to index.html
					if (index) {
						saveRecord2Index(parseFileNameByUrl(url), "file://"
								+ path);
					}

					// if the file is apk then send it to installer
					if (path.endsWith(".apk")) {
						sendApk2Installer(path);
					}
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
	}

	public String convertUrl2Path(String url) {
		int start = url.indexOf(".com/");
		if (start > 0) {
			String tmp = url.substring(start + 4);
			tmp = tmp.replace("?", "_");
			tmp = tmp.replace("/", "_");
			if (!tmp.endsWith(".html")) {
				tmp += ".html";
			}
			return tmp;
		} else {
			return null;
		}
	}
	
	public String getPath(String url){
		int start = url.indexOf(".com/");
		if (start > 0) {
			String tmp = url.substring(start + 4);
			return tmp;
		} else {
			return null;
		}
	}
	
	public String getCurrentPath(String url){
		int end = url.lastIndexOf("/");
		if (end > 0) {
			String tmp = url.substring(0, end+1);
			return tmp;
		} else {
			return null;
		}
	}

	public String saveHtml(String url) {
		// 1.remove ? & / from url and get the path
		String dir = initBaseDir() + "/temp/";
		initDir(dir);
		String path = convertUrl2Path(url);
		if (path == null) {
			return url;
		}
		path = dir + path;
		File tmp = new File(path);
		if (tmp.exists()
				&& (new Date().getTime() - tmp.lastModified() < 3600000)) {
			return "file://" + path;
		}else if(tmp.exists()){
			tmp.delete();
		}

		HttpEntity entity = null;
		try {
			HttpClient httpclient = new DefaultHttpClient();

			// 1.go any page
//			HttpGet httpget = new HttpGet(
//					"http://www.bizhizhan.com/ad/about.html");
//
//			HttpResponse response = httpclient.execute(httpget);
//			entity = response.getEntity();
//			entity.consumeContent();
			
			// loop download all of images from url
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpget);

			int status = response.getStatusLine().getStatusCode();

			if (status == HttpStatus.SC_OK) {
				entity = response.getEntity();
				if (entity != null) {
					String content = EntityUtils.toString(entity, "gbk");
					
					//1.get the original page and parse it
					MobileWParserUtil parser = new MobileWParserUtil();
					parser.setEncode("gbk");
					parser.setLanguage(((MobileWallpaperActivity)activity).getLanguage());
					parser.setPath(getPath(url));
					content = parser.parseLine(content);
					
					//2.replace the href
					Source source = new Source(content);
					String cPath = getCurrentPath(url);

					List<Element> list = source.getAllElements(HTMLElementName.A);
					for (Element ele : list) {
						String href = ele.getAttributeValue("href");
						String newHref = "";
						if (href == null)
							continue;
						if (href.startsWith("/")) {

							// href value is directly used as path
							newHref = "http://www.bizhizhan.com" + href;
						} else {
							// same path prefix without "."
							newHref = cPath + href;
						}
						String tmp1 = ele.toString();
						String tmp2 = tmp1.replace(href, newHref);
						content = content.replace(tmp1, tmp2);
						
					}

					// 2.replace the image src
					list = source
							.getAllElements(HTMLElementName.IMG);
					for (Element ele : list) {
						String src = ele.getAttributeValue("src");

						if (src != null) {
							String newsrc = saveImg(httpclient, "http://www.bizhizhan.com" + src);
							String tmp1 = ele.toString();
							String tmp2 = tmp1.replace(src, newsrc);
							content = content.replace(tmp1, tmp2);
						}
					}
					// 3.save html page
					save2card(content.getBytes("gbk"), path);

					// get content
					return "file://" + path;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}

	public String saveImg(HttpClient httpclient, String url) {
		boolean flag = httpclient == null;
		if(flag){
			httpclient = new DefaultHttpClient();
		}
		String dir = initBaseDir() + "/temp/";
		String path = dir + parseFileNameByUrl(url);
		File tmp = new File(path);
		if (tmp.exists()) {
			return "file://" + path;
		}
		initDir(dir);
		HttpEntity entity = null;
		try {
			// go to the current page
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpget);
			entity = response.getEntity();

			// save to sdcard
			save2card(EntityUtils.toByteArray(entity), path);
			return "file://" + path;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private String getPageDir() {
		SimpleDateFormat formatter4datetime = new SimpleDateFormat(
				"yyyy-MM-dd-HH-mm-ss");

		String dir = formatter4datetime.format(new Date());
		return basePath + "/html/" + dir + "/";
	}

	private String getPicDir() {
		SimpleDateFormat formatter4datetime = new SimpleDateFormat(
				"yyyy-MM-dd-HH-mm-ss");

		String dir = formatter4datetime.format(new Date());
		return basePath + "/pic/" + dir + "/";
	}

	private String parseFileNameByUrl(String url) {
		String res = "index.html";
		int start = url.indexOf("p=") + 2;
		String fileName = url.substring(start);
		start = fileName.indexOf("=");
		if (start >= 0) {
			res = fileName.substring(start + 1) + ".html";
		}
		start = fileName.lastIndexOf("/");
		if (start >= 0) {
			res = fileName.substring(start + 1);
		}
		res = res.replace("&", "_");
		res = res.replace("=", "_");
		res = res.replace("%", "_");
		return res;

	}

	private void save2card(byte[] bytes, String path) {
		try {
			// save to sdcard
			FileOutputStream fos = new FileOutputStream(new File(path));
			IOUtils.write(bytes, fos);

			// release all instances
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void save2card(String content, String path, String encode) {
		try {
			// save to sdcard
			FileOutputStream fos = new FileOutputStream(new File(path));
			IOUtils.write(content, fos, encode);

			// release all instances
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getDir(int id) {
		String dir = "";
		if (id == R.id.menu_save_this_link
				|| id == R.id.menu_save_next_level_link) {
			dir = getPageDir();
		} else {
			dir = getPicDir();
		}
		return dir;
	}

	public String initBaseDir() {
		File sdDir = Environment.getExternalStorageDirectory();
		File uadDir = null;
		if (sdDir.exists() && sdDir.canWrite()) {

		} else {
			sdDir = Environment.getDataDirectory();

		}
		uadDir = new File(sdDir.getAbsolutePath() + "/download/");
		if (uadDir.exists() && uadDir.canWrite()) {

			String path = getIndexPath();
			path = path.substring(0, path.indexOf("index.html"));
			String content = "<html><head><title>Favorite Downloaded</title><meta "
					+ "http-equiv='Content-Type' content='text/html; charset=UTF-8'>"
					+ "</head><body><table border='0'><tr><td><a href='"
					+ "file://"
					+ path
					+ "index.html"
					+ "'>All</a></td>"
					+ "<td><a href='"
					+ "file://"
					+ path
					+ "html/index.html"
					+ "'>Html</a></td>"
					+ "<td><a href='"
					+ "file://"
					+ path
					+ "pic/index.html"
					+ "'>Picture</td></tr><tr><td></td><tr></table><br><br></body></html>";

			// top index initiation
			File index = new File(path + "index.html");
			if (index.exists() && uadDir.canWrite()) {

			} else {

				save2card(content, path + "index.html", "UTF-8");
			}
			// html index initiation
			index = new File(path + "html/index.html");
			if (index.exists() && uadDir.canWrite()) {

			} else {
				save2card(content, path + "html/index.html", "UTF-8");
			}
			// picture index initiation
			index = new File(path + "pic/index.html");
			if (index.exists() && uadDir.canWrite()) {

			} else {
				save2card(content, path + "pic/index.html", "UTF-8");
			}
		} else {
			uadDir.mkdir();
			File hDir = new File(sdDir.getAbsolutePath() + "/download/html/");
			hDir.mkdir();
			File pDir = new File(sdDir.getAbsolutePath() + "/download/pic/");
			pDir.mkdir();
		}
		return uadDir.getAbsolutePath();
	}

	private void saveRecord2Index(String title, String url) {

		String type = "";
		if (url.indexOf("/html/") > 0) {
			type = "Html";
		} else {
			type = "Image";
		}

		try {
			String path = getIndexPath();
			String dir = path.substring(0, path.indexOf("index.html"));
			String content = IOUtils.toString(new FileInputStream(
					new File(path)));
			SimpleDateFormat formatter4datetime = new SimpleDateFormat(
					"yyyy-MM-dd-HH-mm-ss");
			int allTitle = content.indexOf(">All</a>");
			if (allTitle < 0) {
				content = content.replace("<tr><td></td><tr>",
						"<tr><td><a href='" + "file://" + dir + "index.html"
								+ "'>All</a></td>" + "<td><a href='"
								+ "file://" + dir + "html/index.html"
								+ "'>Html</a></td>" + "<td><a href='"
								+ "file://" + dir + "pic/index.html"
								+ "'>Picture</td></tr><tr><td></td><tr>");
			}

			// top index file updated
			content = content.replace(
					"<tr><td></td><tr>",
					"<tr><td></td><tr><tr><td><a href=\"" + url + "\">" + title
							+ "</td><td>"
							+ formatter4datetime.format(new Date())
							+ "</td><td>" + type + "</td></tr>");
			save2card(content, path, "UTF-8");

			// second level index updated
			if (type.equals("Html")) {
				path = dir + "html/index.html";
				content = IOUtils.toString(new FileInputStream(new File(path)));
			} else {
				path = dir + "pic/index.html";
				content = IOUtils.toString(new FileInputStream(new File(path)));
			}
			content = content.replace(
					"<tr><td></td><tr>",
					"<tr><td></td><tr><tr><td><a href=\"" + url + "\">" + title
							+ "</td><td>"
							+ formatter4datetime.format(new Date())
							+ "</td><td>" + type + "</td></tr>");
			save2card(content, path, "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initDir(String dir) {
		File sdDir = new File(dir);
		if (sdDir.exists() && sdDir.canWrite()) {

		} else {
			sdDir.mkdirs();

		}
	}

	public static String getIndexPath() {
		File sdDir = Environment.getExternalStorageDirectory();
		if (sdDir.exists() && sdDir.canWrite()) {

		} else {
			sdDir = Environment.getDataDirectory();

		}
		File index = new File(sdDir.getAbsolutePath() + "/download/index.html");
		return index.getAbsolutePath();
	}

	private void sendApk2Installer(String fileName) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(new File(fileName)),
				"application/vnd.android.package-archive");
		activity.startActivity(intent);
	}

}
