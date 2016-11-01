package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;
import java.util.List;

import com.altratek.altraserver.message.*;

public class CreateRmKOMessage extends ResponseMessage {

	public CreateRmKOMessage(String errMsg, List<SocketChannel> recipients) {
		super(RspMsgType.createRmKO, 0, recipients);
		putString(errMsg);
	}

}
