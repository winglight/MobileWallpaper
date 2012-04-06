package com.omdasoft.mobile.wallpaper;

import java.util.List;

import com.google.api.translate.Language;
import com.google.api.translate.Translate;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

public class MobileWParserUtil extends BaseParserUtil {

	private int urlType = 0; // 0 - index ; 1 - wallpaper ; 2 - tags ; 3 -
								// others
	private static MobileWParserUtil instance;

	public MobileWParserUtil() {
	}

	/**
	 * Singleton access point to the manager.
	 */
	public static MobileWParserUtil getInstance() {
		synchronized (MobileWParserUtil.class) {
			if (instance == null) {
				instance = new MobileWParserUtil();
			}
		}

		return instance;
	}

	public String parseLine(String content) {
		return parseLine(content, path, language, true);
	}

	public String parseLine(String content, String path, String language,
			boolean fromCache) {
		String res = null;

		// Either fromCache was false or the object was not found, so
		// call loadBookVObj to create it

			res = parsePath(content, fromCache);

		return res;

	}

	private String parsePath(String content, boolean fromCache) {
		// handle path
		if (path.contains("/tags.php")) {
			this.urlType = 2;
		} else if (path.endsWith("/")) {
			this.urlType = 0;
		} else {
			String[] tpm = path.split("-");
			if (tpm.length == 2) {
				this.urlType = 0;
			} else if (tpm.length == 3) {
				this.urlType = 1;
			} else {
				if (path.contains("/list_")) {
					this.urlType = 2;
				} else {
					this.urlType = 3;
				}
			}
		}

		// get title
		Source source = new Source(content);
		List<Element> list = source.getAllElements(HTMLElementName.TITLE);
		String title = "index";
		if (list.size() > 0) {
			title = list.get(0).getContent().getTextExtractor().toString();
			// translate into english
			if ("en".equals(language)) {
				title = translate(title);
			}
		}

		// handle content parse
		switch (this.urlType) {
		case 0:
		case 2: {

			// get page links
			String page = "";
			Element pele = source.getFirstElementByClass("page padding5");
			if (pele != null) {
				List<Element> tlist = pele.getAllElements(HTMLElementName.A);
				for (Element ele : tlist) {
					page += ele.toString() + "&nbsp;&nbsp;";

				}
			}

			content = "";

			Element cele = source.getFirstElementByClass("cont_list");
			if (cele != null) {
				List<Element> tlist = cele.getAllElements(HTMLElementName.A);
				for (Element ele : tlist) {
					List<Element> tmpele = ele
							.getAllElements(HTMLElementName.IMG);

					if (tmpele != null && tmpele.size() > 0) {
						content += addTrTd(ele.toString());
					} else {
						Element bele = ele.getFirstElement();
						if (bele != null) {
							String tmp = bele.getTextExtractor().toString();
							int start = tmp.indexOf(",");
								if (start > 0 && (start - 7 >= 0)) {
									tmp = tmp.substring(start - 7);
								} else if (tmp.length() - 7 >= 0){

									tmp = tmp.substring(tmp.length() - 7);
								}
							if (tmp.endsWith("0")) {
								content += addTrTd(tmp);
							}
						}
					}

				}

			}

			content = addHeadEnd(content, title, page, encode);
			break;
		}
		case 1: {
			// get page links
			String page = "";
			Element pele = source.getFirstElementByClass("content-page");
			if (pele != null) {
				String tmp = pele.toString();
				tmp = tmp.replace("<a", "&nbsp;&nbsp;<a");
				page = addTrTd(tmp);
			}

			content = "";

			Element cele = source.getFirstElementByClass("pic_look");
			if (cele != null) {
				content += addTrTd(cele.toString());
				List<Element> plist = cele.getAllElements(HTMLElementName.P);
				for (Element ele : plist) {
					String tmp = ele.getTextExtractor().toString();
					if (tmp != null && !"".equals(tmp)) {
						int start = tmp.indexOf(",");
						if (start > 0 && (start - 7 >= 0)) {
							tmp = tmp.substring(start - 7);
						} else if (tmp.length() - 7 >= 0){

							tmp = tmp.substring(tmp.length() - 7);
						}
						if (tmp.endsWith("0")) {
							tmp = "<p>" + tmp + "</p>";
							content = content.replace(ele.toString(), tmp);
						} else {
							content = content.replace(ele.toString(), "");
						}
					}
				}
			}

			content = addHeadEnd(content, title, page, encode);
			break;
		}
		default:
			content = this.addHeadEnd("URL type error.", "Error", "", encode);
		}

		return content;
	}

	private String addTrTd(String content) {
		return "<TR><TD>" + content + "</TD></TR>";
	}

	@Override
	public String getReleaseUrl() {
		return "mobile_w_release";
	}
	
	public String translate(String src) {
		Translate.setHttpReferrer("http://www.omdasoft.com");

		String translatedText;
		try {
			translatedText = Translate.execute(src, Language.CHINESE,
					Language.ENGLISH);
		} catch (Exception e) {
			translatedText = src;
		}

		return translatedText;
	}
}