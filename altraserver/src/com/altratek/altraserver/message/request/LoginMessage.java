package com.altratek.altraserver.message.request;

import com.altratek.altraserver.buffer.IoBuffer;

public class LoginMessage extends RequestMessage {

	public String zone;
	public String nick;
	public String pwd;

	LoginMessage(IoBuffer buffer) {
		super(buffer);
	}

	@Override
	void parse() throws Exception {
		zone = getString();
		nick = getString();
		pwd = getString();
	}
}
