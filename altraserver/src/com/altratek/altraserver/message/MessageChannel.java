package com.altratek.altraserver.message;

import java.nio.channels.SocketChannel;

// ������ֿ���Ϊ��Ϣ�����ߵĶ���
// �������ת��
public interface MessageChannel {
	SocketChannel getSocketChannel();
}
