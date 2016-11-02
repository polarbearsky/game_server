package com.netty.game.server.domain;

import io.netty.channel.Channel;

public class GameUser {
	private String userName;
	private Channel channel;
	
	public GameUser(String userName, Channel channel) {
		this.userName = userName;
		this.channel = channel;
	}

	public String getUserName() {
		return userName;
	}

	public Channel getChannel() {
		return channel;
	}
}
