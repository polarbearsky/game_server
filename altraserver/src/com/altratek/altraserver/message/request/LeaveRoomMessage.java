package com.altratek.altraserver.message.request;

import com.altratek.altraserver.buffer.IoBuffer;

public class LeaveRoomMessage extends RequestMessage {

	public int leaveRoomId;

	LeaveRoomMessage(IoBuffer buffer) {
		super(buffer);
	}

	@Override
	void parse() throws Exception {
		leaveRoomId = getInt();
	}
}
