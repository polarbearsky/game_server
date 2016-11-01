package com.altratek.altraserver.domain.userbuilder;

import java.nio.channels.SocketChannel;

import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.Zone;

public interface IUserBuilder {
	public User build(SocketChannel channel, String name, Zone zone);
	public User build(SocketChannel channel, String name, Zone zone, int userId);
}