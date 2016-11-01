package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;
import java.util.List;

import com.altratek.altraserver.message.*;

public class UserGoneMessage extends ResponseMessage {
	
	public UserGoneMessage(int fromRoom, int userId, List<SocketChannel> recipients){
		super(RspMsgType.userGone,fromRoom, recipients);
		putInt(userId);
	}
}
