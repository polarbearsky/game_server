package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;
import java.util.List;

import com.altratek.altraserver.message.ResponseMessage;
import com.altratek.altraserver.message.RspMsgType;

public class AdminMsgMessage extends ResponseMessage {

	public AdminMsgMessage(String adminMsg, List<SocketChannel> recipients) {
		super(RspMsgType.dmnMsg, 0, recipients);
		putInt(0); // 纯为了兼容客户端的api
		putString(adminMsg);
	}
}
