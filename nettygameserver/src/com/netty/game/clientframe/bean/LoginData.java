package com.netty.game.clientframe.bean;

public class LoginData {
	public final String ip;
	public final int port;
	public final String userName;
	public final String pwd;
	
	public LoginData(String ip, int port, String userName, String pwd) {
		this.ip = ip;
		this.port = port;
		this.userName = userName;
		this.pwd = pwd;
	}
}
