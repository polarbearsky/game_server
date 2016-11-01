package com.altratek.altraserver.lib.validator;

import com.altratek.altraserver.domain.UserVariable;

public class DefaultMessageValidator implements IMessageValidator {
	@Override
	public int nextMsgSeqNo(int now, int userId, int sessionId) {
		long next = now * 2L + 1; // now *2 + 1���ܻᵼ��int��������Զ���long
		return next >= Integer.MAX_VALUE ? 0 : (int) next;
	}

	private byte[] decryptKey = new byte[4];

	@Override
	public void decryptMsg(byte[] data, int msgSeqNo) {
		// decryptKeyֻ�ڵ��߳�ʹ�ã����Զ����Ա��������������ֲ�����
		// ��Ϊ�����Ա��������ÿ�ζ���һ��С���飬ʡ�ڴ档
		// �����ʡ�ڴ棬��������ByteBuffer.allocate(4).putInt(msgSeqNo).array();
		decryptKey[0] = (byte) (msgSeqNo >> 24);
		decryptKey[1] = (byte) (msgSeqNo >> 16);
		decryptKey[2] = (byte) (msgSeqNo >> 8);
		decryptKey[3] = (byte) (msgSeqNo >> 0);

		for (int i = 0; i < data.length; i++) {
			// i &3 ���෽ʽ ��i % decryptKey.lengthЧ���Ըߣ����ö����Ȳ���
			data[i] = (byte) (data[i] ^ decryptKey[i % decryptKey.length]);
		}
	}

	@Override
	public boolean validateUserVariable(final String varName, final UserVariable userVar) {
		return true;
	}
}
