package com.altratek.altraserver.lib.validator;

import com.altratek.altraserver.domain.UserVariable;

public class DefaultMessageValidator implements IMessageValidator {
	@Override
	public int nextMsgSeqNo(int now, int userId, int sessionId) {
		long next = now * 2L + 1; // now *2 + 1可能会导致int溢出，所以定义long
		return next >= Integer.MAX_VALUE ? 0 : (int) next;
	}

	private byte[] decryptKey = new byte[4];

	@Override
	public void decryptMsg(byte[] data, int msgSeqNo) {
		// decryptKey只在单线程使用，所以定义成员变量，而不定义局部变量
		// 因为定义成员变量避免每次都开一个小数组，省内存。
		// 如果不省内存，简单做法：ByteBuffer.allocate(4).putInt(msgSeqNo).array();
		decryptKey[0] = (byte) (msgSeqNo >> 24);
		decryptKey[1] = (byte) (msgSeqNo >> 16);
		decryptKey[2] = (byte) (msgSeqNo >> 8);
		decryptKey[3] = (byte) (msgSeqNo >> 0);

		for (int i = 0; i < data.length; i++) {
			// i &3 求余方式 比i % decryptKey.length效率略高，不好懂，先不用
			data[i] = (byte) (data[i] ^ decryptKey[i % decryptKey.length]);
		}
	}

	@Override
	public boolean validateUserVariable(final String varName, final UserVariable userVar) {
		return true;
	}
}
