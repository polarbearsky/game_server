package com.altratek.altraserver.handler;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.altratek.altraserver.PoolThreadFactory;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.event.SystemEventDispatcher;
import com.altratek.altraserver.logger.ServerLogger;

public class LostHandler {
	private ThreadPoolExecutor lostBottomHalfExecutorService;

	public void init() {
		lostBottomHalfExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(
				ConfigData.LOST_HANDLER_THREADS, new PoolThreadFactory("LostHandler"));
	}

	public int getThreadCount() {
		return lostBottomHalfExecutorService.getMaximumPoolSize();
	}

	public void shutdown() throws InterruptedException {
		lostBottomHalfExecutorService.shutdown();
		lostBottomHalfExecutorService.awaitTermination(15, TimeUnit.MINUTES);
	}

	public void startLostBottomHalf(User user, String ip, String userLostParam) {
		if (ConfigData.ENABLE_PROFILE) {
			// “_”开头是为了与正常扩展性能监控项分开
			ServerLogger.profNum("_lostExecutor", "QueueLen", this.lostBottomHalfExecutorService.getQueue().size());
		}

		this.lostBottomHalfExecutorService.execute(new LostBottomHalfWork(user, ip, userLostParam));
	}

	private static class LostBottomHalfWork implements Runnable {
		private User user;
		private String ip;
		private String userLostParam;

		public LostBottomHalfWork(User user, String ip, String userLostParam) {
			this.user = user;
			this.ip = ip;
			this.userLostParam = userLostParam;
		}

		public void run() {
			try {
				lostBottomHalf(user, ip, userLostParam);
			} catch (Throwable t) {
				ServerLogger.error("<<Lost Connection bottom half>>: Throwable - ", t);
			}
		}
	}

	// buttom half只做业务数据操作，socket相关的在top half做了。
	private static void lostBottomHalf(User user, String ip, String userLostParam) {
		String userName = user.getName();
		Zone zone = user.getZone();
		int roomsConnected[] = user.getRoomIdConnected();

		try {
			SystemEventDispatcher.instance.triggerUserLost(user, roomsConnected, userLostParam);
		} catch (Exception e) {
			ServerLogger.error("triggerUserLost error", e);
		}

		user.exitAllRooms();

		if (zone != null) {
			zone.removeUserName(user.getName_AsZoneUserMapKey(), user);
			zone.destroyVariables(user);
		}

		if (ServerLogger.infoEnabled) {
			ServerLogger.infof("User[%s] removed, reason[%s]", userName != null ? userName : ip, userLostParam);
		}
	}
}