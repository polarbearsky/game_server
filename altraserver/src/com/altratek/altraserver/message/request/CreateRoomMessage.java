package com.altratek.altraserver.message.request;

import java.util.HashMap;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.domain.RoomVariable;

public class CreateRoomMessage extends RequestMessage {
	public String roomName;
	public String password;
	public boolean isTemp;
	public boolean isGame;
	public int maxSpectators;
	public int maxUsers;
	public boolean exit;
	public boolean updateRoomUserCount;

	public String extensionName;
	public String extensionType;
	public boolean setOwner = false;
	public HashMap<String, RoomVariable> newRoomVariables = null;

	CreateRoomMessage(IoBuffer buffer) {
		super(buffer);
	}

	@Override
	void parse() throws Exception {
		roomName = getString();
		password = getString();
		isTemp = getBool();
		isGame = getBool();
		maxSpectators = getShort();
		maxUsers = getShort();
		exit = getBool();
		updateRoomUserCount = getBool();
		extensionName = getString();
		extensionType = getString();
		setOwner = getBool();
		int size = getShort();
		newRoomVariables = new HashMap<String, RoomVariable>();
		for (int i = 0; i < size; i++) {
			String name = getString();
			String type = new StringBuilder().append(getChar()).toString();
			String value = getString();
			boolean persistent = getBool();
			boolean priv = getBool();
			newRoomVariables.put(name, new RoomVariable(type, value, null, persistent, priv));
		}
	}
}
