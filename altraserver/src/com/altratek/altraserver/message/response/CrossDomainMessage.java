package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.message.ResponseMessage;

public class CrossDomainMessage extends ResponseMessage {
	// 因为crossDomain的内容是恒定的，没有必要每次都创建buffer。
	// crossDomain是个特殊消息，和其他消息相比，只有buffer这个共性，所以就不纳入构造函数继承体系
	// 为了唯一一个特殊消息，把基类构造函数弄复杂了，不值得。那个bool参数非常复杂！
	private static volatile IoBuffer crossDomainbuffer;

	public CrossDomainMessage(String crossDomainContent, SocketChannel recipient) {
		// 这里的线程不安全，没什么问题
		if (crossDomainbuffer == null) {
			crossDomainbuffer = IoBuffer.allocate(1024, true);
			crossDomainbuffer.setAutoExpand(true);
			crossDomainbuffer.setAutoShrink(true);
			putStringWithOutLength(crossDomainbuffer, new StringBuilder(crossDomainContent).append('\0').toString());
			crossDomainbuffer.flip();
		}
		this.buffer = crossDomainbuffer.asReadOnlyBuffer();
		this.recipients = asSocketChannelList(recipient);
	}

	@Override
	public IoBuffer getBuffer() {
		// cross domain message没有消息长度，因为是flash来解析
		// duplicate or asReadOnlyBuffer的buffer不能flip。
		// this.buffer.flip();
		return this.buffer;
	}

	@Override
	public void readyBuffer() {
	}
}
