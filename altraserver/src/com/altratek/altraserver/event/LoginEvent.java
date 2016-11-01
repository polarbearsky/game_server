package com.altratek.altraserver.event;

import java.nio.channels.SocketChannel;

import com.altratek.altraserver.domain.Zone;

public class LoginEvent extends SystemEvent {	
	public final String userName;
	public final String password;
	public final SocketChannel socketChannel;

	public LoginEvent(Zone zone, String userName, String password, SocketChannel socketChannel) {
		super(SystemEvent.Login, zone, null);		
		this.userName = userName;
		this.password = password;
		this.socketChannel = socketChannel;
	}
}

