package com.altratek.altraserver.message.request;

import java.util.HashMap;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.domain.RoomVariable;

public class SetRvarsMessage extends RequestMessage {

	public HashMap<String, RoomVariable> newRoomVariables = null;
	public boolean setOwner;

	SetRvarsMessage(IoBuffer buffer) {
		super(buffer);
	}

	@Override
	void parse() throws Exception {
		setOwner = getBool();
		newRoomVariables = new HashMap<String, RoomVariable>();
		int roomVariabeCount = getShort();
		for (int i = 0; i < roomVariabeCount; i++) {
			String name = getString();
			String type = new StringBuilder().append(getChar()).toString();
			String value = getString();
			boolean persistent = getBool();
			boolean priv = getBool();
			newRoomVariables.put(name, new RoomVariable(type, value, null, persistent, priv));
		}
	}
}
