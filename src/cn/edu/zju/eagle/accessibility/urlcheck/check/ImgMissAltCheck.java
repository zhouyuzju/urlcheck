package cn.edu.zju.eagle.accessibility.urlcheck.check;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class ImgMissAltCheck {
	private static Logger log = Logger.getLogger(ImgMissAltCheck.class);
	public static final String IMAG_MISSING_ALT = "<img\\s*((src|width|align|border|height|hspace|ismap|longdesc"
			+ "|usemap|vspace|class|dir|id|lang|title|style|onabort|onclick|ondblclick|onmousedown|onmousemove"
			+ "|onmouseout|onmouseover|onmouseup|onkeydown|onkeypress|onkeyup|onload)"
			+ "=\"[^\"]*\"\\s*)+/?>";
	
	public static String check(String html) {
		// 直接正则表达式匹配
		Pattern pattern = Pattern.compile(IMAG_MISSING_ALT);
		
		// 将被匹配的正文全部小写后，然后，替换掉其中的"&gt;"和"&lt;"和"&quot";
		String content = html.toLowerCase();
		content = content.replaceAll("&gt;", ">");
		content = content.replaceAll("&lt;", "<");
		content = content.replaceAll("&quot;", "\"");
		Matcher match = pattern.matcher(content);
		
		HashSet<String> illegalSets = new HashSet<String>();
		while (match.find()) {
			String item = match.group();
			log.info(item + "错误：img标签没有提供alt属性");
			illegalSets.add(item);
		}
		for(String item:illegalSets)
			html = html.replaceAll(item, item + "<img src='./image/onlinecheck_error.jpg' alt='错误：img标签没有提供alt属性'" +
					" title='错误：img标签没有提供alt属性'/>");
		return html;
	}
}
