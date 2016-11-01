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
		// 用recipients参数判断这条消息是否发送给多人，即变量toMultiple。
		// 为什么要有用toMultiple呢？
		// direct byte buffer的创建代价大，但发送(socket write)时因为少了一次copy，所以发送有性能优势
		// 显然，如果一个direct buffer创建后，可以重复使用，性能优势大于创建代价。
		// 发送给多人的消息toMultiple=true，就符合这个特点。尽管每个接受者都duplicate了一个buffer
		// 真正存放数据的部分还是一份。参见：duplicate方法。
		boolean toMultiple = (recipients.size() > 1);
		this.buffer = IoBuffer.allocate(getBufferDefaultSize(action), toMultiple);
		this.buffer.setAutoExpand(true);
		putInt(0); // place holder for message length

		this.buffer.put(type);
		if (type == MsgConstants.MSGTYPE_SYSTEM) {
			// 扩展下行消息不需要这两个字段
			putShort(action);
			putInt(fromRoom);
		}
	}

	// 系统消息构造函数
	protected ResponseMessage(int action, int fromRoom, List<SocketChannel> recipients) {
		this(MsgConstants.MSGTYPE_SYSTEM, action, fromRoom, recipients);
	}

	// 扩展消息构造函数
	protected ResponseMessage(byte type, List<SocketChannel> recipients) {
		this(type, -1, -1, recipients);
	}

	public List<SocketChannel> getRecipients() {
		return this.recipients;
	}

	private static int getBufferDefaultSize(int action) {
		// 该缺省值要根据统计结果来确定，不一定是一个值
		// 可以根据action不同而不同
		// 据统计，95%的下行消息小于100byte, 99%的小于300byte
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

	// 本方法会自动先put字符串的长度，默认服务端的字符串转成相应的字节数组长度为2位(2^16)
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

	// 消息的内容顺序为:变量个数，变量名长度，变量名，类型，值长度，值，变量名长度，变量名，类型，值长度，值...
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
				// 变量类型只有一个字符，所以这里只put第一个字符
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
				// 变量类型只有一个字符，所以这里只put第一个字符
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

	// 该方法只能调用一次呀，否则就写在getMsgBuffer里了。
	public void readyBuffer() {
		// 设置消息长度首字节
		this.buffer.flip(); // 以后不要flip了。
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