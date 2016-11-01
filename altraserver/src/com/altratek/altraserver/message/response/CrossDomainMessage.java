package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.message.ResponseMessage;

public class CrossDomainMessage extends ResponseMessage {
	// ��ΪcrossDomain�������Ǻ㶨�ģ�û�б�Ҫÿ�ζ�����buffer��
	// crossDomain�Ǹ�������Ϣ����������Ϣ��ȣ�ֻ��buffer������ԣ����ԾͲ����빹�캯���̳���ϵ
	// Ϊ��Ψһһ��������Ϣ���ѻ��๹�캯��Ū�����ˣ���ֵ�á��Ǹ�bool�����ǳ����ӣ�
	private static volatile IoBuffer crossDomainbuffer;

	public CrossDomainMessage(String crossDomainContent, SocketChannel recipient) {
		// ������̲߳���ȫ��ûʲô����
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
		// cross domain messageû����Ϣ���ȣ���Ϊ��flash������
		// duplicate or asReadOnlyBuffer��buffer����flip��
		// this.buffer.flip();
		return this.buffer;
	}

	@Override
	public void readyBuffer() {
	}
}
