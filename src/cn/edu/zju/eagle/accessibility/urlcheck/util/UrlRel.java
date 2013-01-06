package cn.edu.zju.eagle.accessibility.urlcheck.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uci.ics.crawler4j.url.URLCanonicalizer;
import edu.uci.ics.crawler4j.url.WebURL;
import edu.uci.ics.crawler4j.util.Config;
import edu.uci.ics.crawler4j.util.Debug;

public class UrlRel {

	private static final Pattern filters = Pattern.compile(
			".*(\\.(mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf"
					+ "|rm|smil|wmv|swf|wma|zip|rar|gz))$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern staticFilePatterns = Pattern.compile(
			".*(\\.(js|css|ashx|bmp|gif|jpe?g|png|tiff?|ico))$",
			Pattern.CASE_INSENSITIVE);

	/**
	 * 对html文件中的url进行重定向，使其指向本地资源 TODO 把swf定向到非本地的绝对路径，可以系列reconstructure一下
	 * 
	 * @param rHtml
	 *            待修改的网页源码
	 * @param subDomain
	 *            未经替换的子域名，据此判断是否在统一子域名下
	 * @param domain
	 *            未经替换的域名，据此判断是否为统一站点
	 * @param validPath
	 *            本地存储的path，据此判断深度，得到supdirs
	 * @return
	 */
	public static String redirectUrlsInHtml(String rHtml, WebURL weburl) {
		/*
		 * script中的形式会导致问题: ga.src = ('https:' == document.location.protocol ?
		 * 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
		 */
		String regstr = "((href|src)\\s*=\\s*['\"]\\s*)([^\\s'\">]*)([\\s'\">])";
		Pattern urlFilter = Pattern.compile(regstr, Pattern.CASE_INSENSITIVE);
		Matcher matchRes = urlFilter.matcher(rHtml);

		StringBuffer sb = new StringBuffer();
		String wholeMatch = null, urlMatch = null, preMatch = null, postMatch = null;
		while (matchRes.find()) {
			wholeMatch = matchRes.group(0);
			urlMatch = matchRes.group(3);
			preMatch = matchRes.group(1);
			postMatch = matchRes.group(4);
			// for test
			// System.out.println("regex\t" + regstr + "\t\turlMatch:  "
			// + urlMatch);
			// curl是标准化后的url 地址中不得含有中括号 ExtractLinks中类似代码
			String curl = URLCanonicalizer.getCanonicalURL(urlMatch.trim(),
					weburl.getURL());
			WebURL cweburl = new WebURL();
			if (curl != null) {
				cweburl.setURL(curl);
			} else {
				/* 匹配到的链接不规范，忽略之 */
				cweburl.setURL("http://www.fakeUrl.com/");
				Debug.checkLogger.debug("curl不规范：\t" + curl + "\t<--\t"
						+ urlMatch);
			}
			/* 判断是否redirect */
			String replacement = null;
			if (shouldRedirect(cweburl, weburl)) {
				String locRelPath = getFullValidDomain(cweburl)
						+ cweburl.getPath();
				replacement = preMatch + locRelPath
						+ postMatch;
			} else {
				replacement = wholeMatch;
			}
			/* MARK 使用Matcher.quoteRepalcement()过滤特殊字符 */
			matchRes.appendReplacement(sb,
					Matcher.quoteReplacement(replacement));
		}
		matchRes.appendTail(sb);
		return sb.toString();
	}
	/**
	 * 将url合法化：包括获取端口，对特殊字符转化
	 * 
	 * @param weburl
	 * @return
	 */
	public static String getFullValidDomain(WebURL weburl) {
		String fullValidDomain = null;
		fullValidDomain = (weburl.getSubDomain() == "") ? (weburl.getDomain())
				: (weburl.getSubDomain() + "." + weburl.getDomain());
		if (weburl.getPort() != 80) {
			fullValidDomain += ":" + weburl.getPort();
		}
		return "http://" + fullValidDomain;
	}

	/**
	 * 和SnapshotCrawler中的shouldVisit方法一致
	 * 
	 * @param url
	 * @return
	 */
	private static boolean shouldRedirect(WebURL url, WebURL context) {
		String href = url.getURL().toLowerCase();
		// ignore radio video etc...
		if (filters.matcher(href).matches()) {
			return false;
		}

		// download static files like:pic, js, css
		if (staticFilePatterns.matcher(href).matches()) {
			return true;
		}

		// sub domain
		if (!Config.isCrossSubDomains()
				&& !url.getSubDomain().equals(context.getSubDomain())) {
			return false;
		}

		// port
		if (!Config.isCrossPorts() && url.getPort() != context.getPort()) {
			return false;
		}

		if (url.getDomain().equals(context.getDomain())) {
			return true;
		}
		return false;

	}

	public static void main(String args[]) {
		// String rHtml = null, path = "blog/main.html";
		// rHtml =
		// "href = \" /abc/def.html\"   src = \" http://baike.cdpsn.org.cn/123/456\" ";
		// System.out.println(redirectUrls(rHtml, "www", "cdpsn.org.cn", path));
		String rHtml = "<img src='/themes/default/images/logo.gif' width='80' height='80' title='网站首页' alt='网站首页'>";
		WebURL weburl = new WebURL();
		// // ip不能解析
		// //
		weburl.setURL("http://www.scdpf.org.cn:8080/Content/ggtz/ggtz1.html");
		// weburl.setURL("http://10.214.43.12:8080/a/b/c");
		// System.out.println(weburl.getPath());
		// System.out.println(weburl.getShortPath());
		System.out.println(UrlRel.redirectUrlsInHtml(rHtml, weburl));
	}
}
