package com.altratek.altraserver.message.request;

import java.util.HashMap;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.domain.UserVariable;

public class SetUvarsMessage extends RequestMessage {

	public HashMap<String, UserVariable> userVariables = null;

	SetUvarsMessage(IoBuffer buffer) {
		super(buffer);
	}

	@Override
	void parse() throws Exception {
		userVariables = new HashMap<String, UserVariable>();
		int userVariabeCount = getShort();
		for (int i = 0; i < userVariabeCount; i++) {
			String name = getString();
			String type = new StringBuilder().append(getChar()).toString();
			String value = getString();
			userVariables.put(name, new UserVariable(type, value));
		}
	}
}
