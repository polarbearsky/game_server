package com.altratek.altraserver.lib;

import com.altratek.altraserver.domain.User;

// ����ϵͳ����չ���г��ֵ��¼������������ڴӿͻ��˷�����¼�RequetEvent��
// ��չ����׼��������
public class ServerEvent {
	private final Object eventData;
	// ��ServerEvent�����User��Ϊ�˵��û�����ʱ���Ŷӵ�������Ȼ�ܱ�����
	// �������User����SocketChannel��ȥ��User������û����ߣ����ܲ鲻����
	// �鲻��User�������޷���������ġ�
	// userֻ���ڵ�¼����ʱ��null
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
