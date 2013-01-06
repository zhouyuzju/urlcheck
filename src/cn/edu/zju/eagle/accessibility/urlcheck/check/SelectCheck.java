/*
 * @(#)SelectCheck.java 1.0 2012/08/18
 * 
 * Copyright (c) 浙江大学 Eagle实验室。
 * 保留所有权利(All Rights Reserved)。
 */
package cn.edu.zju.eagle.accessibility.urlcheck.check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * 检测select控件：对select控件，或有title属性，或有label for标签。
 * 2012行标5.2.4_可操作非文本控件（等级1）：如果非文本内容是一个控件或接受用户输入，那么它要有一个能说明其目的名称。
 * 
 * @author cxy
 * @version 1.0 2012-5-28
 * @desription Check F68:the association of label and user interface controls
 *             not being programmatically determinable
 * 
 */
public class SelectCheck{
	private static Logger log = Logger.getLogger(SelectCheck.class);
	public static final String LABLE_FOR = "<label\\s*(\\w*=\"[^\"]*\"\\s*)+/?>";
	public static final String SELECT = "<select[^<>]*>";
	
	/**
	 * @param input
	 *            : content of one web-page
	 * @param url
	 *            : the url of the web-page (without host url)
	 * @return List<string> : a list of string which is ids ;<br/>
	 *         以list的形式，返回label for标签所指向的id的集合;<br/>
	 *         label for="***(id)">
	 */
	private static List<String> lableCheck(String input) {
		Pattern pattern = Pattern.compile(LABLE_FOR);
		Matcher matcher = pattern.matcher(input);
		List<String> labelFor = new ArrayList<String>();
		while (matcher.find()) {
			String label = matcher.group();
			String[] splits = label.split("\\s+");
			for (String str : splits) {
				int index = str.indexOf('=');
				if (index != -1) {
					if (str.substring(0, index).equals("for")) {

						// case：<label for="q5_3">
						if (str.endsWith(">")) {
							labelFor.add(str.substring(index + 1,
									str.lastIndexOf("\"")).replaceAll("\"", ""));
						} else {
							labelFor.add(str.substring(index + 1).replaceAll(
									"\"", ""));
						}
					}
				}
			}
		}
		return labelFor;
	}

	public static String check(String html) {
		Map<String, String> isExist = new HashMap<String, String>();

		Pattern pattern = Pattern.compile(SELECT);
		// 将被匹配的正文全部小写后，然后，替换掉其中的"&gt;"和"&lt;"和"&quot";
		String content = html.toLowerCase();
		content = content.replaceAll("&gt;", ">");
		content = content.replaceAll("&lt;", "<");
		content = content.replaceAll("&quot;", "\"");
		Matcher match = pattern.matcher(content);

		// 得到该页面所有的label for的List<String>,其中String为从label中提取的for指向的id
		List<String> labelFor = lableCheck(content);

		while (match.find()) {
			isExist.clear();
			String input = match.group();

			// 对匹配出的select进行属性分解
			String[] attributes = input.split("\\s+");
			for (String attr : attributes) {
				int index = attr.indexOf('=');
				if (index != -1) {
					if (attr.endsWith(">")) {// 特殊case，比如type="text">
						isExist.put(
								attr.substring(0, index),
								attr.substring(index + 1,
										attr.lastIndexOf("\"")).replaceAll(
										"\"", ""));
					} else {
						isExist.put(attr.substring(0, index),
								attr.substring(index + 1).replaceAll("\"", ""));
					}
				}
			}

			if (!isExist.containsKey("title")) {// 肯定无title
				if (!isExist.containsKey("id") || !labelFor.contains(isExist.get("id")))
				{
					html = html.replace(input, input + "<img src='./image/onlinecheck_error.jpg' alt='错误：select标签要有对应的label for标签或title属性'" +
							" title='错误：select标签要有对应的label for标签或title属性'/>");
					log.info(input + "错误：select标签要有对应的label for标签或title属性");
				}
			}
			// 肯定有title
		}
		
		return html;
	}
}
