package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;
import java.util.List;

import com.altratek.altraserver.message.MsgConstants;
import com.altratek.altraserver.message.ResponseMessage;

public class XtResMessageB extends ResponseMessage {
	public XtResMessageB(String data, List<SocketChannel> recipients) {
		super(MsgConstants.MSGTYPE_EXTENSION_B, recipients);
		putString(data);
	}

	public XtResMessageB(byte[] arrByteData, List<SocketChannel> recipients) {
		super(MsgConstants.MSGTYPE_EXTENSION_B, recipients);
		this.buffer.put(arrByteData);
	}
}