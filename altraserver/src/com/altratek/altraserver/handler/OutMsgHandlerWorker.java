package com.altratek.altraserver.handler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.altratek.altraserver.ServerWriter;
import com.altratek.altraserver.Session;
import com.altratek.altraserver.ThreadUncaughtExceptionHandler;
import com.altratek.altraserver.logger.ServerLogger;

public class OutMsgHandlerWorker extends Thread {
	private BlockingQueue<Session> channelQueue;
	private ServerWriter writer;
	private volatile boolean running = true;

	public OutMsgHandlerWorker(String name, ServerWriter writer) {
		super(name);
		// TODO:����queue size���Ӽ�����ݿ���100�϶����㹻�ġ�
		this.channelQueue = new ArrayBlockingQueue<Session>(100);
		this.writer = writer;

		this.setUncaughtExceptionHandler(ThreadUncaughtExceptionHandler.getHandler());
		this.setDaemon(true);
	}

	public void run() {		
		while (running) {
			try {
				Session session = channelQueue.take();
				writer.writeOutMsg(session);
			} catch (InterruptedException e) {
				if (this.running) {
					ServerLogger.errorf("%s unexpected InterruptedException : ", e, this.writer.getName());
				}
			} catch (Throwable t) {
				ServerLogger.errorf("%s error : ", t, this.writer.getName());
			}
		}
	}

	public final void acceptEventChannel(Session session) {
		try {
			boolean failed = !channelQueue.offer(session);
			// ���д����ȥ�������Ҹ�ʱ��������У���������Ϣ��ʱ���ж����flag
			session.enqueueOutMsgFailed.compareAndSet(!failed, failed);
			if (failed) {
				ServerLogger.error("writer queue full.");
			}
		} catch (Exception e) {
			ServerLogger.error("channel queue put error:", e);
		}
	}

	public void shutdown() {
		running = false;
		this.interrupt();
	}

	public int getChannelQueueSize() {
		return channelQueue.size();
	}
}