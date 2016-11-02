package com.netty.game.server.manager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.netty.game.server.domain.GameUser;

import io.netty.channel.Channel;

public class ChannelManager {
	public final static ChannelManager instance = new ChannelManager();
	
	private ConcurrentHashMap<Channel, GameUser> channel_User = new ConcurrentHashMap<Channel, GameUser>();
	
	public boolean addUser(GameUser user){
		return channel_User.putIfAbsent(user.getChannel(), user) == null;
	}
	
	public void removeUser(Channel channel){
		channel_User.remove(channel);
	}
	
	public Collection<GameUser> getAllUsers(){
		return channel_User.values();
	}
}
