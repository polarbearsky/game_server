package com.altratek.altraserver;

import com.altratek.altraserver.logger.ServerLogger;

public class ThreadUncaughtExceptionHandler implements
		Thread.UncaughtExceptionHandler {

	private static Thread.UncaughtExceptionHandler instance = new ThreadUncaughtExceptionHandler();

	public void uncaughtException(Thread t, Throwable e) {
		String msg = String.format("an uncaught exception of %s/%s", t.getId(),
				t.getName());
		ServerLogger.error(msg, e);
	}

	public static Thread.UncaughtExceptionHandler getHandler() {
		return instance;
	}
}
