package com.netty.game.server.thread;

import com.netty.game.server.ServerLogger;
import com.netty.game.server.msg.ServerMsg;

public final class InMsgHandlerWorkPool {
	private final String name;
	public final int workerCount;
	private InMsgHandlerWorker[] workers;

	public static final InMsgHandlerWorkPool instance = new InMsgHandlerWorkPool("InMsgPool", 10);
	
	private InMsgHandlerWorkPool(String name, int workerCount) {
		this.name = name;
		this.workerCount = workerCount;
		this.initWorkers();
	}

	public void init(){
		
	}
	
	private void initWorkers() {
		workers = new InMsgHandlerWorker[workerCount];
		for (int i = 0; i < workerCount; i++) {
			workers[i] = new InMsgHandlerWorker(String.format("%s-%s", name, i + 1));
			workers[i].start();
		}
	}

	// 关了之后，队列中剩余的消息不处理了。
	public void shutdown() throws InterruptedException {
		for (InMsgHandlerWorker w : this.workers) {
			w.shutdown();
		}

		for (Thread t : this.workers) {
			t.join();
		}

		ServerLogger.info(this.name + " closed.");
	}

	public final void acceptEvent(ServerMsg serverMsg, int distributeKey) {
		InMsgHandlerWorker worker = this.workers[distributeKey];
		worker.acceptEvent(serverMsg);
	}

	public String getName() {
		return this.name;
	}

	public int getEventQueueSize() {
		int total = 0;
		for (InMsgHandlerWorker w : this.workers) {
			total += w.getMsgQueueSize();
		}
		return total;
	}
}
