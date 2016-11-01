package com.altratek.altraserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.handler.OutMsgHandlerWorker;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.ResponseMessage;
import com.altratek.altraserver.util.AltraServerUtils;
import com.altratek.altraserver.util.ServerStat;

public class ServerWriter {
	private final AltraServer als;
	private final String name = "Writer";
	private OutMsgHandlerWorker workers[];

	public ServerWriter(final int threadCount) {
		als = AltraServer.getInstance();
		initWorkers(threadCount);
	}

	private void initWorkers(final int workerCount) {
		workers = new OutMsgHandlerWorker[workerCount];
		for (int i = 0; i < workerCount; i++) {
			workers[i] = new OutMsgHandlerWorker(String.format("%s-%s", this.name, i + 1), this);
			workers[i].start();
		}
	}

	// �ط�������Ϣд�꣬��Ϊselect�Ѿ����ˣ�д�����Ѿ�����ȫ��
	public void shutdown() throws InterruptedException {
		for (OutMsgHandlerWorker w : this.workers) {
			w.shutdown();
		}

		for (Thread t : this.workers) {
			t.join();
		}

		ServerLogger.info("writer closed.");
	}

	public String getName() {
		return this.name;
	}

	public int getWorkerCount() {
		return this.workers.length;
	}

	public int getChannelQueueSize() {
		int total = 0;
		for (OutMsgHandlerWorker w : this.workers) {
			total += w.getChannelQueueSize();
		}
		return total;
	}

	// �����������Ҫ���û�session(channel)����д���У��ȴ�д����
	// case 1. ���µ���Ϣ���� && û�а����Ϣ���� && session֮ǰ���ڶ����С����߳�
	// case 2. select����write ready֮�����ʱ��һ�����а����Ϣ���ڵġ����̣߳�select�̣߳�
	// case 3. �ɹ�д��һ����Ϣ֮�󣬻��д�д������Ϣ�����̣߳��û�����д�̣߳�
	// case 4. ��д�������ˣ�����д����ʧ�ܡ�����������ٷ�����
	// dispatchMsgChannelToWorker���������ε��ã��ֱ���������������
	// case 3���ڵı�Ҫ��
	// �û���Ϣ��������umq��session�ڶ����е�����siq
	// ���û�������ϢҪ����umq++, siq++
	// writer����һ����Ϣsiq--, umq--
	// ����ܼ򵥣�
	// �������partial write������ͬһ��Ϣ��η�����siq--��Σ�umq--һ��
	// ��ʹ��select��siq--��Σ�����ֻ����һ��case 2 select write ready
	// ���������umq > 0��siq = 0�����û�����Ϣ����û�Ŷӣ�û���ᷢ���ˡ�
	void dispatchMsgChannelToWorker(Session session) {
		this.workers[session.outMsgDistributeKey].acceptEventChannel(session);
	}

	//
	// ����event����������ߵ���Ϣ������
	// 1.ȡ������Ϣ���н�����
	// 2.�������н�����
	// 3.ȡ������Ϣ���У�������Ϣ��������
	//
	public void sendOutMsg(ResponseMessage rspMsg) {
		if (als.shutdowning) {
			return;
		}

		List<SocketChannel> recipients = rspMsg.getRecipients();

		boolean toMultiple = recipients.size() > 1;
		rspMsg.readyBuffer();
		ByteBuffer orginalBuffer = rspMsg.getBuffer().buf();

		for (SocketChannel recipient : recipients) {
			// Ϊʲô�������Ƕ����ʱ��Ҫduplicate buffer�أ�
			// �����һ��buffer����Ϊ�ж�������ߣ����buffer�����õ���
			// ���������Ƕ��߳�ͬʱ�õ�����Ȼ��������
			// duplicate�������ǹ��õģ�capacity, limit, position, and mark values����
			ByteBuffer msgBuffer = toMultiple ? orginalBuffer.asReadOnlyBuffer() : orginalBuffer;

			this.sendMsgToRecipient(recipient, msgBuffer);
		}
	}

	private void sendMsgToRecipient(SocketChannel recipient, ByteBuffer msgBuffer) {

		ServerStat.instance.increaseOutMsgs(1);

		SelectionKey sk = als.getChannelKey(recipient);
		if (sk == null || !sk.isValid()) {
			return;
		}

		Session session = (Session) sk.attachment();
		if (session.lost) {
			return;
		}

		int msgCount = session.appendOutMsg(msgBuffer);

		if (msgCount == 1 || session.enqueueOutMsgFailed.get()) {
			// case 1 || case 4
			dispatchMsgChannelToWorker(session);
		} else if (msgCount == -1) {
			this.handleFailedWrite(session, recipient);
		}
	}

	private void handleFailedWrite(Session session, SocketChannel sc) {

		ServerStat.instance.increaseOutDroppedMsg(1);

		String ip = AltraServerUtils.getIpBySocketChannel(sc);

		if (ServerLogger.debugEnabled) {
			ServerLogger.debugf("Dropped outgoing message - ip[%s]", ip);
		}

		if (ConfigData.MAX_DROPPED_PACKETS < 1) {
			return;
		}

		session.droppedMsgCount++;

		// ���ý����߶�ʧ����Ϣ���������涨�������򽫸��û���Ϊ��ɾ�����û�
		if (session.droppedMsgCount > ConfigData.MAX_DROPPED_PACKETS) {
			als.lostConn(sc, "msg dropped");
			ServerLogger.errorf("Too many dropped msgs, disconnect ip [%s, %s, %s]", ip, session.droppedMsgCount,
					session.partiallyWriteCounter());
		}
	}

	/**
	 * ���û�channel����Ϣ�����л�ȡ��һ�������͸����û�
	 */
	public void writeOutMsg(Session session) {
		SelectionKey sk = session.selectionKey;
		if (sk == null || !sk.isValid()) {
			return;
		}

		if (session.lost) {
			return;
		}

		ByteBuffer writeBuffer = session.takeOutMsg();
		if (writeBuffer == null) {
			// û�д�д��Ϣ
			return;
		}

		SocketChannel channel = session.channel;

		try {
			// ��¼ʵ�ʷ��ͳ�ȥ����Ϣ�ܳ���/��λ���ȣ���λbyte��
			int outMsgLength = channel.write(writeBuffer);
			ServerStat.instance.increaseOutBytes(outMsgLength);
			if (ConfigData.ENABLE_MSG_LENGTH_DEBUG) {
				ServerLogger.msgLengthDebug(new StringBuilder("out/").append(outMsgLength).toString());
			}

			if (writeBuffer.hasRemaining()) {
				session.setOutPartiallyMsg(writeBuffer);
				interestWrite(sk);
			} else {
				if (session.outMsgCount() > 0) {
					// case 3
					this.dispatchMsgChannelToWorker(session);
				}
			}
		} catch (IOException ioe) {
			if (ServerLogger.debugEnabled) {
				ServerLogger.error("Failed writing to channel: ", ioe);
			}
			if (ConfigData.DEAD_CHANNELS_POLICY > 0) {
				this.als.lostConn(channel, "failed write");
			}
		} catch (Exception e) {
			if (e instanceof NullPointerException && session.lost) {
				// ignore this exception��������û����ߣ�dispose session�������£��������󣬱���Ⱦlog��
			} else {
				ServerLogger.error("Generic exception during write: ", e);
			}
		}
	}

	// From <java NIO> 4.2
	// "Generally SelectionKey objects are thread-safe, but it's important to know that operations that modify the
	// interest set are synchronized by Selector objects. This could cause calls to the interestOps( )
	// method to block for an indeterminate amount of time."
	// �������������д�߳������������
	// ���԰��޸�interestOps�Ĳ���������У���select�߳�selectNow֮ǰͳһ������interestOps,
	// ����ǿ���Ƽ���ô��(google Rox java nio tutorial��ǽ��)���Թ�֮�󣬽����û�����Բ�ͬ��
	// ��Too many dropped msgs�����ߵ������û�и��ơ�
	private void interestWrite(SelectionKey sk) {
		sk.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}
}