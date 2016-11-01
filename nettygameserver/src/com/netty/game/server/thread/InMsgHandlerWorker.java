package com.netty.game.server.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.netty.game.server.ServerLogger;
import com.netty.game.server.config.ServerConfigData;
import com.netty.game.server.exception.ThreadUncaughtExceptionHandler;
import com.netty.game.server.msg.ServerMsg;

public class InMsgHandlerWorker extends Thread {
	private static final int maxQueueSize = Math.min(ServerConfigData.MAX_INCOMING_QUEUE, 2000);
	private BlockingQueue<ServerMsg> msgQueue;
	private volatile boolean running = true;

	public InMsgHandlerWorker(String name) {
		super(name);
		this.msgQueue = new ArrayBlockingQueue<ServerMsg>(maxQueueSize);

		this.setUncaughtExceptionHandler(ThreadUncaughtExceptionHandler.getHandler());
		this.setDaemon(true);
	}

	public void run() {
		while (running) {
			try {
				ServerMsg se = msgQueue.take();
				se.handleMsg();
			} catch (InterruptedException e) {
				if (this.running) {
					ServerLogger.errorf("%s unexpected InterruptedException:", e, this.getName());
				}
			} catch (Throwable t) {
				ServerLogger.errorf("%s error : ", t, this.getName());
			}
		}
	}

	public final void acceptEvent(ServerMsg serverMsg) {
		boolean ok = msgQueue.offer(serverMsg);
		if (!ok) {
			ServerLogger.warn("in msg handle queue full");
		}
	}

	public int getMsgQueueSize() {
		return msgQueue.size();
	}

	public void shutdown() {
		running = false;
		this.interrupt();
	}
}
