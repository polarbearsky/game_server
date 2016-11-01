package com.netty.game.server.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.netty.game.server.ServerLogger;
import com.netty.game.server.exception.ThreadUncaughtExceptionHandler;
import com.netty.game.server.msg.ServerOutMsg;

public class OutMsgHandlerWorker extends Thread {
	private BlockingQueue<ServerOutMsg> outMsgQueue;
	private volatile boolean running = true;
	
	public OutMsgHandlerWorker(String name) {
		super(name);
		this.outMsgQueue = new ArrayBlockingQueue<ServerOutMsg>(100);

		this.setUncaughtExceptionHandler(ThreadUncaughtExceptionHandler.getHandler());
		this.setDaemon(true);
	}

	public void run() {		
		while (running) {
			try {
				ServerOutMsg outMsg = outMsgQueue.take();
				outMsg.handleOutMsg();
			} catch (InterruptedException e) {
				if (this.running) {
					ServerLogger.errorf("%s unexpected InterruptedException : ", e, this.getName());
				}
			} catch (Throwable t) {
				ServerLogger.errorf("%s error : ", t, this.getName());
			}
		}
	}

	public final void acceptOutMsg(ServerOutMsg outMsg) {
		try {
			outMsgQueue.offer(outMsg);
			/*boolean failed = !outMsgQueue.offer(outMsg);
			//先不考虑失败情况
			session.enqueueOutMsgFailed.compareAndSet(!failed, failed);
			if (failed) {
				ServerLogger.error("writer queue full.");
			}*/
		} catch (Exception e) {
			ServerLogger.warn("channel queue put error:", e);
		}
	}

	public void shutdown() {
		running = false;
		this.interrupt();
	}

	public int getChannelQueueSize() {
		return outMsgQueue.size();
	}
}