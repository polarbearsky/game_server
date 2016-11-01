package com.altratek.altraserver.message.request;

import com.altratek.altraserver.buffer.IoBuffer;

public class AsObjMessage extends RequestMessage {
	public String data;

	AsObjMessage(IoBuffer buffer) {
		super(buffer);
	}

	@Override
	void parse() throws Exception {
		data = getString();
	}
}
