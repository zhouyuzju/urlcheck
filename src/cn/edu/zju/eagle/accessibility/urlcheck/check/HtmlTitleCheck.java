/*
 * @(#)HtmlTitleCheckNew.java 0.1 2012/06/29
 * 
 * Copyright (c) 浙江大学 Eagle实验室。
 * 保留所有权利(All Rights Reserved)。
 */
package cn.edu.zju.eagle.accessibility.urlcheck.check;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


import net.paoding.analysis.analyzer.PaodingAnalyzer;
import net.paoding.analysis.analyzer.PaodingTokenizer;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;

import Jama.Matrix;

/**
 * 该类用来计算 网页标题 与 网页正文的关联度的
 * 
 * @author XuFeng
 * @version 0.10 2012-06-29
 */
public class HtmlTitleCheck{
	private static Logger log = Logger.getLogger(HtmlTitleCheck.class);

	/**
	 * 对字串进行分词
	 * 
	 * @param content
	 *            被分词的内容
	 * @return 返回分词的结果，<分词，词频>
	 */
	public static HashMap<String, Integer> strAnalyzer(String content) {
		HashMap<String, Integer> contentWord = new HashMap<String, Integer>();

		Reader r = new StringReader(content);
		Analyzer analyzer = new PaodingAnalyzer();
		PaodingTokenizer ts = (PaodingTokenizer) analyzer.tokenStream("", r);

		Token t;
		try {
			while ((t = ts.next()) != null) {
				String str = t.termText();
				if (contentWord.containsKey(str)) {
					contentWord.put(str, contentWord.get(str) + 1);
				} else {
					contentWord.put(str, 1);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return contentWord;
	}

	/**
	 * 计算网页标题与正文之间的余弦cos
	 * 
	 * @param title
	 *            标题分词后的结果
	 * @param content
	 *            正文分词后的结果
	 * @return
	 */
	public static double computeCosRelation(HashMap<String, Integer> title,
			HashMap<String, Integer> content) {
		double relation = 0;

		/* 初始化set：将hash中的key全部加入set中，构建向量的维度 */
		Set<String> dimensions = new HashSet<String>();
		Iterator<Entry<String, Integer>> iterTitle = title.entrySet()
				.iterator();
		while (iterTitle.hasNext()) {
			Map.Entry<String, Integer> entry = (Entry<String, Integer>) iterTitle
					.next();
			dimensions.add(entry.getKey());
		}
		Iterator<Entry<String, Integer>> iterContent = content.entrySet()
				.iterator();
		while (iterContent.hasNext()) {
			Map.Entry<String, Integer> entry = (Entry<String, Integer>) iterContent
					.next();
			dimensions.add(entry.getKey());
		}

		/* 构建title和content的向量 */
		int n = dimensions.size();
		if (n == 0) {// 0维空间，则直接返回0
			return 0;
		}
		double[][] titleVector = new double[1][n];
		double[][] contentVector = new double[1][n];
		int index = 0;
		for (String str : dimensions) {
			if (title.containsKey(str)) {
				titleVector[0][index] = (double) title.get(str);
			} else {
				titleVector[0][index] = 0;
			}
			if (content.containsKey(str)) {
				contentVector[0][index] = (double) content.get(str);
			} else {
				contentVector[0][index] = 0;
			}
			index++;
		}

		/* 计算title向量与content向量之间的余弦cos: a*b/(|a|*|b|) */
		Matrix titleMatrix = new Matrix(titleVector);
		Matrix contentMatrix = new Matrix(contentVector);
		double ab = titleMatrix.times(contentMatrix.transpose()).get(0, 0);
		double a = Math.sqrt(titleMatrix.times(titleMatrix.transpose()).get(0,
				0));
		double b = Math.sqrt(contentMatrix.times(contentMatrix.transpose())
				.get(0, 0));
		if (a == 0 || b == 0) {
			return 0;
		}
		relation = ab / a / b;

		return relation;
	}

	/**
	 * @param html
	 *            :输入的一个网页，包含标题、网页代码等；
	 * @return 计算的标题与正文之间的关联度
	 */
	public static String check(String html) {
		// 计时
		Long startTime = System.currentTimeMillis();

		// 声明变量
		HashMap<String, Integer> contentHashMap = new HashMap<String, Integer>();
		HashMap<String, Integer> titleHashMap = new HashMap<String, Integer>();

		// 提取html页面正文部分
		TextExtractor te = new TextExtractor();
		te.extractHTML(html);
		Long endExtractTime = System.currentTimeMillis();
		log.info("提取正文耗时：" + (endExtractTime - startTime) + "ms");

		if (!te.getText().equals("*推测您提供的网页为非主题型网页，目前暂不处理！:-)")) {
			Long startAnaTime = System.currentTimeMillis();
			// 对网页标题、网页正文内容进行分词
			contentHashMap = strAnalyzer(te.getText());
			titleHashMap = strAnalyzer(te.getTitle());

			Long endAnaTime = System.currentTimeMillis();
			log.info("分词耗时：" + (endAnaTime - startAnaTime) + "ms");

			// 计算网页标题与网页正文之间的关联度
			double relation = computeCosRelation(titleHashMap, contentHashMap);

			Long computeEndTime = System.currentTimeMillis();
			log.info("计算耗时：" + (computeEndTime - endAnaTime) + "ms");

			// 返回结果，打印结果
			if (log.isInfoEnabled()) {
				log.info(relation);
				log.info(titleHashMap.toString());
				log.info(contentHashMap.toString());
			}
			if(relation < 0.1)
				html = html.replace("<body>", "<body><img src='./image/onlinecheck_error.jpg' alt='错误：标题与正文不相关'" +
						" title='错误：标题与正文不相关'/>");
		} else {
			if (te.getTitle().equals("")) {
				html = html.replace("<body>", "<body><img src='./image/onlinecheck_error.jpg' alt='错误：标题为空'" +
						" title='错误：标题为空'/>");// 表示空标题, 或者标题与正文无关
			} else {
				html = html.replace("<body>", "<body><img src='./image/onlinecheck_error.jpg' alt='错误：非主题型网页，无正文，或被提取的页面无法分辨正文'" +
						" title='错误：非主题型网页，无正文，或被提取的页面无法分辨正文'/>");
				// -1 表示是非主题型网页，无正文，或被提取的页面无法分辨正文
			}
		}

		Long endTime = System.currentTimeMillis();
		log.info("check func:check耗时：" + (endTime - startTime) + "ms");

		return html;
	}
}
