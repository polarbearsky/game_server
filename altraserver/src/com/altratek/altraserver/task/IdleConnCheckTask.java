package com.altratek.altraserver.task;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.ZoneManager;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.event.SystemEventDispatcher;
import com.altratek.altraserver.logger.ServerLogger;

public class IdleConnCheckTask implements Runnable {
	private final AltraServer als;
	private final long maxUserIdleTime;
	private final long maxGameIdleTime;

	private final List<SocketChannel> readyToClean = new ArrayList<SocketChannel>();

	public IdleConnCheckTask() {
		this.maxUserIdleTime = ConfigData.MAX_USER_IDLETIME * 1000;
		this.maxGameIdleTime = ConfigData.MAX_GAME_IDLETIME * 1000;

		als = AltraServer.getInstance();
	}

	@Override
	public void run() {
		try {
			this.doRun();
		} catch (Throwable t) {
			ServerLogger.error("<<ConnClearTimerTask.run()>>: Throwable - ", t);
		}
	}

	private void doRun() {
		List<Zone> zones = ZoneManager.instance.getAllZones();

		for (Zone zone : zones) {
			if (zone == Zone.adminZone) {
				continue;
			}

			List<User> users = zone.getUserList();

			for (User user : users) {
				this.checkConnection(user);
			}
		}

		doClean();
	}

	private void checkConnection(User user) {
		long now = System.currentTimeMillis();

		long diff = now - user.getLastOperationTime();
		if (diff > maxGameIdleTime && diff <= maxUserIdleTime) {
			SystemEventDispatcher.instance.triggerExceedGameIdleTime(user);
		}

		if (diff > maxUserIdleTime) {
			readyToClean.add(user.getSocketChannel());
		}
	}

	private void doClean() {
		for (SocketChannel sc : readyToClean) {
			try {
				als.lostConn(sc, "clear task");
			} catch (Exception e) {
				ServerLogger.error("<<ConnClearTimerTask.run()>>: Exception while doing clean - ", e);
			}
		}

		readyToClean.clear();
	}
}