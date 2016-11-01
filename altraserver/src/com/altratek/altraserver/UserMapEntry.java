package com.altratek.altraserver;

import com.altratek.altraserver.domain.User;

// 用于SocketChannel - User 的map做value。
public class UserMapEntry {
	public final long connectTime = System.currentTimeMillis();
	// user = null表示未登录，登录之后，会被赋值
	public User user = null;
}
