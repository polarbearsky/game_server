package com.netty.game.server;

public class ServerLogger {
	public static void warn(String msg){
		System.err.println(msg);
	}
	
	public static void warn(String msg, Throwable throwable){
		System.err.println(msg);
		System.err.println(throwable.toString());
	}
	
	public static void info(String msg){
		System.out.println(msg);
	}
	
	public static void errorf(String msg, Throwable throwable, Object... args) {
		System.err.println(String.format(msg, args));
		System.err.println(throwable.toString());
	}
}
