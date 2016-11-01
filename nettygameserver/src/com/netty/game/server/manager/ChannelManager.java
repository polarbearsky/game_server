package com.netty.game.server.manager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.netty.game.server.domain.GameUser;

import io.netty.channel.Channel;

public class ChannelManager {
	public final static ChannelManager instance = new ChannelManager();
	
	private ConcurrentHashMap<Channel, GameUser> channel_User = new ConcurrentHashMap<Channel, GameUser>();
	private ConcurrentHashMap<String, GameUser> name_User = new ConcurrentHashMap<String, GameUser>();
	
	public boolean addUser(GameUser user){
		if(channel_User.containsKey(user.getChannel()) || name_User.containsKey(user.getUserName())){
			return false;
		}
		channel_User.put(user.getChannel(), user);
		name_User.put(user.getUserName(), user);
		return true;
	}
	
	public GameUser getUser(Channel channel){
		return channel_User.get(channel);
	}
	
	public void removeUser(Channel channel){
		GameUser user = channel_User.remove(channel);
		if(user != null){
			name_User.remove(user.getUserName());
		}
	}
	
	public Collection<GameUser> getAllUsers(){
		return channel_User.values();
	}
}
