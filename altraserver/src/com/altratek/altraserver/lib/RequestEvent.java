package com.altratek.altraserver.lib;

import java.nio.channels.SocketChannel;

import com.altratek.altraserver.buffer.IoBuffer;

public class RequestEvent {
	private IoBuffer buffer;
	private SocketChannel senderChannel;
	private short action;
	private byte type;

	public RequestEvent(IoBuffer buffer, SocketChannel senderChannel) {
		this.buffer = buffer;
		this.senderChannel = senderChannel;
		// 之前已经flip了
		this.type = this.buffer.get();
		this.action = this.buffer.getShort();
	}

	public SocketChannel getSenderChannel() {
		return senderChannel;
	}

	public short getAction() {
		return action;
	}

	public byte getType() {
		return type;
	}

	public IoBuffer getBuffer() {
		return buffer;
	}
}