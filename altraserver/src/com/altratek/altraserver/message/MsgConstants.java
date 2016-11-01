package com.altratek.altraserver.message;

public class MsgConstants {
	public static final byte MSGTYPE_SYSTEM = 0;
	public static final byte MSGTYPE_EXTENSION = 1;
	// only for reponse message (����)
	public static final byte MSGTYPE_EXTENSION_B = 2;

	public final static byte BYTE_TRUE = (byte) 49;
	public final static byte BYTE_FALSE = (byte) 48;

	// ThreadLocal��Ϊ�˱���ÿ�ζ�newһ��encoder
	public static final ThreadLocal<MsgCharset> StringMsgCharset = new ThreadLocal<MsgCharset>() {
		protected MsgCharset initialValue() {
			return new MsgCharset();
		}
	};
}