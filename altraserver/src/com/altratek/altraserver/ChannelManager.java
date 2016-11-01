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
	// ���map��������չ��ĳЩҵ�������ڲ����á�
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
		// ����д�ڹ��츳ֵ�����ΪConfigData���ܻ�δ����
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
		// ���������Ӻ͵�¼֮���û��Ĳ���
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
		// ������һ������SocketChannel��Zone��userName�ļ��
		if (sc == null) {
			throw new LoginException(ConfigData.LOGIN_SOCKET_NULL);
		}
		if (!sc.isConnected()) {
			throw new LoginException(ConfigData.LOGIN_SOCKET_CLOSED);
		}
		UserMapEntry userEntry = getUserEntry(sc); // ��̫����Ϊnull�ˡ�
		User u = userEntry.user;
		if (u != null) {
			throw new LoginException(ConfigData.LOGIN_AREADY_IN);
		}
		if (!targetZone.validateUserName(userName)) {
			throw new LoginException(ConfigData.LOGIN_NAME_TAKEN);
		}
		// �ж��Ƿ񳬹�����Server������û���
		User newUser = null;
		if (this.checkMaxConnections()) {
			// �����µ�User����
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
			// ���������루ҵ������sessionid����ΪsessionKey��
			((Session) selectionKey.attachment()).setUser(newUser, password);
			this.addLoginUser(newUser);
			// ��¼Ŀ��Zone
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

	// ����Ҫ�Ͽ������Ӷ�����һ��list����select֮ǰͳһ�������ĶϿ�
	// �����������˶��߳����Ͽ���select�����Ĳ�������
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
				// ֮ǰ�Ѿ������lostConn
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

	// TODO:��Ҫtry...catch��
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

		// ����ǵ��߳��ˣ�û�б�Ҫclone
		for (Entry<SocketChannel, UserMapEntry> en : channel_User.entrySet()) {
			// for (SocketChannel sc : all) {
			// UserMapEntry ue = channel_User.get(sc);
			// if (ue == null) {
			// // ��������Ǵ�map�����ģ������ǿ��ܱ�����߳��Ƴ������߳��ţ�
			// continue;
			// }
			UserMapEntry ue = en.getValue();
			if (ue.user == null) { // δ��¼����
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