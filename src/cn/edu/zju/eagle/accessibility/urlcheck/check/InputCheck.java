/*
 * @(#)InputCheck.java 1.0 2012/08/18
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
 * （1）对于下面类型的控件，要显式的使用label标签：text、check-box、radio、file、password、text-area、
 * select；<br/>
 * （2）对于下面类型的控件，一般不使用label标签：submit &reset 按钮、 Image 按钮、Hidden input
 * fields、button按钮；<br/>
 * （3）当不能或者不适合用label标签的时候，可以使用title属性；当鼠标悬停在包含title属性的输入元素上，会显示a
 * tool-tips,用户代理（辅助技术）可以读出title属性。<br/>
 * 因而：<br/>
 * （1）对radio、check-box、password必须使用label for标签；<br/>
 * （2）對text、file有label for標誌，或者有title屬性；<br/>
 * （3）對button要有value屬性，且其值不為空，且不使用label for；<br/>
 * （4）對type=image的，要有alt屬性，且alt不为空；<br/>
 * （5）對type=submit或reset的，要有value屬性；<br/>
 * （6）對type=hidden，不使用label for。<br/>
 * 2012行标5.2.4_可操作非文本控件（等级1）：如果非文本内容是一个控件或接受用户输入，那么它要有一个能说明其目的名称。
 * 
 * @author XuFeng
 * @version 1.0,2012-05-25
 */
public class InputCheck{
	private static Logger log = Logger.getLogger(InputCheck.class);
	public static final String LABLE_FOR = "<label\\s*(\\w*=\"[^\"]*\"\\s*)+/?>";
	public static final String INPUT = "<input\\s*(\\w*=\"[^\"]*\"\\s*)+/?>";
	
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
							labelFor.add(str.substring(index + 1, str.lastIndexOf("\"")).replaceAll("\"",""));
						} else {
							labelFor.add(str.substring(index + 1).replaceAll("\"",""));
						}
					}
				}
			}
		}
		return labelFor;
	}
	

	/**
	 * @param html
	 *            : content(html tags) of one web-page
	 * @return nothing: just print out those that do not meet the requirement
	 */
	public static String check(String html) {
		Map<String, String> isExist = new HashMap<String, String>();
		
		Pattern pattern = Pattern.compile(INPUT);

		// 将被匹配的正文全部小写后，然后，替换掉其中的"&gt;"和"&lt;"和"&quot";
		String content = html.toLowerCase();
		content = content.replaceAll("&gt;", ">");
		content = content.replaceAll("&lt;", "<");
		content = content.replaceAll("&quot;", "\"");
		Matcher match = pattern.matcher(content);
		
		// 得到该页面所有的label for的List<String>,其中String为从label中提取的for指向的id
		List<String> labelFor = lableCheck(content);
		
		while (match.find()) {
			
			// 解析出匹配的input语句中的各个属性<key,value>=<attr,attr-value>
			isExist.clear();
			String input = match.group();
			String[] attributes = input.split("\\s+");
			for (String attr : attributes) {
				int index = attr.indexOf('=');
				if (index != -1) {
					if (attr.endsWith(">")) {// 特殊case，比如type="text">
						isExist.put(attr.substring(0, index),
								attr.substring(index + 1,attr.lastIndexOf("\"")).replaceAll("\"", ""));
					} else {
						isExist.put(attr.substring(0, index),
								attr.substring(index + 1).replaceAll("\"", ""));
					}
				}
			}
			
			// input属性中有type属性
			if (isExist.containsKey("type")) {
				String type = isExist.get("type");
				if (type.equals("hidden")) {
					//（6）對type=hidden，不使用label for。
					if (isExist.containsKey("id") && labelFor.contains(isExist.get("id"))) 
					{
						html = html.replace(input, input + "<img src='./image/onlinecheck_error.jpg' alt='错误：input标签type=hidden不能有对应的label for标签'" +
								" title='错误：input标签type=hidden不能有对应的label for标签'/>");
						log.info(input + "错误：input标签type=hidden不能有对应的label for标签");
					}
				}
				else if (type.equals("submit") || type.equals("reset")) {
					//（5）對type=submit或reset的，要有value屬性
					if (!isExist.containsKey("value") || isExist.get("value").isEmpty())
					{
						html = html.replace(input, input + "<img src='./image/onlinecheck_error.jpg' alt='错误：input标签type=submit或reset要有非空的value属性'" +
								" title='错误：input标签type=submit或reset要有非空的value属性'/>");
						log.info(input + "错误：input标签type=submit或reset要有非空的value属性");
					}
				}
				else if (type.equals("image")) {
					// （4）對type=image的，要有alt屬性，且alt不为空
					if (!isExist.containsKey("alt") ||isExist.get("alt").isEmpty())
					{
						html = html.replace(input, input + "<img src='./image/onlinecheck_error.jpg' alt='错误：input标签type=image要有非空的alt属性'" +
								" title='错误：input标签type=image要有非空的alt属性'/>");
						log.info(input + "错误：input标签type=image要有非空的alt属性");
					}	
				} 
				else if (type.equals("button")) {
					// （3）對button要有value屬性，且其值不為空，且不使用label for
					if (!isExist.containsKey("value") || isExist.get("value").isEmpty()) 
					{
						html = html.replace(input, input + "<img src='./image/onlinecheck_error.jpg' alt='错误：input标签type=button要有非空的value属性'" +
								" title='错误：input标签type=button要有非空的value属性'/>");
						log.info(input + "错误：input标签type=button要有非空的value属性");
					}
					else if (isExist.containsKey("id") && labelFor.contains(isExist.get("id"))) 
					{
						html = html.replace(input, input + "<img src='./image/onlinecheck_error.jpg' alt='错误：input标签type=button不能由对应的label for标签'" +
								" title='错误：input标签type=button不能由对应的label for标签'/>");
						log.info(input + "错误：input标签type=button不能由对应的label for标签");
					}
				} 
				else if (type.equals("text") || type.equals("file")){
					//（2）對text、file有label for標誌，或者有title屬性；
					if (!isExist.containsKey("title")) {
						if (!isExist.containsKey("id") || !labelFor.contains(isExist.get("id")))
						{
							html = html.replace(input, input + "<img src='./image/onlinecheck_error.jpg' alt='错误：input标签type=text或file要有对应的label for标签或者非空title属性'" +
									" title='错误：input标签type=text或file要有对应的label for标签或者非空title属性'/>");
							log.info(input + "错误：input标签type=text或file要有对应的label for标签或者非空title属性");
						}
					}
				} 
				else if (type.equals("radio") || type.equals("checkbox") || type.equals("password")) {
					// 对radio、check-box、password必须使用label for标签
					if (!isExist.containsKey("id") || !labelFor.contains(isExist.get("id")))
					{
						html = html.replace(input, input + "<img src='./image/onlinecheck_error.jpg' alt='错误：input标签type=radio、check-box、password要有对应的label for标签'" +
								" title=='错误：input标签type=radio、check-box、password要有对应的label for标签'/>");
						log.info(input + "错误：input标签type=radio、check-box、password要有对应的label for标签");
					}
				} 
			}
			else {
			    // case:input中没有type属性
				html = html.replace(input, input + "<img src='./image/onlinecheck_error.jpg' alt='错误：input标签没有type属性'" +
						" title='错误：input标签没有type属性'/>");
				log.info(input + "错误：input标签没有type属性");
			}
		}
		return html;
	}

}
