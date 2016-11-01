package com.netty.game.server.exception;

import com.netty.game.server.ServerLogger;

public class ThreadUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

	private static Thread.UncaughtExceptionHandler instance = new ThreadUncaughtExceptionHandler();

	public void uncaughtException(Thread t, Throwable e) {
		String msg = String.format("an uncaught exception of %s/%s", t.getId(), t.getName());
		ServerLogger.warn(msg, e);
	}

	public static Thread.UncaughtExceptionHandler getHandler() {
		return instance;
	}
}
