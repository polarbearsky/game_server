package com.altratek.altraserver;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.exception.LoginException;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.util.AltraServerUtils;

public class ChannelManager {
	public final static ChannelManager instance = new ChannelManager();

	private long maxUnloginIdleTime;

	private ConcurrentHashMap<SocketChannel, UserMapEntry> channel_User = new ConcurrentHashMap<SocketChannel, UserMapEntry>();
	// 这个map仅用于扩展的某些业务，引擎内部不用。
	private ConcurrentHashMap<Integer, User> id_User = new ConcurrentHashMap<Integer, User>();

	private final CopyAndClearList<LostChannel> willLostChannels = new CopyAndClearList<LostChannel>();

	private static class LostChannel {
		public final SocketChannel channel;
		public final String reason;

		public LostChannel(SocketChannel channel, String reason) {
			this.channel = channel;
			this.reason = reason;
		}
	}

	void init() {
		// 不能写在构造赋值那里，因为ConfigData可能还未加载
		this.maxUnloginIdleTime = ConfigData.MAX_UNLOGIN_IDLETIME * 1000;
	}

	public List<SocketChannel> getClientChannelClone() {
		return new ArrayList<SocketChannel>(channel_User.keySet());
	}

	public User getUserByChannel(SocketChannel sc) {
		UserMapEntry ue = channel_User.get(sc);
		return ue != null ? ue.user : null;
	}

	void addChannel(SocketChannel sc) {
		channel_User.put(sc, new UserMapEntry());
	}

	private UserMapEntry getUserEntry(SocketChannel sc) {
		return channel_User.get(sc);
	}

	private void addLoginUser(User user) {
		id_User.put(user.getUserId(), user);
	}

	public User getUserById(int id) {
		return id_User.get(id);
	}

	private boolean checkMaxConnections() {
		return channel_User.size() < ConfigData.SERVER_MAX_CLIENTS;
	}

	public int getGlobalUserCount() {
		// 忽略了链接和登录之后用户的差异
		return getChannelCount();
	}

	public int getChannelCount() {
		return channel_User.size();
	}

	public User doLogin(String userName, String password, Zone targetZone, SocketChannel sc)
			throws LoginException {
		return doLogin(userName, password, targetZone, sc, -1);
	}

	// exceptedUserId : id for new user
	public User doLogin(String userName, String password, Zone targetZone, SocketChannel sc,
			int exceptedUserId) throws LoginException {
		// 以下是一连串的SocketChannel、Zone、userName的检查
		if (sc == null) {
			throw new LoginException(ConfigData.LOGIN_SOCKET_NULL);
		}
		if (!sc.isConnected()) {
			throw new LoginException(ConfigData.LOGIN_SOCKET_CLOSED);
		}
		UserMapEntry userEntry = getUserEntry(sc); // 不太可能为null了。
		User u = userEntry.user;
		if (u != null) {
			throw new LoginException(ConfigData.LOGIN_AREADY_IN);
		}
		if (!targetZone.validateUserName(userName)) {
			throw new LoginException(ConfigData.LOGIN_NAME_TAKEN);
		}
		// 判断是否超过整个Server的最大用户数
		User newUser = null;
		if (this.checkMaxConnections()) {
			// 生成新的User对象
			if (exceptedUserId == -1) {
				newUser = AltraServer.getInstance().getUserBuilder().build(sc, userName, targetZone);
			} else {
				if (this.getUserById(exceptedUserId) != null) {
					throw new LoginException(ConfigData.LOGIN_AREADY_IN);
				}
				newUser = AltraServer.getInstance().getUserBuilder().build(sc, userName, targetZone,
						exceptedUserId);
			}
			userEntry.user = newUser;
			// channel_User.putIfAbsent(sc, userEntry);
			SelectionKey selectionKey = AltraServer.getInstance().getChannelKey(sc);
			// 现在用密码（业务上是sessionid）作为sessionKey。
			((Session) selectionKey.attachment()).setUser(newUser, password);
			this.addLoginUser(newUser);
			// 登录目标Zone
			targetZone.addUserName(newUser);
			if (ServerLogger.infoEnabled) {
				ServerLogger.infof("User[%s] logged into Zone[%s]", userName, targetZone.getName());
			}
		} else {
			throw new LoginException(ConfigData.LOGIN_CLINET_COUNT_FULL);
		}
		return newUser;
	}

	public void lostConn(int id, String reason) {
		User user = getUserById(id);
		if (user != null) {
			this.lostConn(user.getSocketChannel(), reason);
		}
	}

	// 所有要断开的链接都放入一个list，在select之前统一做真正的断开
	// 这样做避免了多线程做断开和select操作的并发问题
	public void lostConn(SocketChannel sc, String reason) {
		this.willLostChannels.add(new LostChannel(sc, reason));
	}

	void lostWillLostConns() {
		List<LostChannel> lostChannels = this.willLostChannels.copy();
		if (lostChannels == null) {
			return;
		}

		for (LostChannel lc : lostChannels) {
			this.LostOneConn(lc.channel, lc.reason);
		}
	}

	private void LostOneConn(SocketChannel sc, String reason) {
		try {
			UserMapEntry ue = channel_User.remove(sc);
			if (ue == null) {
				// 之前已经发起过lostConn
				return;
			}

			User user = ue.user;
			String ip = user != null ? user.getIP() : AltraServerUtils.getIpBySocketChannel(sc);

			closeSocketChannel(sc);

			IpFloodChecker.instance.decreaseIpCount(ip);

			if (user == null) {
				ServerLogger.infof("unlogin ip[%s] removed, reason[%s]", ip, reason);
				return;
			}

			user.lost = true;
			id_User.remove(user.getUserId());

			AltraServer.getInstance().getLostHandler().startLostBottomHalf(user, ip, reason);

		} catch (Exception e) {
			ServerLogger.error("lost one connection error : ", e);
		}
	}

	// TODO:需要try...catch吗？
	void closeSocketChannel(SocketChannel sc) throws Exception {
		SelectionKey sk = AltraServer.getInstance().getChannelKey(sc);
		if (sk != null) {
			Object att = sk.attachment();
			if (att != null) {
				((Session) att).dispose();
			}
		}
		sc.close();
	}

	void checkUnLoginChannels() {
		// List<SocketChannel> all = getClientChannelClone();

		// 如果是单线程了，没有必要clone
		for (Entry<SocketChannel, UserMapEntry> en : channel_User.entrySet()) {
			// for (SocketChannel sc : all) {
			// UserMapEntry ue = channel_User.get(sc);
			// if (ue == null) {
			// // 尽管这个是从map里来的，但还是可能被别的线程移除，多线程呐！
			// continue;
			// }
			UserMapEntry ue = en.getValue();
			if (ue.user == null) { // 未登录链接
				long now = System.currentTimeMillis();
				if (now - ue.connectTime > maxUnloginIdleTime) {
					this.lostConn(en.getKey(), "unlogin");
				}
			}
		}
	}

	void closeAllChannels() {
		List<SocketChannel> scList = new ArrayList<SocketChannel>(channel_User.keySet());
		for (SocketChannel sc : scList) {
			this.lostConn(sc, "shutdown");
		}
		this.lostWillLostConns();
	}
}