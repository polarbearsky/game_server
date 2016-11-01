package com.altratek.altraserver.lib;

import com.altratek.altraserver.domain.User;

// 会在系统和扩展队列出现的事件，不仅仅限于从客户端发起的事件RequetEvent。
// 扩展队列准备开发。
public class ServerEvent {
	private final Object eventData;
	// 在ServerEvent里加入User是为了当用户断线时，排队的请求仍然能被处理
	// 如果不加User，用SocketChannel再去查User，如果用户断线，可能查不到，
	// 查不到User请求是无法继续处理的。
	// user只有在登录请求时是null
	private final User user;
	private final IServerEventHandler handler;

	public ServerEvent(User user, Object eventData, IServerEventHandler handler) {
		this.user = user;
		this.eventData = eventData;
		this.handler = handler;
	}

	public final void handleEvent() {
		this.handler.handleEvent(this.user, this.eventData);
	}
}
