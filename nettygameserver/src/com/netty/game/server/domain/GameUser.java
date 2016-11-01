package com.netty.game.server.domain;

import com.netty.game.server.thread.InMsgHandlerWorkPool;
import com.netty.game.server.thread.OutMsgHandlerWorkerPool;

import io.netty.channel.Channel;

public class GameUser {
	private String userName;
	private Channel channel;
	public final int distributeKey_InMsg;
	public final int distributeKey_OutMsg;
	
	private static Object distributeLock = new Object();
	private static int distributeKey = 0;
	
	public GameUser(String userName, Channel channel) {
		this.userName = userName;
		this.channel = channel;
		int distributeKey = genDistributeKey();
		this.distributeKey_InMsg = distributeKey % InMsgHandlerWorkPool.instance.workerCount;
		this.distributeKey_OutMsg = distributeKey % OutMsgHandlerWorkerPool.instance.getWorkerCount();
	}

	private static int genDistributeKey() {
		synchronized (distributeLock) {
			distributeKey++;
			if (distributeKey == Integer.MAX_VALUE) {
				distributeKey = 0;
			}
			return distributeKey;
		}
	}
	
	public String getUserName() {
		return userName;
	}

	public Channel getChannel() {
		return channel;
	}
}
