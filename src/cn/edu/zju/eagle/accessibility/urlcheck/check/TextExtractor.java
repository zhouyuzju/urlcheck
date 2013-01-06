package cn.edu.zju.eagle.accessibility.urlcheck.check;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * 主题型网页正文抽取，比较适合于新闻和Blog的正文抽取.<br />
 * 采用《基于行块分布函数的通用网页正文抽取》的算法，该算法时间复杂度为线性， 不需要建立DOM树，且不依赖HTML标签。<br />
 * 首先将网页正文抽取问题转化为求页面的行块分布函数，这种方法不用建立Dom树， 不被病态HTML所累（事实上与HTML标签完全无关）。
 * 通过在线性时间内建立的行块分布函数图，直接准确定位网页正文。
 * 
 * @author Fandy Wang(lfwang@ir.hit.edu.cn)
 * @date 2010.08.12
 */
public class TextExtractor {
	/** 打印日志 */
	private static Logger textExtractorLog = Logger
			.getLogger(TextExtractor.class);
	/** 行块大小. */
	private static Integer _block = 3;

	/** The Constant _titlePattern. */
	private final static String _titlePattern = "<title>(.*?)</title>";// |&lt;title&gt;([^<>]*)&lt;/title&gt;";

	/** The Constant _titleRegexPattern. */
	private final static Pattern _titleRegexPattern = Pattern.compile(
			_titlePattern, Pattern.CANON_EQ | Pattern.DOTALL
					| Pattern.CASE_INSENSITIVE);

	/** The _title. */
	private String _title = "";

	/** The _text. */
	private String _text = "";

	/**
	 * Sets the block.
	 * 
	 * @param block
	 *            the new block
	 */
	@SuppressWarnings("unused")
	private static void setBlock(Integer block) {
		_block = block;
	}

	/**
	 * Extract url.
	 * 
	 * @param url
	 *            the url
	 */
	public void extractURL(String url) {
		url = url.trim();
		if (isLegalURL(url)) {
			if (isContentURL(url)) {
				String htmlText = DownloadURL.downURL(url, "IE8.0");
				extractHTML(htmlText);
			} else {
				_text = "*推测您提供的网页为非主题型网页，目前暂不处理！:-)";
			}
		} else {
			_title = "*非法URL:-)";
		}
	}

	/**
	 * Extract url.
	 * 
	 * @param url
	 *            the url
	 * @param encoding
	 *            the encoding
	 */
	public void extractURL(String url, String encoding) {
		url = url.trim();
		if (isLegalURL(url)) {
			if (isContentURL(url)) {
				String htmlText = DownloadURL.downURL(url, encoding, "IE8.0");
				extractHTML(htmlText);
			} else {
				_text = "*推测您提供的网页为非主题型网页，目前暂不处理！:-)";
			}
		} else {
			_title = "*非法URL:-)";
		}
	}

	/**
	 * Checks if is content url.
	 * 
	 * @param url
	 *            the url
	 * 
	 * @return true, if is content url
	 */
	private boolean isContentURL(String url) {
		int count = 0;
		for (int i = 0; i < url.length() - 1 && count < 3; i++) {
			if (url.charAt(i) == '/')
				count++;
		}

		return count > 2;
	}

	/**
	 * Checks if is legal url.
	 * 
	 * @param url
	 *            the url
	 * 
	 * @return true, if is legal url
	 */
	private boolean isLegalURL(String url) {
		if (url == null || url.length() == 0) {
			return false;
		}

		String regEx = "^(http|https|ftp)\\://([a-zA-Z0-9\\.\\-]+(\\:[a-zA-"
				+ "Z0-9\\.&%\\$\\-]+)*@)?((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{"
				+ "2}|[1-9]{1}[0-9]{1}|[1-9])\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}"
				+ "[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|"
				+ "[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-"
				+ "4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])|([a-zA-Z0"
				+ "-9\\-]+\\.)*[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,4})(\\:[0-9]+)?(/"
				+ "[a-zA-Z0-9\\.\\,\\?\\'\\\\/\\+&%\\$\\=~_\\-@]*)*$";
		Pattern p = Pattern.compile(regEx);
		Matcher matcher = p.matcher(url);
		if (textExtractorLog.isInfoEnabled()) {
			textExtractorLog.info(matcher.matches());
			// System.out.println(matcher.matches());
		}

		return matcher.matches();
	}

	/**
	 * Extract html.
	 * 
	 * @param htmlText
	 *            the html text
	 */
	public void extractHTML(String htmlText) {
		// System.out.println(htmlText);
		extractTitle(htmlText);
		htmlText = preProcess(htmlText);
		if (!isContentPage(htmlText)) {
			_text = "！非主题型网页！";
			return;
		}
		// System.out.println(htmlText);

		List<String> lines = Arrays.asList(htmlText.split("\n"));
		List<Integer> indexDistribution = lineBlockDistribute(lines);

		List<String> textList = new ArrayList<String>();
		List<Integer> textBeginList = new ArrayList<Integer>();
		List<Integer> textEndList = new ArrayList<Integer>();

		// System.out.println("Ha" + lines.toString());
		for (int i = 0; i < indexDistribution.size(); i++) {
			if (indexDistribution.get(i) > 0) {
				StringBuilder tmp = new StringBuilder();
				textBeginList.add(i);
				while (i < indexDistribution.size()
						&& indexDistribution.get(i) > 0) {
					// System.out.println("test" + lines.get(i).length() +
					// lines.get(i));
					if (lines.get(i).length() >= 2) {// 当文本足够长
						tmp.append(lines.get(i)).append("\n");
					}
					i++;
				}
				textEndList.add(i);
				textList.add(tmp.toString());
			}
		}

		// 如果两块只差两个空行，并且两块包含文字均较多，则进行块合并，以弥补单纯抽取最大块的缺点
		for (int i = 1; i < textList.size(); i++) {
			if (textBeginList.get(i) == textEndList.get(i - 1) + 1
					&& textEndList.get(i) > textBeginList.get(i) + _block
					&& textList.get(i).replaceAll("\\s+", "").length() > 40) {
				if (textEndList.get(i - 1) == textBeginList.get(i - 1) + _block
						&& textList.get(i - 1).replaceAll("\\s+", "").length() < 40) {
					continue;
				} else if (textList.get(i - 1).toLowerCase()
						.contains("copyright")
						|| textList.get(i - 1).contains("版权所有")
						|| textList.get(i).toLowerCase().contains("copyright")
						|| textList.get(i).contains("版权所有")) {
					continue;
				}
				textList.set(i - 1, textList.get(i - 1) + textList.get(i));
				textEndList.set(i - 1, textEndList.get(i));

				textList.remove(i);
				textBeginList.remove(i);
				textEndList.remove(i);
				--i;
			}
		}

		String result = "";
		for (String text : textList) {
			// System.out.println("text:" + text + "\n" +
			// text.replaceAll("\\s+", "").length());
			if (text.replaceAll("\\s+", "").length() > result.replaceAll(
					"\\s+", "").length())
				result = text;
		}

		// 最长块长度小于100，归为非主题型网页
		if (result.replaceAll("\\s+", "").length() < 50) // 100 )
			_text = "*推测您提供的网页为非主题型网页，目前暂不处理！:-)";
		else if (result.toLowerCase().contains("copyright")
				|| result.contains("版权所有")) {
			_text = "*推测您提供的网页为非主题型网页，目前暂不处理！:-)";
		} else
			_text = result;
	}

	/**
	 * Checks if is content page. 以逗号、句号为判断，判断网页中至少是否有5个逗号或句号。
	 * 
	 * @param htmlText
	 *            the html text
	 * 
	 * @return true, if is content page
	 */
	private boolean isContentPage(String htmlText) {
		int count = 0;
		for (int i = 0; i < htmlText.length() && count < 5; i++) {
			if (htmlText.charAt(i) == '，' || htmlText.charAt(i) == '。')
				count++;
		}

		return count >= 5;
	}

	/**
	 * Extract title.
	 * 
	 * @param htmlText
	 *            the html text
	 */
	private void extractTitle(String htmlText) {
		Matcher m1 = _titleRegexPattern.matcher(replaceSpecialChar(htmlText));

		if (m1.find()) {
			_title = replaceSpecialChar(m1.group(1));
		}
		_title = _title.replaceAll("\n+", "");
	}

	/**
	 * Gets the title.
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return _title;
	}

	/**
	 * Gets the text.
	 * 
	 * @return the text
	 */
	public String getText() {
		return _text;
	}

	/**
	 * Line block distribute.
	 * 
	 * @param lines
	 *            the lines
	 * 
	 * @return the list< integer>
	 */
	private List<Integer> lineBlockDistribute(List<String> lines) {
		List<Integer> indexDistribution = new ArrayList<Integer>();

		for (int i = 0; i < lines.size(); i++) {
			lines.set(i, lines.get(i).replaceAll("\\s+", ""));
			indexDistribution.add(lines.get(i).length());
		}
		// 删除上下存在两个空行的文字行
		/*
		 * for (int i = 0; i+4 < lines.size(); i++) { if(
		 * indexDistribution.get(i) == 0 && indexDistribution.get(i+1) == 0 &&
		 * indexDistribution.get(i+2) > 0 && indexDistribution.get(i+2) < 40 &&
		 * indexDistribution.get(i+3) == 0 && indexDistribution.get(i+4) == 0 )
		 * { //System.out.println("line:" + lines.get(i+2)); lines.set(i+2, "");
		 * indexDistribution.set(i+2, 0); i += 3; } }
		 */

		for (int i = 0; i < lines.size() - _block; i++) {
			int wordsNum = indexDistribution.get(i);
			for (int j = i + 1; j < i + _block && j < lines.size(); j++) {
				wordsNum += indexDistribution.get(j);
			}
			indexDistribution.set(i, wordsNum);
		}

		return indexDistribution;
	}

	/**
	 * Pre processing.
	 * 
	 * @param htmlText
	 *            the html text
	 * 
	 * @return the string
	 */
	private String preProcess(String htmlText) {
		// DTD
		htmlText = htmlText.replaceAll("(?is)<!DOCTYPE.*?>", "");
		// html comment
		htmlText = htmlText.replaceAll("(?is)<!--.*?-->", "");
		// js
		htmlText = htmlText.replaceAll("(?is)<script.*?>.*?</script>", "");
		// css
		htmlText = htmlText.replaceAll("(?is)<style.*?>.*?</style>", "");
		// added 2012-07-14 将图片中的内容替换为图片的等效替代文本，alt属性的内容
		htmlText = htmlText.toLowerCase().replaceAll(
				"<img[\\s\\S]*?alt=(\"|')([^<>]*?)(\"|')[\\s\\S]*?/>", "$2");
		// added 防止html中在<>中包括大于号的判断
		// htmlText = htmlText.replaceAll("<[^>'\"]*['\"].*['\"].*?>", "");
		// html
		htmlText = htmlText.replaceAll("<[\\s\\S]*?>", "");
		htmlText = htmlText.replaceAll("<.*?>", "");
		htmlText = htmlText.replaceAll("\r\n", "\n");

		return replaceSpecialChar(htmlText);
	}

	/**
	 * Replace special char.
	 * 
	 * @param content
	 *            the content
	 * 
	 * @return the string
	 */
	private String replaceSpecialChar(String content) {
		String text = content.replaceAll("&quot;", "\"");
		text = text.replaceAll("&ldquo;", "“");
		text = text.replaceAll("&rdquo;", "”");
		text = text.replaceAll("&middot;", "·");
		text = text.replaceAll("&#8231;", "·");
		text = text.replaceAll("&#8212;", "——");
		text = text.replaceAll("&#28635;", "濛");
		text = text.replaceAll("&hellip;", "…");
		text = text.replaceAll("&#23301;", "嬅");
		text = text.replaceAll("&#27043;", "榣");
		text = text.replaceAll("&#8226;", "·");
		text = text.replaceAll("&#40;", "(");
		text = text.replaceAll("&#41;", ")");
		text = text.replaceAll("&#183;", "·");
		text = text.replaceAll("&amp;", "&");
		text = text.replaceAll("&bull;", "·");
		text = text.replaceAll("&lt;", "<");
		text = text.replaceAll("&#60;", "<");
		text = text.replaceAll("&gt;", ">");
		text = text.replaceAll("&#62;", ">");
		text = text.replaceAll("&nbsp;", " ");
		text = text.replaceAll("&#160;", " ");
		text = text.replaceAll("&tilde;", "~");
		text = text.replaceAll("&mdash;", "—");
		text = text.replaceAll("&copy;", "@");
		text = text.replaceAll("&#169;", "@");
		text = text.replaceAll("♂", "");
		text = text.replaceAll("&.{2,5};|&#.{2,5};", " "); // remove special
															// char

		return text;
	}

	public static void main(String[] args) {
		TextExtractor te = new TextExtractor();
		te.extractURL("http://news.sina.com.cn/c/2012-09-10/045925136560.shtml");
		System.out.println(te.getText());
	}

}
