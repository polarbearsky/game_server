package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;
import java.util.List;

import com.altratek.altraserver.message.*;

public class PubMsgResopnseMessage extends ResponseMessage {
	
	public PubMsgResopnseMessage(int roomId,int userId,String pubmsg, List<SocketChannel> recipients){
		super(RspMsgType.pubMsg, roomId, recipients);
		putInt(userId);
		putString(pubmsg);
	}

}
