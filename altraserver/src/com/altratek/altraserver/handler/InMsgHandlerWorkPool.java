package com.altratek.altraserver.handler;

import com.altratek.altraserver.lib.ServerEvent;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.util.ServerStat;

public final class InMsgHandlerWorkPool {
	private final String name;
	public final int workerCount;
	private InMsgHandlerWorker[] workers;

	private final int statIndex;

	public InMsgHandlerWorkPool(String name, int workerCount) {
		this.name = name;
		this.workerCount = workerCount;
		this.initWorkers();

		statIndex = ServerStat.instance.registerInMsgHandler(this);
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
	
	public final void acceptEvent(ServerEvent serverEvent, int distributeKey) {
		InMsgHandlerWorker worker = this.workers[distributeKey];
		worker.acceptEvent(serverEvent);
		ServerStat.instance.increaseInMsgs(this.statIndex, 1);
	}

	public String getName() {
		return this.name;
	}

	public int getEventQueueSize() {
		int total = 0;
		for (InMsgHandlerWorker w : this.workers) {
			total += w.getEventQueueSize();
		}
		return total;
	}
}
