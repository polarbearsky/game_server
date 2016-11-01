package com.altratek.altraserver.logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.util.AltraServerUtils;

public class ServerLogger {
	private static Logger logger;
	private static Logger profileExtension;
	private static Logger profileMsgLengthDebug;
	private static Logger serverStat;
	private static Logger invalidMsg;

	public static volatile boolean infoEnabled;
	public static volatile boolean debugEnabled;

	public static void initLogger(String logConfigFileDir, boolean appendLogRootDir) throws Exception {

		String log4jxml = AltraServerUtils.combinePath(logConfigFileDir, ConfigData.LOG4J_XML_FILE);

		if (appendLogRootDir) {
			DOMConfigurator.configure(appendLogRootDir(log4jxml));
		} else {
			DOMConfigurator.configure(log4jxml);
		}

		logger = Logger.getLogger("ServerLogger");
		profileExtension = Logger.getLogger("ExtensionMonitor");
		profileMsgLengthDebug = Logger.getLogger("MsgLengthDebug");
		serverStat = Logger.getLogger("ServerStat");
		invalidMsg = Logger.getLogger("Plug");

		setLoggerSwitch();
	}
	
	private static void setLoggerSwitch() {
		infoEnabled = logger.isInfoEnabled();
		debugEnabled = logger.isDebugEnabled();
	}
	
	// 供测试代码用
	public static void initLogger() {
		try {
			initLogger("", false);
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}

	public static void destroy() {
	}

	public static boolean isDebugEnabled() {
		return debugEnabled;
	}

	public static boolean isInfoEnabled() {
		return infoEnabled;
	}

	public static void debug(String msg, Throwable t) {
		logger.debug(msg, t);
	}

	public static void debug(String msg) {
		logger.debug(msg);
	}

	public static void debugf(String msg, Object... args) {
		logger.debug(String.format(msg, args));
	}

	public static void info(String msg) {
		logger.info(msg);
	}

	public static void infof(String msg, Object... args) {
		logger.info(String.format(msg, args));
	}

	public static void error(String msg) {
		logger.error(msg);
	}

	public static void errorf(String msg, Object... args) {
		logger.error(String.format(msg, args));
	}

	public static void error(String msg, Throwable t) {
		logger.error(msg, t);
	}

	public static void errorf(String msg, Throwable t, Object... args) {
		logger.error(String.format(msg, args), t);
	}

	public static void prof(Object item, Object subItem, long startTime) {
		profNum(item, subItem, System.currentTimeMillis() - startTime);
	}

	public static void profNum(Object item, Object subItem, long num) {
		prof(String.format("%s/%s/%s", item, subItem, num));
	}

	public static void prof(String msg) {
		profileExtension.info(msg);
	}

	public static void msgLengthDebug(String msg) {
		profileMsgLengthDebug.info(msg);
	}

	public static void serverStat(String msg) {
		serverStat.info(msg);
	}

	public static void invalidMsg(User u, String msg) {
		// null user记录了也没用，无法追究责任，忽略
		if (u == null) {
			return;
		}

		String name = u.getName();
		int id = u.getUserId();

		// 加四个----与扩展写的日志区分
		invalidMsg.info(String.format("----\t%s\t%s\t%s", id, name, msg));
	}

	// 在用logConfig.xml初始化之后，为什么还要动态set log level呢？
	// 这个level在线上环境通常需要重新集中配置，在读取集中配置之前，代码里就用到logger
	// 所以先初始化一个用着，之后读取到level再改变设置
	// 这一点是很别扭的一个逻辑！
	public static void SetLevel(String levelName) {
		if (levelName == null) {
			return;
		}

		Map<String, Level> levelMap = new HashMap<String, Level>();
		levelMap.put("all", Level.ALL);
		levelMap.put("info", Level.INFO);
		levelMap.put("debug", Level.DEBUG);
		levelMap.put("warn", Level.WARN);

		Level level = levelMap.get(levelName.toLowerCase());
		if (level != null) {
			logger.setLevel(level);
			setLoggerSwitch();			
		}
	}
	
	// 只用于显示
	public static Level getLevel() {
		return logger.getLevel();
	}

	// 把配置中的相对路径加上绝对路径（日志根目录）
	private static Element appendLogRootDir(String log4jxml) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// Make DocumentBuilder.parse ignore DTD references, 'log4j.dtd'
		// http://stackoverflow.com/questions/155101/make-documentbuilder-parse-ignore-dtd-references
		dbf.setFeature("http://xml.org/sax/features/namespaces", false);
		dbf.setFeature("http://xml.org/sax/features/validation", false);
		dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(new File(log4jxml));
		Element root = dom.getDocumentElement();

		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression exp = xPath.compile("//appender/param[@name='File']");
		NodeList nodeList = (NodeList) exp.evaluate(dom, XPathConstants.NODESET);

		for (int i = 0; i < nodeList.getLength(); i++) {
			Element node = (Element) nodeList.item(i);
			String relativeFilePath = node.getAttribute("value");
			node.setAttribute("value", AltraServerUtils.combinePath(ConfigData.LOG_DIR, relativeFilePath));
		}

		return root;
	}
}