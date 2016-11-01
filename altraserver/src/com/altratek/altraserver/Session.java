package com.altratek.altraserver;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.lib.validator.IMessageValidator;
import com.altratek.altraserver.logger.ServerLogger;

public class Session {
	// ������Ϣbuffer������
	private static int IN_MSG_BUFFER_LEN = 8192;
	// ������Ϊѡ����е�������ӣ���SocketChannel.hashCode�ĸĽ����Ⱥ��߾���һ��
	// ���ڶ����ϵ��û����뿪������ÿ���˵���Ϣ��Ҳ�в��죬�����ϸ�����е���ʵ�֣�ֻ������۾��ȡ�
	// ��������С��������80%��Ч��
	private static Object sessionSeqLock = new Object();
	private static int sessionSeq = 0;

	ByteBuffer inMsgBuffer;
	public User user;
	// ��ֵÿ��session����ͬ��������У����Ϣ�����к� - msgSeqNo
	private int sessionHash;
	// �����к������ã�����ÿ�ζ�user != null, user.getUserId()
	private int userId = 0;
	public volatile boolean lost = false;
	final int sysDistributeKey;
	final int extDistributeKey;
	final int outMsgDistributeKey;
	// msgSeqNo�����Ͷ�ӦChannelSelector��MSG_HEAD_LENֵ��
	private int curMsgSeqNo;
	private int nextMsgSeqNo;
	private IMessageValidator msgValidator;
	public SocketChannel channel;
	SelectionKey selectionKey;
	OutMsgs outMsgs = new OutMsgs();
	int droppedMsgCount = 0;
	public AtomicBoolean enqueueOutMsgFailed = new AtomicBoolean(false);

	Session(SocketChannel channel) {
		inMsgBuffer = ByteBuffer.allocateDirect(IN_MSG_BUFFER_LEN);
		nextMsgSeqNo = 0;
		msgValidator = AltraServer.getInstance().getMessageValidator();
		this.channel = channel;

		int seq = genSessionSeq();
		this.sysDistributeKey = seq % InMsgHandleFacade.systemHandlerThreadCount();
		this.extDistributeKey = seq % InMsgHandleFacade.extensionHandlerThreadCount();
		this.outMsgDistributeKey = seq % ConfigData.OUT_QUEUE_THREADS;
	}

	void setUser(User user, String sessionKey) {
		this.user = user;
		this.userId = user.getUserId();
		if (sessionKey != null) {
			sessionHash = sessionKey.hashCode();
		}
	}

	private static int genSessionSeq() {
		synchronized (sessionSeqLock) {
			sessionSeq++;
			if (sessionSeq == Integer.MAX_VALUE) {
				sessionSeq = 0;
			}
			return sessionSeq;
		}
	}
	
	int getMsgSeqNo() {
		return this.curMsgSeqNo;
	}

	boolean checkMsgSeqNo() {
		this.curMsgSeqNo = this.inMsgBuffer.getInt();
		final boolean match = (curMsgSeqNo == this.nextMsgSeqNo);
		if (match) { // ���������ԣ��Ͷ����ˣ�����next��
			this.nextMsgSeqNo = this.msgValidator.nextMsgSeqNo(this.curMsgSeqNo, this.userId, this.sessionHash);
		} else {
			this.logDisMatchMsgNum(curMsgSeqNo, this.nextMsgSeqNo);
		}
		return match;
	}

	private void logDisMatchMsgNum(int msgSeqNo, int expectedMsgSeqNo) {
		if (user == null) {
			String ip = AltraServer.getInstance().getIpBySocketChannel(this.channel);
			ServerLogger.errorf("msg seq no match : IP[%s], in/tar[%s/%s]", ip, msgSeqNo, expectedMsgSeqNo);
		} else {
			ServerLogger.errorf("msg seq no match : User[%s, %s], in/tar[%s/%s]", user.getUserId(), user.getName(),
					msgSeqNo, expectedMsgSeqNo);
		}
	}

	int appendOutMsg(ByteBuffer msg) {
		// 0û�κ����壬ֻ����-1, 1���֣����ط�-1, 1�������κκ�������
		return this.outMsgs != null ? this.outMsgs.appendOutMsg(msg) : 0;
	}

	ByteBuffer takeOutMsg() {
		return this.outMsgs != null ? this.outMsgs.takeOutMsg() : null;
	}

	void setOutPartiallyMsg(ByteBuffer msg) {
		if (this.outMsgs != null) {
			this.outMsgs.setOutPartiallyMsg(msg);
		}
	}

	int outMsgCount() {
		return this.outMsgs != null ? this.outMsgs.outMsgCount() : 0;
	}

	int partiallyWriteCounter() {
		return this.outMsgs != null ? this.outMsgs.partiallyWriteCounter : -1;
	}	

	// ��ʵû��Ҫdispose�����ڴ�й¶����Ϊsession������������SelectionKey�����ģ������ǿ��ơ�
	// dispose֮��write�߳̿�����NullPointerException����Ϊ�������ˣ����üƽϡ�
	void dispose() {
		this.lost = true;
		this.inMsgBuffer = null;
		this.user = null;
		this.selectionKey = null;
		this.channel = null;
		this.outMsgs = null;
	}

	// ������һ������Ϊ�˸���ͬ��������ͱ��ҵ����ͬ����ͻ����νͬ��ģ�黯
	private static class OutMsgs {
		// Ϊʲô��ArrayDeque���͵Ķ���?
		// ÿ���˵�������Ϣ������������ǹ̶��ģ�ͨ��MAX_CHANNEL_QUEUE���ã�����������������Ϣ,
		// ����ArrayDequeҲ֧�ֶ�̬���ݣ���������������ò��ϣ��͵��̶��������ˡ�
		// ��ArrayDeque��Ԥ�����ڴ�ķ�ʽ����ȶ�̬��LinkedList�����˴�����java.util.LinkedList$Entry����ڵ���ʱ����
		// (��jmap��أ������ʱ����ͨ����������һ��ģ�һ��Ⱥ����Ϣ������n��)�����������ʱ���󣬼���GC����, so called GC nice��
		// ArrayDeque���̰߳�ȫ���Լ�ͬ����֤��
		private ArrayDeque<ByteBuffer> outMsgs = new ArrayDeque<ByteBuffer>(ConfigData.MAX_CHANNEL_QUEUE);
		// һ��socket writeδд��İ����Ϣ��
		private volatile ByteBuffer outPartiallyMsg = null;
		// �ۼ�socketchannel.writeûд�����
		private volatile short partiallyWriteCounter = 0;

		// �������֮�����Ϣ���г��ȣ�����֮���ж��Ƿ�Ҫ����д����
		// -1 ��ʾ����
		// 1 ��ʾҪ���봦�����
		private synchronized int appendOutMsg(ByteBuffer msg) {
			int msgCount = outMsgs.size();
			if (msgCount >= ConfigData.MAX_CHANNEL_QUEUE) {
				return -1;
			}
			outMsgs.addLast(msg);
			// ��outPartiallyMsg������Ϊ�˱��socketchannel����д
			// ����дʱ����Ҫ��session����д���У���Ϊ�ó������ǲ�����������
			return (msgCount + 1) + (this.outPartiallyMsg != null ? 1 : 0);
		}

		private synchronized ByteBuffer takeOutMsg() {
			// �ȿ���û���ϴ�δд��İ����Ϣ��
			if (this.outPartiallyMsg != null) {
				ByteBuffer msg = this.outPartiallyMsg;
				this.outPartiallyMsg = null;
				return msg;
			}

			// ���û�а����Ϣ���ø������ġ�
			return outMsgs.pollFirst();
		}

		// �ݴ�����Ϣ��
		private synchronized void setOutPartiallyMsg(ByteBuffer msg) {
			this.outPartiallyMsg = msg;
			this.partiallyWriteCounter++;
		}

		private synchronized int outMsgCount() {
			return outMsgs.size() + (this.outPartiallyMsg != null ? 1 : 0);
		}
	}
}
