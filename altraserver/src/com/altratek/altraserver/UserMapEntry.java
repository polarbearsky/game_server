package com.altratek.altraserver;

import com.altratek.altraserver.domain.User;

// ����SocketChannel - User ��map��value��
public class UserMapEntry {
	public final long connectTime = System.currentTimeMillis();
	// user = null��ʾδ��¼����¼֮�󣬻ᱻ��ֵ
	public User user = null;
}
