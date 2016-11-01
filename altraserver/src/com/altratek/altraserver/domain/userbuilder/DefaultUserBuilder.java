package com.altratek.altraserver.domain.userbuilder;

import java.nio.channels.SocketChannel;

import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.Zone;

public class DefaultUserBuilder implements IUserBuilder {
	@Override
	public User build(SocketChannel channel, String name, Zone zone) {
		return new User(channel, name, zone);
	}

	@Override
	public User build(SocketChannel channel, String name, Zone zone, int userId) {
		return new User(channel, name, zone, userId);
	}
}