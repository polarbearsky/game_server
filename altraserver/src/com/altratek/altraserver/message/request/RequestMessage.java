package com.altratek.altraserver.message.request;

import java.nio.charset.CharacterCodingException;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.MsgConstants;
import com.altratek.altraserver.message.ReqMsgType;

public class RequestMessage {
	public int fromRoom;
	protected IoBuffer buffer;
	protected boolean isValid = true;

	public RequestMessage() {
	}

	protected RequestMessage(IoBuffer buffer) {
		this.buffer = buffer;
		// ����쳣�����ﲶ������㣬���һ��isValid = false�ķ�null����
		// �����ߴ������
		try {
			this.fromRoom = this.buffer.getInt();
		} catch (Exception e) {
			this.isValid = false;
		}
	}

	public static RequestMessage create(int action, IoBuffer buffer) {
		RequestMessage msg = _create(action, buffer);
		if (msg != null && msg.isValid) {
			try {
				msg.parse();
			} catch (Exception e) {
				msg.isValid = false;
				ServerLogger.errorf("parse [%s] error :", e, msg.getClass().getName());
			}
		}

		return msg;
	}

	private static RequestMessage _create(int action, IoBuffer buffer) {
		switch (action) {
		case ReqMsgType.AsObj:
			return new AsObjMessage(buffer);
		case ReqMsgType.CreateRoom:
			return new CreateRoomMessage(buffer);
		case ReqMsgType.GtRmList:
			return new GtRmListMessage();
		case ReqMsgType.Hit:
			return new HitMessage();
		case ReqMsgType.JoinRoom:
			return new JoinRoomMessage(buffer);
		case ReqMsgType.LeaveRoom:
			return new LeaveRoomMessage(buffer);
		case ReqMsgType.Login:
			return new LoginMessage(buffer);		
		case ReqMsgType.PubMsg:
			return new PubMsgMessage(buffer);
		case ReqMsgType.SetRvars:
			return new SetRvarsMessage(buffer);
		case ReqMsgType.SetUvars:
			return new SetUvarsMessage(buffer);
		case ReqMsgType.XtReq:
			return new XtReqMessage(buffer);
		default:
			return null;
		}
	}

	public short getShort() {
		return buffer.getShort();
	}

	public int getInt() {
		return buffer.getInt();
	}

	public double getDouble() {
		return buffer.getDouble();
	}

	public float getFloat() {
		return buffer.getFloat();
	}

	public char getChar() {
		return (char) buffer.get();
	}

	public boolean getBool() {
		return buffer.get() == MsgConstants.BYTE_TRUE;
	}

	// Ĭ�Ͽͻ��˷��������ַ���ת����Ӧ���ֽ�����֮�󳤶Ȳ��ᳬ��2^16,��������Ĭ�Ͽͻ��˵��ַ����ֽ����鳤��Ϊ2
	public String getString() throws CharacterCodingException {
		return buffer.getPrefixedString(2, MsgConstants.StringMsgCharset.get().decoder);
	}

	public String getLongString() throws CharacterCodingException {
		return buffer.getPrefixedString(4, MsgConstants.StringMsgCharset.get().decoder);
	}

	public boolean validateMsg() {
		return isValid;
	}

	void parse() throws Exception {
	}
}