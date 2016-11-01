package com.altratek.altraserver.message.request;

import com.altratek.altraserver.buffer.IoBuffer;

public class PubMsgMessage extends RequestMessage {

	public String pubMsg;

	PubMsgMessage(IoBuffer buffer) {
		super(buffer);
	}

	@Override
	void parse() throws Exception {
		pubMsg = getString();
	}
}
