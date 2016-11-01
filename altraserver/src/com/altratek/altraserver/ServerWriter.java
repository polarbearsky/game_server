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

	// 关服不等消息写完，因为select已经关了，写机制已经不健全。
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

	// 有四种情况需要把用户session(channel)放入写队列，等待写出：
	// case 1. 有新的消息产生 && 没有半截消息存在 && session之前不在队列中。多线程
	// case 2. select发现write ready之后，这个时候，一定是有半截消息存在的。单线程（select线程）
	// case 3. 成功写完一个消息之后，还有待写出的消息。单线程（用户所属写线程）
	// case 4. 因写队列满了，放入写队列失败。这种情况极少发生。
	// dispatchMsgChannelToWorker方法的三次调用，分别就是以上三种情况
	// case 3存在的必要：
	// 用户消息队列数量umq，session在队列中的数量siq
	// 当用户有新消息要发送umq++, siq++
	// writer发送一个消息siq--, umq--
	// 好像很简单！
	// 如果发生partial write，可能同一消息多次发生，siq--多次，umq--一次
	// 即使有select，siq--多次，可能只产生一次case 2 select write ready
	// 这样会出现umq > 0，siq = 0，即用户有消息，但没排队，没机会发送了。
	void dispatchMsgChannelToWorker(Session session) {
		this.workers[session.outMsgDistributeKey].acceptEventChannel(session);
	}

	//
	// 将该event加入其接收者的消息队列中
	// 1.取出该消息所有接收者
	// 2.遍历所有接收者
	// 3.取出其消息队列，将该消息加入其中
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
			// 为什么接受者是多个的时候要duplicate buffer呢？
			// 如果用一个buffer，因为有多个接受者，这个buffer会多次用到，
			// 甚至可能是多线程同时用到，显然会有问题
			// duplicate，内容是公用的，capacity, limit, position, and mark values独立
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

		// 若该接收者丢失的消息次数超过规定次数，则将该用户设为可删除的用户
		if (session.droppedMsgCount > ConfigData.MAX_DROPPED_PACKETS) {
			als.lostConn(sc, "msg dropped");
			ServerLogger.errorf("Too many dropped msgs, disconnect ip [%s, %s, %s]", ip, session.droppedMsgCount,
					session.partiallyWriteCounter());
		}
	}

	/**
	 * 从用户channel的消息队列中获取第一条并发送给该用户
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
			// 没有待写消息
			return;
		}

		SocketChannel channel = session.channel;

		try {
			// 记录实际发送出去的消息总长度/单位长度（单位byte）
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
				// ignore this exception，这个是用户断线，dispose session对象所致，正常现象，别污染log。
			} else {
				ServerLogger.error("Generic exception during write: ", e);
			}
		}
	}

	// From <java NIO> 4.2
	// "Generally SelectionKey objects are thread-safe, but it's important to know that operations that modify the
	// interest set are synchronized by Selector objects. This could cause calls to the interestOps( )
	// method to block for an indeterminate amount of time."
	// 如果是这样，在写线程做这个合适吗？
	// 尝试把修改interestOps的操作放入队列，在select线程selectNow之前统一批量做interestOps,
	// 有人强烈推荐这么做(google Rox java nio tutorial，墙外)，试过之后，结果并没有明显不同，
	// 因Too many dropped msgs而掉线的情况并没有改善。
	private void interestWrite(SelectionKey sk) {
		sk.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}
}