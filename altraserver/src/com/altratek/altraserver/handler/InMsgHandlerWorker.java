package com.altratek.altraserver.handler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.altratek.altraserver.ThreadUncaughtExceptionHandler;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.lib.ServerEvent;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.util.ServerStat;

public class InMsgHandlerWorker extends Thread {
	private static final int maxQueueSize = Math.min(ConfigData.MAX_INCOMING_QUEUE, 2000);
	private BlockingQueue<ServerEvent> eventQueue;
	private volatile boolean running = true;

	public InMsgHandlerWorker(String name) {
		super(name);
		this.eventQueue = new ArrayBlockingQueue<ServerEvent>(maxQueueSize);

		this.setUncaughtExceptionHandler(ThreadUncaughtExceptionHandler.getHandler());
		this.setDaemon(true);
	}

	public void run() {
		while (running) {
			try {
				ServerEvent se = eventQueue.take();
				se.handleEvent();
			} catch (InterruptedException e) {
				if (this.running) {
					ServerLogger.errorf("%s unexpected InterruptedException:", e, this.getName());
				}
			} catch (Throwable t) {
				ServerLogger.errorf("%s error : ", t, this.getName());
			}
		}
	}

	public final void acceptEvent(ServerEvent serverEvent) {
		boolean ok = eventQueue.offer(serverEvent);
		if (!ok) {
			ServerLogger.error("in msg handle queue full");
			ServerStat.instance.increaseInDroppedMsg(1);
		}
	}

	public int getEventQueueSize() {
		return eventQueue.size();
	}

	public void shutdown() {
		running = false;
		this.interrupt();
	}
}
