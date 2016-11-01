package com.altratek.altraserver.config;

import java.util.List;

public class ProcessConfigData {
	public String ip;
	public int port;
	// Integer, Boolean���͵ı�ʾ����Ϊnull��Ϊnull����Ĭ��ֵ
	public Integer sysHandlerThreads;
	public Integer extHandlerThreads;
	public Integer outQueueThreads;
	public Integer lostHandlerThread;
	public Boolean enableProfile;
	public Boolean enableMsgLengthDebug;
	public String logLevel;
	public List<String> zoneNames;
}
