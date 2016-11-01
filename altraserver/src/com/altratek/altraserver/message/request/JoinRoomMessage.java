package com.altratek.altraserver.message.request;

import com.altratek.altraserver.buffer.IoBuffer;

public class JoinRoomMessage extends RequestMessage {
	public String targetRoomName;
	public boolean leaveOldRoom;
	public int oldRoomId;
	public boolean ignoreMaxCount;
	public String customProp;

	JoinRoomMessage(IoBuffer buffer) {
		super(buffer);
	}

	@Override
	void parse() throws Exception {
		targetRoomName = getString();
		leaveOldRoom = getBool();
		oldRoomId = getInt();
		ignoreMaxCount = getBool();
		customProp = getString();
	}
}