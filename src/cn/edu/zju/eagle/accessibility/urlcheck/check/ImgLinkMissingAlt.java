/*
 * @(#)ImgLinkMissingAlt.java 1.0 2012/08/18
 * 
 * Copyright (c) 浙江大学 Eagle实验室。
 * 保留所有权利(All Rights Reserved)。
 */
package cn.edu.zju.eagle.accessibility.urlcheck.check;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


/**
 * Check F89:using null alt on an image where the image is the only content in a
 * link<br/>
 * 5.2.3（Level 1）的规则：为网页中的非文本链接提供替代文本，替代文本应说明链接目的或链接用途。
 * 
 * @author cxy
 * @version 1.0 2012-5-28
 */
public class ImgLinkMissingAlt{
	private static Logger log = Logger.getLogger(ImgLinkMissingAlt.class);
	public static final String LINK = "<a.*?>.*?</a>";
	public static final String IMG_IN_LINK = "<img.*?/?>";
	
	public static String check(String html) {
		Pattern pattern = Pattern.compile(LINK);

		// 将被匹配的正文全部小写后，然后，替换掉其中的"&gt;"和"&lt;"和"&quot";
		String content = html.toLowerCase();
		content = content.replaceAll("&gt;", ">");
		content = content.replaceAll("&lt;", "<");
		content = content.replaceAll("&quot;", "\"");
		Matcher match = pattern.matcher(content);

		while (match.find()) {
			
			// 匹配有图片的超链接
			Pattern imgPattern = Pattern.compile(IMG_IN_LINK);
			Matcher imgMatch = imgPattern.matcher(match.group());
			
			while(imgMatch.find()) {
			
				// alt为非空  : alt=""
				Pattern altNullPattern = Pattern.compile("alt\\s*=\\s*\"(\\s*[^\"\\s]+)+\\s*\"");
				Matcher altNullMatch = altNullPattern.matcher(imgMatch.group());
	
				// 若不满足有alt非空
				if (!altNullMatch.find()) {
					String patternString = match.group();
					log.info(patternString + "非文本链接（图片链接）需要有等效替代文本描述链接的目的或用途");
					html = html.replace(patternString, patternString + "<img src='./image/onlinecheck_error.jpg' alt='错误：非文本链接（图片链接）需要有等效替代文本描述链接的目的或用途'" +
							" title='错误：非文本链接（图片链接）需要有等效替代文本描述链接的目的或用途'/>");
				}
			}
		}

		/* 视情况返回结果,若该页面没有匹配的结果，则返回null */
		return html;
	}

}
