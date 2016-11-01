package com.altratek.altraserver.domain;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.MessageChannel;
import com.altratek.altraserver.message.MsgSender;

public class User implements MessageChannel {
	private static AtomicInteger autoId = new AtomicInteger(1);

	private boolean isAdmin = false;

	private int id;
	private long lastOperationTime;
	private long loginTime;

	private Zone zone;
	private String name;
	private String ip;

	public volatile boolean lost = false;

	private LinkedList<Room> roomsConnected = new LinkedList<Room>();
	private LinkedList<Long> roomsCreatedTime = new LinkedList<Long>();

	private ConcurrentHashMap<String, UserVariable> userVars = new ConcurrentHashMap<String, UserVariable>();
	// 用户属性，提供给extension使用，保存用户的特殊属性，不同于userVars，并无系统自带的增/改/删操作及对应的客户端命令
	public ConcurrentHashMap<Object, Object> userProps = new ConcurrentHashMap<Object, Object>();

	private SocketChannel userChannel;

	public User(SocketChannel channel, String name, Zone zone) {
		this(channel, name, zone, genUserId());
	}

	public User(SocketChannel channel, String name, Zone zone, int userId) {
		this.id = userId;
		this.userChannel = channel;
		this.zone = zone;
		this.name = name.length() < 1 ? (new StringBuilder("Guest_")).append(this.id).toString() : name;
		this.loginTime = System.currentTimeMillis();
		this.lastOperationTime = this.loginTime;
		this.ip = AltraServer.getInstance().getIpBySocketChannel(this.userChannel);
	}

	public boolean isAdmin() {
		return this.isAdmin;
	}

	public void setAdmin() {
		this.isAdmin = true;
	}

	public int getUserId() {
		return this.id;
	}

	private static int genUserId() {
		synchronized (autoId) {
			int newId = autoId.getAndIncrement();
			if (autoId.intValue() > ConfigData.MAX_USER_COUNT) {
				autoId.set(1);
			}
			return newId;
		}
	}

	public String getName() {
		return this.name;
	}

	public String getName_AsZoneUserMapKey() {
		return this.name;
	}

	// 提供该get只是为了保持就接口兼容
	// call getZone
	@Deprecated()	
	public String getZoneName() {
		return this.zone.getName();
	}

	public Zone getZone() {
		return this.zone;
	}

	public long getLoginTime() {
		return this.loginTime;
	}

	public String getIP() {
		return ip;
	}

	@Override
	public SocketChannel getSocketChannel() {
		return this.userChannel;
	}

	public void setLastOperationTime(long lastOperationTime) {
		this.lastOperationTime = lastOperationTime;
	}

	public long getLastOperationTime() {
		return this.lastOperationTime;
	}

	public void updateOperationTime() {
		this.setLastOperationTime(System.currentTimeMillis());
	}

	public UserVariable getVariable(String varName) {
		return userVars.get(varName);
	}

	public Integer getIntVarValue(String varName) {
		UserVariable uv = getVariable(varName);
		return uv == null ? null : uv.getIntValue();
	}

	public Boolean getBoolVarValue(String varName) {
		UserVariable uv = getVariable(varName);
		return uv == null ? null : uv.getBoolValue();
	}

	public String getStringVarValue(String varName) {
		UserVariable uv = getVariable(varName);
		return uv == null ? null : uv.getStringValue();
	}

	public ConcurrentHashMap<String, UserVariable> getVariables() {
		return userVars;
	}

	public LinkedList<String> getVariableNames() {
		return new LinkedList<String>(userVars.keySet());
	}

	public HashMap<String, UserVariable> getCloneOfUserVariables() {
		HashMap<String, UserVariable> result = new HashMap<String, UserVariable>();
		result.putAll(this.userVars);
		return result;
	}

	/**
	 * 设置用户变量，若名为varName的用户变量已经存在，则进行修改，否则新增该变量
	 */
	public boolean setVariable(String varName, String varValue, String varType) {
		boolean result = false;
		if (varName.length() == 0) {
			return result;
		}
		varValue = varValue == null ? "" : varValue;
		// 查找并设置指定用户房间变量
		UserVariable userVar = null;
		synchronized (userVars) {
			userVar = userVars.get(varName);
			if (userVar != null) {
				userVar.setType(varType);
				userVar.setValue(varValue);
				result = true;
				if (ServerLogger.infoEnabled) {
					ServerLogger.infof("Setting UserVar[%s]=%s", varName, varValue);
				}
			} else {
				if (userVars.size() < ConfigData.MAX_USER_VARS || ConfigData.MAX_USER_VARS == -1) {
					userVars.put(varName, new UserVariable(varType, varValue));
					if (ServerLogger.infoEnabled) {
						ServerLogger.infof("Setting UserVar[%s] = %s  - created by User[%s]", varName, varValue, name);
					}
					result = true;
				} else// 超出用户个人可以创建的变量上限
				{
					ServerLogger.errorf("UserVar's limit exceeded for User[%s]", name);
				}
			}
		}
		return result;
	}

	public int getRoomsConnectedCount() {
		return roomsConnected.size();
	}

	public boolean checkCreateRoomFrequency(String roomName) {
		if (zone.getTimePeriod_MaxRoomsPerUser() <= 0) {
			// 无限制，不验证
			return true;
		}
		if (this.roomsCreatedTime.size() >= zone.getMaxRoomsPerUser()) {
			if (System.currentTimeMillis() - this.roomsCreatedTime.getFirst() <= zone.getTimePeriod_MaxRoomsPerUser()) {
				ServerLogger.errorf("[%s] create room too frequently when create [%s]", this.name, roomName);
				return false;
			} else {
				this.roomsCreatedTime.removeFirst();
			}
		}
		return true;
	}

	public void addRoomConnected(Room room) {
		synchronized (roomsConnected) {
			roomsConnected.add(room);
		}
	}

	public void addCreatedRoom(Room room) {
		synchronized (roomsCreatedTime) {
			roomsCreatedTime.add(System.currentTimeMillis());
		}
	}

	public void removeRoom(Room room) {
		synchronized (roomsConnected) {
			roomsConnected.remove(room);
		}
	}

	public void deleteVariable(String varName) {
		this.userVars.remove(varName);
		if (ServerLogger.infoEnabled) {
			ServerLogger.info(new StringBuilder("UserVar[").append(varName).append("] deleted by User[").append(
					getName()).append("]").toString());
		}
	}

	public boolean inRoom(int roomId) {
		synchronized (roomsConnected) {
			for (Room room : roomsConnected) {
				if (room != null && room.getId() == roomId) {
					return true;
				}
			}
		}
		return false;
	}

	public int[] getRoomIdConnected() {
		int roomIds[];
		synchronized (roomsConnected) {
			roomIds = new int[roomsConnected.size()];
			int c = 0;
			for (Room r : roomsConnected) {
				roomIds[c++] = r.getId();
			}
		}
		return roomIds;
	}

	@Deprecated
	// call getInRoom
	public int getFristConnectedRoomId() {
		Room r = getInRoom();
		return r == null ? -1 : r.getId();
	}

	@Deprecated
	// call getInRoom
	public int getRoom() {
		return getFristConnectedRoomId();
	}

	// 方法命名假设一个用户不会同时在两个房间
	public Room getInRoom() {
		try {
			return roomsConnected.getFirst();
		} catch (NoSuchElementException e) {
			// roomsConnected.size == 0
			return null;
		}
	}

	public void exitAllRooms() {
		LinkedList<Room> waitToRemovedRoomList = new LinkedList<Room>();
		synchronized (roomsConnected) {
			for (Room room : roomsConnected) {
				boolean removeSuccess = room.removeUser(this, false, false);
				if (!removeSuccess) {
					ServerLogger.errorf("Problems during user removed from Room - User[%s]", getName());
				} else {
					if (room.isAutoRemoved()) {
						waitToRemovedRoomList.add(room);
					}
				}
			}
		}

		this.getZone().roomManager.tryRemoveEmptyTempRooms(waitToRemovedRoomList);
	}

	// 如果是用户通过客户端发起的用户变量修改，要验证，不需要通知自己。
	// 如果是扩展发起的用户变量修改，不验证，正确性由扩展保证，要通知用户本人
	public HashMap<String, UserVariable> setUserVariables(HashMap<String, UserVariable> vars, boolean validate,
			boolean notifySelf, boolean broadcastAll) {
		HashMap<String, UserVariable> updatedUserVariables = new HashMap<String, UserVariable>();

		for (Entry<String, UserVariable> entry : vars.entrySet()) {
			String varName = entry.getKey();
			UserVariable userVariable = entry.getValue();

			if (validateUserVariable(varName, userVariable, validate)) {
				if (this.changeUserVariable(varName, userVariable)) {
					updatedUserVariables.put(varName, userVariable);
				}
			}
		}

		if (broadcastAll && updatedUserVariables.size() > 0) {
			int roomIds[] = this.getRoomIdConnected();
			Zone zone = this.getZone();
			for (int i = 0; i < roomIds.length; i++) {
				Room room = zone.getRoomById(roomIds[i]);
				if (room != null) {
					MsgSender.sendUserVariablesUpdate(this, room, updatedUserVariables, notifySelf);
				}
			}
		}

		return updatedUserVariables;
	}

	private boolean validateUserVariable(String varName, UserVariable userVariable, boolean validate) {
		if (!validate) {
			return true;
		}

		return AltraServer.getInstance().getMessageValidator().validateUserVariable(varName, userVariable);
	}

	private boolean changeUserVariable(String varName, UserVariable uv) {
		boolean succeed = false;
		if (uv.getType().equals("x")) {// 删除用户变量
			this.deleteVariable(varName);
			succeed = true;
		} else if (this.setVariable(varName, uv.getValue(), uv.getType())) {// 设定用户变量成功
			succeed = true;
		}
		return succeed;
	}
}