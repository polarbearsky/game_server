package com.netty.game.server.thread;

import com.netty.game.server.ServerLogger;
import com.netty.game.server.msg.ServerOutMsg;

public class OutMsgHandlerWorkerPool {
	private String name;
	private OutMsgHandlerWorker workers[];

	public static final OutMsgHandlerWorkerPool instance = new OutMsgHandlerWorkerPool("OutMsgPool", 10);

	private OutMsgHandlerWorkerPool(final String name, final int threadCount) {
		this.name = name;
		initWorkers(threadCount);
	}

	public void init(){
		
	}
	
	private void initWorkers(final int workerCount) {
		workers = new OutMsgHandlerWorker[workerCount];
		for (int i = 0; i < workerCount; i++) {
			workers[i] = new OutMsgHandlerWorker(String.format("%s-%s", this.name, i + 1));
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

	public void dispatchMsgChannelToWorker(ServerOutMsg outMsg) {
		this.workers[outMsg.user.distributeKey_OutMsg].acceptOutMsg(outMsg);
	}
}