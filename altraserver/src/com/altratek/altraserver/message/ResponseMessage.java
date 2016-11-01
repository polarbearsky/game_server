package com.altratek.altraserver.message;

import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.RoomVariable;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.UserVariable;
import com.altratek.altraserver.logger.ServerLogger;

public class ResponseMessage {
	public int fromRoom;
	protected IoBuffer buffer;
	protected List<SocketChannel> recipients;

	public ResponseMessage() {
	}

	private ResponseMessage(byte type, int action, int fromRoom, List<SocketChannel> recipients) {
		this.recipients = recipients;
		// ��recipients�����ж�������Ϣ�Ƿ��͸����ˣ�������toMultiple��
		// ΪʲôҪ����toMultiple�أ�
		// direct byte buffer�Ĵ������۴󣬵�����(socket write)ʱ��Ϊ����һ��copy�����Է�������������
		// ��Ȼ�����һ��direct buffer�����󣬿����ظ�ʹ�ã��������ƴ��ڴ������ۡ�
		// ���͸����˵���ϢtoMultiple=true���ͷ�������ص㡣����ÿ�������߶�duplicate��һ��buffer
		// ����������ݵĲ��ֻ���һ�ݡ��μ���duplicate������
		boolean toMultiple = (recipients.size() > 1);
		this.buffer = IoBuffer.allocate(getBufferDefaultSize(action), toMultiple);
		this.buffer.setAutoExpand(true);
		putInt(0); // place holder for message length

		this.buffer.put(type);
		if (type == MsgConstants.MSGTYPE_SYSTEM) {
			// ��չ������Ϣ����Ҫ�������ֶ�
			putShort(action);
			putInt(fromRoom);
		}
	}

	// ϵͳ��Ϣ���캯��
	protected ResponseMessage(int action, int fromRoom, List<SocketChannel> recipients) {
		this(MsgConstants.MSGTYPE_SYSTEM, action, fromRoom, recipients);
	}

	// ��չ��Ϣ���캯��
	protected ResponseMessage(byte type, List<SocketChannel> recipients) {
		this(type, -1, -1, recipients);
	}

	public List<SocketChannel> getRecipients() {
		return this.recipients;
	}

	private static int getBufferDefaultSize(int action) {
		// ��ȱʡֵҪ����ͳ�ƽ����ȷ������һ����һ��ֵ
		// ���Ը���action��ͬ����ͬ
		// ��ͳ�ƣ�95%��������ϢС��100byte, 99%��С��300byte
		return 128;
	}

	protected void putShort(short value) {
		buffer.putShort(value);
	}

	protected void putShort(int value) {
		buffer.putShort((short) value);
	}

	protected void putInt(int value) {
		buffer.putInt(value);
	}

	protected void putDouble(double value) {
		buffer.putDouble(value);
	}

	protected void putFloat(float value) {
		buffer.putFloat(value);
	}

	protected void putChar(char value) {
		buffer.put((byte) value);
	}

	protected void putBool(boolean value) {
		buffer.put(value ? MsgConstants.BYTE_TRUE : MsgConstants.BYTE_FALSE);
	}

	// ���������Զ���put�ַ����ĳ��ȣ�Ĭ�Ϸ���˵��ַ���ת����Ӧ���ֽ����鳤��Ϊ2λ(2^16)
	protected void putString(String value) {
		try {
			buffer.putPrefixedString(value, 2, MsgConstants.StringMsgCharset.get().encoder);
		} catch (CharacterCodingException cce) {
			ServerLogger.error(cce.getMessage(), cce);
			putShort(0);
		}
	}

	protected void putStringWithOutLength(String value) {
		putStringWithOutLength(this.buffer, value);
	}

	protected static void putStringWithOutLength(IoBuffer buffer, String value) {
		try {
			buffer.putString(value, MsgConstants.StringMsgCharset.get().encoder);
		} catch (CharacterCodingException cce) {
			ServerLogger.error(cce.getMessage(), cce);
		}
	}

	protected void putLongString(String value) {
		try {
			buffer.putPrefixedString(value, 4, MsgConstants.StringMsgCharset.get().encoder);
		} catch (CharacterCodingException cce) {
			ServerLogger.error(cce.getMessage(), cce);
			putShort(0);
		}
	}

	// ��Ϣ������˳��Ϊ:�������������������ȣ������������ͣ�ֵ���ȣ�ֵ�����������ȣ������������ͣ�ֵ���ȣ�ֵ...
	protected void putUserVariables(Map<String, UserVariable> userVariables) {
		if (userVariables == null) {
			putShort(0);
		} else {
			putShort(userVariables.size());
			for (Iterator<Entry<String, UserVariable>> ituvn = userVariables.entrySet().iterator(); ituvn.hasNext();) {
				Entry<String, UserVariable> entry = ituvn.next();
				String vname = entry.getKey();
				putString(vname);
				UserVariable uv = entry.getValue();
				// ��������ֻ��һ���ַ�����������ֻput��һ���ַ�
				putChar(uv.getType().charAt(0));
				putString(uv.getValue());
			}
		}
	}

	protected void putRoomVariables(HashMap<String, RoomVariable> roomVariables) {
		if (roomVariables == null) {
			putShort(0);
		} else {
			putShort(roomVariables.size());
			for (Iterator<Entry<String, RoomVariable>> itrvn = roomVariables.entrySet().iterator(); itrvn.hasNext();) {
				Entry<String, RoomVariable> entry = itrvn.next();
				String vname = entry.getKey();
				putString(vname);
				RoomVariable rv = entry.getValue();
				// ��������ֻ��һ���ַ�����������ֻput��һ���ַ�
				putChar(rv.getType().charAt(0));
				putString(rv.getValue());
			}
		}
	}

	protected void putRoomUserListFromRoom(Room room) {
		User[] users = room.getAllUsersArray();		
		putShort(users.length);
		for (User user : users) {			
			putInt(user.getUserId());
			putString(user.getName());
			putUserVariables(user.getCloneOfUserVariables());			
		}	
	}

	public IoBuffer getBuffer() {
		return this.buffer;
	}

	// �÷���ֻ�ܵ���һ��ѽ�������д��getMsgBuffer���ˡ�
	public void readyBuffer() {
		// ������Ϣ�������ֽ�
		this.buffer.flip(); // �Ժ�Ҫflip�ˡ�
		this.buffer.putInt(0, this.buffer.remaining() - 4);
	}
	
	protected static List<SocketChannel> asSocketChannelList(SocketChannel socketChannel) {
		List<SocketChannel> list = new ArrayList<SocketChannel>(1);
		list.add(socketChannel);
		return list;
	}
	
	protected static List<SocketChannel> asSocketChannelList(User user) {
		return asSocketChannelList(user.getSocketChannel());
	}
}