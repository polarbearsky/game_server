package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;
import java.util.List;

import com.altratek.altraserver.message.*;

public class DataObjMessage extends ResponseMessage {
	
	public DataObjMessage(int roomId, int userId, String dataObj, List<SocketChannel> recipients){
		super(RspMsgType.dataObj, roomId, recipients);
		putInt(userId);
		putString(dataObj);
	}

}
