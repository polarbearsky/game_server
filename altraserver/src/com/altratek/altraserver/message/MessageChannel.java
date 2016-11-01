package com.altratek.altraserver.message;

import java.nio.channels.SocketChannel;

// 抽象各种可作为消息接受者的对象
// 避免大量转换
public interface MessageChannel {
	SocketChannel getSocketChannel();
}
