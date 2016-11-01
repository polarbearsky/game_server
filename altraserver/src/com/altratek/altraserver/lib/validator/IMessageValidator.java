package com.altratek.altraserver.lib.validator;

import com.altratek.altraserver.domain.UserVariable;

public interface IMessageValidator {
	public int nextMsgSeqNo(int now, int userId, int sessionId);

	public void decryptMsg(byte[] data, int msgSeqNo);

	public boolean validateUserVariable(final String varName, final UserVariable userVar);
}