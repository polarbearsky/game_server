package com.altratek.altraserver.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.ChannelManager;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.handler.InMsgHandlerWorkPool;
import com.altratek.altraserver.logger.ServerLogger;

// Server����״��ͳ��
// ������������������룬ͳ����ش��뾡�����������
public class ServerStat implements Runnable {
	// �޷�����ͳ�����ݵ��ۼƼ����������������ǲ���������û�м�����
	// �����Ƕ�ʱ���ۼ�ֵ�����ж���AtomInteger��AtomicLong�Ḵ��Щ��
	static class Counters {
		private AtomicInteger outMsgs = new AtomicInteger(0);
		private AtomicInteger inBytes = new AtomicInteger(0);
		private AtomicInteger outBytes = new AtomicInteger(0);
		private AtomicInteger inDropMsgs = new AtomicInteger(0);
		private AtomicInteger outDropMsgs = new AtomicInteger(0);
		// �ִ����̳߳طֱ�ͳ��������Ϣ
		// ���ᳬ����ô���̳߳ذɣ�
		// Ŀǰ��1��2����Ϊ�Ժ�¶�����̳߳ع��ܣ��ඨ��һ�㣬��ʹ����������ʱregisterInMsgHandler�ᱨ��
		private final static int MAX_HANDLER = 100;
		private AtomicInteger[] inMsgHandlerMsgs = new AtomicInteger[MAX_HANDLER];

		Counters() {
			for (int i = 0; i < MAX_HANDLER; i++) {
				inMsgHandlerMsgs[i] = new AtomicInteger(0);
			}
		}
	}

	public static final ServerStat instance = new ServerStat();
	public static final int STAT_INTERVAL = 10;

	private volatile Counters counters = new Counters();
	private List<InMsgHandlerWorkPool> handlers = new ArrayList<InMsgHandlerWorkPool>();
	private volatile boolean printHeader = false;

	// �����߳�register
	public int registerInMsgHandler(InMsgHandlerWorkPool handler) {
		handlers.add(handler);
		if (handlers.size() > this.counters.inMsgHandlerMsgs.length) {
			throw new IllegalArgumentException("too many in message handler thread pool.");
		}

		// ��������increaseInMsgs����
		int msgHandlerCounterIndex = handlers.size() - 1;
		return msgHandlerCounterIndex;
	}

	public void increaseInMsgs(int counterIndex, int count) {
		// ����׼����������û�д��ۣ�����log string����������ENABLE_PROFILE�������ж�
		// ���ٶ����������������
		if (ConfigData.ENABLE_PROFILE) {
			this.counters.inMsgHandlerMsgs[counterIndex].addAndGet(count);
		}
	}

	public void increaseOutMsgs(int count) {
		if (ConfigData.ENABLE_PROFILE) {
			this.counters.outMsgs.addAndGet(count);
		}
	}

	public void increaseInBytes(int count) {
		if (ConfigData.ENABLE_PROFILE) {
			this.counters.inBytes.addAndGet(count);
		}
	}

	public void increaseOutBytes(int count) {
		if (ConfigData.ENABLE_PROFILE) {
			this.counters.outBytes.addAndGet(count);
		}
	}

	public void increaseInDroppedMsg(int count) {
		if (ConfigData.ENABLE_PROFILE) {
			this.counters.inDropMsgs.addAndGet(count);
		}
	}

	public void increaseOutDroppedMsg(int count) {
		if (ConfigData.ENABLE_PROFILE) {
			this.counters.outDropMsgs.addAndGet(count);
		}
	}

	private String getInMsgHandlerHeader() {
		List<String> headers = new ArrayList<String>();
		for (InMsgHandlerWorkPool handler : handlers) {
			String name = handler.getName();
			headers.add(name + "_queue");
			headers.add(name + "_threads");
			headers.add(name + "_msgs");
		}

		return AltraServerUtils.join(headers, "\t");
	}

	private String getInMsgHandlerItems(Counters snapshot) {
		List<String> items = new ArrayList<String>();
		for (int i = 0; i < this.handlers.size(); i++) {
			InMsgHandlerWorkPool handler = this.handlers.get(i);
			items.add(String.valueOf(handler.getEventQueueSize()));
			items.add(String.valueOf(handler.workerCount));
			items.add(String.valueOf(snapshot.inMsgHandlerMsgs[i]));
		}

		return AltraServerUtils.join(items, "\t");
	}

	// �м�û�ո񣬸�ʽ���Ѷ��͵�����
	final static String header_for_read = "  \t connections \t inBytes  \t outBytes  \t inDropMsgs \t outDropMsgs \t writer_queue  \t writer_threads  \t outMsgs \t %s";
	final static String patterns_for_read = "\t %s              \t %s        \t %s           \t %s            \t %s               \t %s                 \t %s                   \t %s        \t %s";
	final static String header = header_for_read.replace(" ", "");
	final static String patterns = patterns_for_read.replace(" ", "");;

	private String getHeader() {
		return String.format(header, getInMsgHandlerHeader());
	}

	private String getSnapshot() {
		Counters snapshot = this.counters;
		Counters newCounters = new Counters();
		this.counters = newCounters;

		int connections = ChannelManager.instance.getGlobalUserCount();
		int inBytes = snapshot.inBytes.get();
		int outBytes = snapshot.outBytes.get();
		int inDropMsgs = snapshot.inDropMsgs.get();
		int outDropMsgs = snapshot.outDropMsgs.get();
		int writer_queue = AltraServer.getInstance().getServerWriter().getChannelQueueSize();
		int writer_threads = AltraServer.getInstance().getServerWriter().getWorkerCount();
		int outMsgs = snapshot.outMsgs.get();
		String inMsgHandlerItems = this.getInMsgHandlerItems(snapshot);

		return String.format(patterns, connections, inBytes, outBytes, inDropMsgs, outDropMsgs, writer_queue,
				writer_threads, outMsgs, inMsgHandlerItems);
	}

	private void logHeader() {
		ServerLogger.serverStat(this.getHeader());
	}

	@Override
	public void run() {
		if (!ConfigData.ENABLE_PROFILE)
			return;

		if (!this.printHeader) {
			this.printHeader = true;
			this.logHeader();
		}

		try {
			ServerLogger.serverStat(this.getSnapshot());
		} catch (Exception e) {
			ServerLogger.error("server stat log error :", e);
		}
	}
}
