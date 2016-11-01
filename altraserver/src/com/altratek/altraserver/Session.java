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
	// 上行消息buffer容量，
	private static int IN_MSG_BUFFER_LEN = 8192;
	// 用于作为选择队列的求余分子，是SocketChannel.hashCode的改进，比后者均匀一点
	// 由于队列上的用户有离开，加上每个人的消息量也有差异，所以严格均匀有点难实现，只做到宏观均匀。
	// 算是用最小代价做到80%的效果
	private static Object sessionSeqLock = new Object();
	private static int sessionSeq = 0;

	ByteBuffer inMsgBuffer;
	public User user;
	// 该值每次session都不同，仅用于校验消息的序列号 - msgSeqNo
	private int sessionHash;
	// 供序列号生成用，不想每次都user != null, user.getUserId()
	private int userId = 0;
	public volatile boolean lost = false;
	final int sysDistributeKey;
	final int extDistributeKey;
	final int outMsgDistributeKey;
	// msgSeqNo的类型对应ChannelSelector的MSG_HEAD_LEN值。
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
		if (match) { // 如果结果不对，就断线了，不用next了
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
		// 0没任何意义，只是与-1, 1区分，返回非-1, 1，不做任何后续处理
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

	// 其实没必要dispose，怕内存泄露，因为session的生命周期是SelectionKey决定的，非我们控制。
	// dispose之后，write线程可能有NullPointerException，因为都离线了，不用计较。
	void dispose() {
		this.lost = true;
		this.inMsgBuffer = null;
		this.user = null;
		this.selectionKey = null;
		this.channel = null;
		this.outMsgs = null;
	}

	// 单独抽一个类是为了隔离同步，避免和别的业务发生同步冲突，所谓同步模块化
	private static class OutMsgs {
		// 为什么用ArrayDeque类型的队列?
		// 每个人的下行消息队列最大容量是固定的，通过MAX_CHANNEL_QUEUE配置，超过容量，丢弃消息,
		// 尽管ArrayDeque也支持动态扩容，但在这个场景，用不上，就当固定数组用了。
		// 用ArrayDeque是预分配内存的方式，相比动态的LinkedList，少了大量的java.util.LinkedList$Entry链表节点临时对象
		// (据jmap监控，这个临时对象通常是排名第一多的，一条群发消息，产生n个)，消除这个临时对象，减轻GC负担, so called GC nice。
		// ArrayDeque非线程安全，自己同步保证。
		private ArrayDeque<ByteBuffer> outMsgs = new ArrayDeque<ByteBuffer>(ConfigData.MAX_CHANNEL_QUEUE);
		// 一次socket write未写完的半截消息。
		private volatile ByteBuffer outPartiallyMsg = null;
		// 累计socketchannel.write没写完次数
		private volatile short partiallyWriteCounter = 0;

		// 返回添加之后的消息队列长度，用于之后判断是否要加入写队列
		// -1 表示已满
		// 1 表示要加入处理队列
		private synchronized int appendOutMsg(ByteBuffer msg) {
			int msgCount = outMsgs.size();
			if (msgCount >= ConfigData.MAX_CHANNEL_QUEUE) {
				return -1;
			}
			outMsgs.addLast(msg);
			// 加outPartiallyMsg数量是为了表达socketchannel不可写
			// 不可写时不需要把session加入写队列，因为拿出来还是不能真正处理
			return (msgCount + 1) + (this.outPartiallyMsg != null ? 1 : 0);
		}

		private synchronized ByteBuffer takeOutMsg() {
			// 先看有没有上次未写完的半截消息。
			if (this.outPartiallyMsg != null) {
				ByteBuffer msg = this.outPartiallyMsg;
				this.outPartiallyMsg = null;
				return msg;
			}

			// 如果没有半截消息，拿个完整的。
			return outMsgs.pollFirst();
		}

		// 暂存半截消息。
		private synchronized void setOutPartiallyMsg(ByteBuffer msg) {
			this.outPartiallyMsg = msg;
			this.partiallyWriteCounter++;
		}

		private synchronized int outMsgCount() {
			return outMsgs.size() + (this.outPartiallyMsg != null ? 1 : 0);
		}
	}
}
