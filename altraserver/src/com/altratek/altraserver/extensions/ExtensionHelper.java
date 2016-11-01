package com.altratek.altraserver.extensions;

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.ChannelManager;
import com.altratek.altraserver.IpFloodChecker;
import com.altratek.altraserver.ZoneManager;
import com.altratek.altraserver.config.reloader.IConfigReloader;
import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.RoomVariable;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.UserVariable;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.exception.CreateRoomException;
import com.altratek.altraserver.exception.JoinRoomException;
import com.altratek.altraserver.exception.LoginException;
import com.altratek.altraserver.handler.SystemHandler;
import com.altratek.altraserver.lib.ActionscriptObject;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.MessageChannel;
import com.altratek.altraserver.message.MsgSender;

public class ExtensionHelper {
	private final static ExtensionHelper instance = new ExtensionHelper();

	private AltraServer als;
	private SystemHandler sh;

	private ExtensionHelper() {
		als = AltraServer.getInstance();
		sh = als.getSystemHandler();
	}

	public static ExtensionHelper instance() {
		return instance;
	}

	public void sendXtResponse(ActionscriptObject ao, MessageChannel recipient) {
		MsgSender.sendXtResponse(ao, recipient, this);
	}

	public void sendXtResponse(ActionscriptObject ao, Collection<? extends MessageChannel> recipients) {
		MsgSender.sendXtResponse(ao, recipients, this);
	}

	public void sendXtResponse(ActionscriptObject ao, SocketChannel recipient) {
		MsgSender.sendXtResponse(ao, recipient, this);
	}

	public void sendByteResponse(byte[] arrByteCmd, MessageChannel recipient) {
		MsgSender.sendXtByteResponse(arrByteCmd, recipient, this);
	}

	public void sendByteResponse(byte[] arrByteCmd, Collection<? extends MessageChannel> recipients) {
		MsgSender.sendXtByteResponse(arrByteCmd, recipients, this);
	}

	public User canLogin(String userName, String password, SocketChannel sc, String targetZoneName)
			throws LoginException {
		return ChannelManager.instance.doLogin(userName, password, ZoneManager.instance.getZoneByName(targetZoneName), sc);
	}

	public User canLogin(String userName, String password, SocketChannel sc, String targetZoneName, int exceptedUserId)
			throws LoginException {
		return ChannelManager.instance.doLogin(userName, password, ZoneManager.instance.getZoneByName(targetZoneName), sc, exceptedUserId);
	}

	public void lostConnection(int userId) {
		lostConnection(userId, "from ext");
	}

	public void lostConnection(int userId, String userLostParam) {
		ChannelManager.instance.lostConn(userId, userLostParam);
	}

	public void lostConnection(SocketChannel sc, String userLostParam) {
		als.lostConn(sc, userLostParam);
	}

	public void dispatchPublicMessage(String msg, Room room, User user) {
		MsgSender.sendPublicMessage(msg, room, user.getUserId());
	}

	@Deprecated
	// 有User，没必要提供userId接口
	public void dispatchPublicMessage(String msg, Room room, int userId) {
		MsgSender.sendPublicMessage(msg, room, userId);
	}

	public Room createRoomOfManualRemoved(Zone zone, String name, int maxUsers) throws CreateRoomException {
		return zone.roomManager.createRoomOfManualRemoved(name, maxUsers);
	}

	public Room destroyRoom(Zone zone, int roomId) {
		return zone.roomManager.destroyRoom(roomId);
	}

	// currentRoomId 调用者如何得到？扩展的fromRoom吗？
	// 如果是客户端join，这个方法有必要吗？
	public void joinRoom(User user, int currentRoomId, String targetRoomName, boolean leaveRoom,
			boolean ignoreMaxCount, String customProperty, boolean notifyUser) throws JoinRoomException {
		if (user == null) {
			throw new IllegalArgumentException("user is null");
		}
		Zone zone = user.getZone();
		Room oldRoom = null;
		if (currentRoomId > -1) {
			oldRoom = zone.getRoomById(currentRoomId);
		} else {
			leaveRoom = false;
		}
		Room targetRoom = zone.getRoomByName(targetRoomName);
		if (targetRoom == null) {
			return;
		}

		user.getZone().roomManager.joinAndLeaveRoom(user, oldRoom, targetRoom, leaveRoom, ignoreMaxCount,
				customProperty, notifyUser, true);
	}

	@Deprecated
	public void notifyNewUserEnterRoom(int userId, String userName, boolean isMod,
			ConcurrentHashMap<String, UserVariable> userVar, int targetRoomId, LinkedList<SocketChannel> recipients,
			String customProperty) {
		notifyNewUserEnterRoom(userId, userName, isMod, userVar, targetRoomId, recipients, false, customProperty);
	}

	@Deprecated
	public void notifyNewUserEnterRoom(int userId, String userName, boolean isMod,
			ConcurrentHashMap<String, UserVariable> userVar, int targetRoomId, LinkedList<SocketChannel> recipients,
			boolean forceIntoStage, String customProperty) {
		MsgSender
				.sendUserEnterRoom(userId, userName, userVar, targetRoomId, recipients, forceIntoStage, customProperty);
		if (ServerLogger.debugEnabled) {
			ServerLogger.debugf("UserEnterRoom - Response sent to roommate(s) - ForceInit[%s]", forceIntoStage);
		}
	}

	public void notifyNewUserEnterRoom(User user, Map<String, UserVariable> userVar, Room room, String customProperty) {
		notifyNewUserEnterRoom(user, userVar, room, false, customProperty);
	}

	public void notifyNewUserEnterRoom(User user, Map<String, UserVariable> userVar, Room room, boolean forceIntoStage,
			String customProperty) {
		MsgSender.sendUserEnterRoom(user.getUserId(), user.getName(), userVar, room.getId(), room
				.getUserSocketsButOne(user), forceIntoStage, customProperty);
		if (ServerLogger.debugEnabled) {
			ServerLogger.debugf("UserEnterRoom - Response sent to roommate(s) - ForceInit[%s]", forceIntoStage);
		}
	}

	public void leaveRoom(User user, int roomId, boolean broadcastEvent) {
		user.getZone().roomManager.leaveRoom(user, roomId, -1, broadcastEvent);
	}

	public void notifyUserLeave(int userId, int roomId, String zoneName) {
		sh.notifyUserLeave(userId, roomId, ZoneManager.instance.getZoneByName(zoneName));
	}

	public void createAndJoinRoom(User user, String name, int maxUsers, int currentRoomId, boolean leaveRoom,
			boolean ignoreMaxCount, String customProperty) throws CreateRoomException, JoinRoomException {
		if (user == null) {
			throw new IllegalArgumentException("user is null");
		}

		user.getZone().roomManager.createAndJoinRoom(user, name, maxUsers, currentRoomId, leaveRoom, ignoreMaxCount,
				customProperty);
	}

	@Deprecated
	public void setRoomVariable(Room room, User user, String varName, String varType, String varValue, boolean priv,
			boolean persist, boolean setOwnership, boolean broadcastAll) {
		room.doSetRoomVariable(varName, varType, varValue, priv, persist, user, setOwnership);
	}

	public HashMap<String, RoomVariable> setRoomVariables(Room room, User user, HashMap<String, RoomVariable> vars,
			boolean setOwnership, boolean broadcastAll) {
		HashMap<String, RoomVariable> updatedRoomVariables = new HashMap<String, RoomVariable>();
		if (room != null && vars != null) {
			return room.setRoomVariables(user, vars, setOwnership, broadcastAll);
		}
		return updatedRoomVariables;
	}

	public void sendRoomVariablesUpdate(Room room, HashMap<String, RoomVariable> roomVars) {
		MsgSender.sendRoomVariablesUpdate(room, roomVars);
	}

	public void deleteUserVariable(User user, String varName) {
		setUserVariable(user, varName, null);
	}

	public void setUserVariable(User user, String varName, Object value) {
		HashMap<String, UserVariable> vars = new HashMap<String, UserVariable>();
		vars.put(varName, UserVariable.createUserVariable(value));

		setUserVariables(user, vars, true);
	}

	public HashMap<String, UserVariable> setUserVariables(User user, HashMap<String, UserVariable> vars,
			boolean broadcastAll) {
		return user.setUserVariables(vars, false, true, broadcastAll);
	}

	@Deprecated
	public void sendUserVariablesUpdate(int userId, Room room, HashMap<String, UserVariable> userVars) {
		MsgSender.sendUserVariablesUpdate(ChannelManager.instance.getUserById(userId), room, userVars, true);
	}

	public Zone getZone(String zoneName) {
		return ZoneManager.instance.getZoneByName(zoneName);
	}

	public LinkedList<Zone> getZoneCloneList() {
		return ZoneManager.instance.getAllZones();
	}

	public void disconnectUser(User user) {
		if (user != null) {
			als.lostConn(user.getSocketChannel(), "from ext");
		}
	}

	public User getUserByChannel(SocketChannel userChannel) {
		return ChannelManager.instance.getUserByChannel(userChannel);
	}

	public User getUserById(int id) {
		return ChannelManager.instance.getUserById(id);
	}

	public void sendAdminMessage(String msg, User recipient) {
		MsgSender.sendAdminMessage(msg, recipient);
	}

	public void registerConfigReloader(String type, IConfigReloader reloader) {
		als.getConfigReloaderManager().register(type, reloader);
	}
	
	public void reloadIpWhiteList() {
		IpFloodChecker.instance.reload();
	}
}
