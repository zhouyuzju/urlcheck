package cn.edu.zju.eagle.accessibility.urlcheck.main;

import org.apache.log4j.Logger;

import cn.edu.zju.eagle.accessibility.urlcheck.check.HtmlTitleCheck;
import cn.edu.zju.eagle.accessibility.urlcheck.check.ImgLinkMissingAlt;
import cn.edu.zju.eagle.accessibility.urlcheck.check.ImgMissAltCheck;
import cn.edu.zju.eagle.accessibility.urlcheck.check.InputCheck;
import cn.edu.zju.eagle.accessibility.urlcheck.check.SelectCheck;
import cn.edu.zju.eagle.accessibility.urlcheck.check.TextareaCheck;
import cn.edu.zju.eagle.accessibility.urlcheck.util.OnlineCheckDAO;
import cn.edu.zju.eagle.accessibility.urlcheck.util.UrlRel;

import com.trolltech.qt.core.QTimer;
import com.trolltech.qt.core.QUrl;
import com.trolltech.qt.gui.*;
import com.trolltech.qt.webkit.QWebView;

import edu.uci.ics.crawler4j.url.WebURL;


/**
 * @desc: qt webkit render, read local html, css, js files and render them using
 *        webkit and then write to the database
 * @author: zhouyu
 * @copyright: 2012-06-21
 */
public class QtRender{
	private QWebView view; // render object
	private QTimer timer; // timer to monitor rending process
	private static Logger logger = Logger.getLogger(QtRender.class.getName());
	
	private long startTime;
	private long finishTime;
	private final int TIMEINTERVAL = 5000;
	
	private String html;
	
	public QtRender() {
		try {
			timer = new QTimer();
			view = new QWebView();
			timer.timeout.connect(this, "onLoadDelay()");
			view.loadStarted.connect(this, "onLoadStart()");
			view.loadFinished.connect(this, "onLoadFinish()");
			view.loadProgress.connect(this, "onLoadProgress()");
			timer.setSingleShot(false);
			timer.setInterval(TIMEINTERVAL);
			timer.start();
			
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
	}

	private void onLoadStart() {
		startTime = System.currentTimeMillis();
	}

	private void onLoadProgress() {

	}

	/**
	 * @desc if render a file finish, insert result into database, check css
	 *       element and then do load again
	 * @author zhouyu
	 * @date 2012-08-29
	 */
	private void onLoadFinish() {
		finishTime = System.currentTimeMillis();
		html = view.page().mainFrame().toHtml();
		logger.info("Load Finish!Cost Time: " + (finishTime - startTime));
		timer.stop();
		QApplication.instance().quit();
	}

	/**
	 * @desc when page rending delay(almost the case that there exists
	 *       unreachable resources),stop rending and continue
	 * @author zhouyu
	 * @date 2012-08-29
	 */
	private void onLoadDelay() {
		if ((System.currentTimeMillis() - startTime) > TIMEINTERVAL) {
			logger.warn("Load delay! cost: "
					+ (System.currentTimeMillis() - startTime) + "ms");
			view.stop();
		}
	}

	/**
	 * @desc public api for runing this render
	 * @author zhouyu
	 * @date 2012-08-29
	 */
	public void render(String url) {
		view.load(new QUrl(url));
	}
	
	/**
	 * 
	 * @Title: getHtml
	 * @Description: public api, get the rendering result
	 * @return String
	 * @throws
	 */
	public String getHtml(){
		return html;
	}
	
	public static void main(String args[]){
		QApplication.initialize(args);
		QtRender render = new QtRender();
		render.render(args[0]);
		QApplication.exec();
		
		String html = render.getHtml();
		WebURL weburl = new WebURL();
		weburl.setURL(args[0]);
		html = UrlRel.redirectUrlsInHtml(html, weburl);
		html = ImgLinkMissingAlt.check(html);	//5.2.3
		html = ImgMissAltCheck.check(html);	//5.2.5
		html = InputCheck.check(html);	//5.2.4
		html = SelectCheck.check(html);	//5.2.4
		html = TextareaCheck.check(html);	//5.2.3
		html = HtmlTitleCheck.check(html);	//7.3.1
		html = html.replaceFirst("(<.*?charset\\s*=\\s*['\"]?)[^'\";,\\s>]*?(['\";,\\s>])",
				"$1utf-8$2");
		
		int checkId = Integer.parseInt(args[1]);
		OnlineCheckDAO onlineCheckDAO = new OnlineCheckDAO();
		onlineCheckDAO.update(html, checkId);
	}
}
