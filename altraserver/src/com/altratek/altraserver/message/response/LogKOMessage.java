package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;

import com.altratek.altraserver.message.*;

public class LogKOMessage extends ResponseMessage {

	public LogKOMessage(String errMsg, SocketChannel recipient) {
		super(RspMsgType.logKO, 0, asSocketChannelList(recipient));
		putString(errMsg);
	}
}
