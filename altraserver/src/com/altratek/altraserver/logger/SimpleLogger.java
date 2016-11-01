package com.altratek.altraserver.logger;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.altratek.altraserver.util.AltraServerUtils;

// 开服和关服单独记录log文件
// log4j未初始化时，也用这个logger。在logge未初始化之前，启动失败了，没日志不方便。
public class SimpleLogger {
	private static Logger logger;

	public static void init(String logDir) {
		FileHandler fh = null;
		logger = Logger.getLogger("server_on_off");
		try {
			File f = new File(logDir);
			if (!f.exists()) {
				f.mkdir();
			}
			String logFilePath = AltraServerUtils.combinePath(logDir, "server_on_off.log");
			fh = new FileHandler(logFilePath, true);
		} catch (Exception e) {
			System.out.print("Error while initing ConsoleHandler/FileHandler");
			e.printStackTrace();
			System.exit(1);
		}
		fh.setLevel(Level.FINE);
		fh.setFormatter(new SimpleLogFileFormatter());
		logger.addHandler(fh);
		
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(Level.FINE);
		ch.setFormatter(new SimpleLogFileFormatter());
		logger.addHandler(ch);
	}

	public static void info(String msg) {
		logger.log(Level.INFO, msg);		
	}
	
	public static void info(Object msg) {
		logger.log(Level.INFO, msg == null ? "null" : msg.toString());		
	}
	
	public static void infof(String msg, Object... args) {
		logger.log(Level.INFO, String.format(msg, args));
	}
	
	public static void error(String msg, Throwable t) {
		logger.log(Level.WARNING, markErrorMsg(msg), t);
	}

	public static void error(String msg) {
		logger.log(Level.WARNING, markErrorMsg(msg));
	}

	private static String markErrorMsg(String msg) {
		return "********** " + msg;
	}
}
