/*
 * @(#)TextareaCheck.java 1.0 2012/08/18
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
 * 若textarea区域不可编辑，不接受用户的输入，忽略之；<br/>
 * 若textarea区域可接收用户的输入，要有label for属性。<br/>
 * 2012行标5.2.4_可操作非文本控件（等级1）：如果非文本内容是一个控件或接受用户输入，那么它要有一个能说明其目的名称。
 * 
 * @author Dell
 * @version 1.0 2012-08-18
 */
public class TextareaCheck{
	private static Logger log = Logger.getLogger(TextareaCheck.class);
	public static final String LABLE_FOR = "<label\\s*(\\w*=\"[^\"]*\"\\s*)+/?>";
	public static final String TEXTAREA = "(<textarea[^<>]*>)([^<>]*)</textarea>";
	
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
		
		Pattern pattern = Pattern.compile(TEXTAREA);
		
		// 将被匹配的正文全部小写后，然后，替换掉其中的"&gt;"和"&lt;"和"&quot";
		String content = html.toLowerCase();
		content = content.replaceAll("&gt;", ">");
		content = content.replaceAll("&lt;", "<");
		content = content.replaceAll("&quot;", "\"");
		Matcher match = pattern.matcher(content);
		
		List<String> labelFor = lableCheck(content);
		while (match.find()) {
			
			// 分解<textarea ****>的属性
			isExist.clear();
			String input = match.group(1).toLowerCase();
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
			
			// 分析textarea中的属性
			String str_disabled = isExist.get("disabled");
			String str_readonly = isExist.get("readonly");
			
			// 得到textarea中的disabled、readonly属性的值
			boolean bl_disabled = false;
			boolean bl_readonly = false;
			if (str_disabled != null && str_disabled.equals("disabled")) {
				bl_disabled = true;
			}
			if (str_readonly != null && str_readonly.equals("true")) {
				bl_disabled = true;
			}
			
			// 反馈结果
			if (bl_disabled || bl_readonly) ;
			else if (!isExist.containsKey("id") || !labelFor.contains(isExist.get("id"))) 
			{
					html = html.replace(input, input + "<img src='./image/onlinecheck_error.jpg' alt='错误：textarea标签要有对应的label for标签'" +
							" title='错误：textarea标签要有对应的label for标签'/>");
					log.info(input + "错误：textarea标签要有对应的label for标签");
			}
		}

		return html;
	}

}
