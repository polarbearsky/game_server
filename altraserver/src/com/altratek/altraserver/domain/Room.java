package com.altratek.altraserver.domain;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.exception.JoinRoomException;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.MsgSender;

public class Room {
	private boolean isTemporary;
	private boolean isAutoRemoved;

	private int id;
	private int maxUsers;
	private int userCount;

	private String name;
	private String zoneName;

	private ConcurrentHashMap<String, RoomVariable> roomVariables;

	private List<User> users;

	public Room(int id, String name, int maxUsers, boolean isTemp, boolean isAutoRemoved, String zoneName) {
		this.id = id;
		this.name = name;
		this.maxUsers = maxUsers;
		this.isTemporary = isTemp;
		this.isAutoRemoved = isAutoRemoved;
		if (!this.isTemporary) {
			this.isAutoRemoved = false;
		}
		this.zoneName = zoneName;
		this.userCount = 0;
		this.roomVariables = new ConcurrentHashMap<String, RoomVariable>();
		// 房间列表移除频繁，所以用LinkedList。
		this.users = new LinkedList<User>();
	}

	public int getId() {
		return this.id;
	}

	public boolean empty() {
		return this.users.size() == 0;
	}

	public int getMaxUsers() {
		return maxUsers;
	}

	public int getUserCount() {
		return this.userCount;
	}

	public String getZoneName() {
		return zoneName;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isTemp() {
		return this.isTemporary;
	}

	public void setTemporary(boolean isTemporary) {
		this.isTemporary = isTemporary;
		if (!this.isTemporary) {
			this.isAutoRemoved = false;
		}
	}

	public boolean isAutoRemoved() {
		return this.isAutoRemoved;
	}

	public Integer[] getUserIdsArray() {
		synchronized (users) {
			Integer[] userIds = new Integer[users.size()];
			int i = 0;
			for (User u : users) {
				userIds[i] = u.getUserId();
				i++;
			}
			return userIds;
		}
	}

	public User[] getAllUsersArray() {
		synchronized (users) {
			User[] r = new User[this.users.size()];
			users.toArray(r);
			return r;
		}
	}

	public List<User> getUserList() {
		synchronized (users) {
			List<User> copyUserList = new ArrayList<User>(users.size());
			copyUserList.addAll(users);
			return copyUserList;
		}
	}

	public List<User> getUserListButOne(User excludedUser) {
		List<User> list = new ArrayList<User>();
		synchronized (users) {
			for (User u : users) {
				if (u != excludedUser) {
					list.add(u);
				}
			}
		}

		return list;
	}

	public List<User> getUserListButOne(int excludedUserId) {
		List<User> list = new ArrayList<User>();
		synchronized (users) {
			for (User u : users) {
				if (u.getUserId() != excludedUserId) {
					list.add(u);
				}
			}
		}

		return list;
	}

	public List<SocketChannel> getUserSockets() {
		synchronized (users) {
			List<SocketChannel> userChannelList = new ArrayList<SocketChannel>(this.users.size());
			for (User u : users) {
				userChannelList.add(u.getSocketChannel());
			}
			return userChannelList;
		}
	}

	public List<SocketChannel> getUserSocketsButOne(User excludedUser) {
		synchronized (users) {
			List<SocketChannel> userChannelList = new ArrayList<SocketChannel>();
			for (User u : users) {
				if (excludedUser != u) {
					userChannelList.add(u.getSocketChannel());
				}
			}
			return userChannelList;
		}
	}

	public LinkedList<Integer> getUserIds() {
		LinkedList<Integer> userIds = new LinkedList<Integer>();
		synchronized (users) {
			for (User u : users) {
				userIds.add(u.getUserId());
			}
		}
		return userIds;
	}

	public void join(User user, boolean ignoreMaxCount) throws JoinRoomException {
		boolean addOk = false;
		synchronized (users) {
			if (contains(user)) {
				ServerLogger.errorf("Join room error : already in room[%s], user[%s]", this.name, user.getName());
				throw new JoinRoomException(ConfigData.JOINROOM_AREADY_IN);
			}

			if (ignoreMaxCount || userCount < maxUsers) {
				users.add(user);
				addOk = true;
			} else {
				if (ServerLogger.debugEnabled) {
					ServerLogger.debugf("Join room error : room[%s] full, user[%s]", this.name, user.getName());
				}
				throw new JoinRoomException(ConfigData.JOINROOM_FULL);
			}
		}

		if (addOk) {
			userCount++;
			user.addRoomConnected(this);

			if (ServerLogger.infoEnabled) {
				ServerLogger.infof("User[%s] joined Room[%s]", user.getName(), getName());
			}
		}
	}

	// 内部方法，调用者负责users同步。
	private boolean contains(User user) {
		boolean result = false;
		for (User u : users) {
			if (u.getUserId() == user.getUserId()) {
				result = true;
				break;
			}
		}
		return result;
	}

	public boolean removeUser(User user, boolean updateUserRoomList, boolean destroyVars) {
		boolean removeOk = false;
		synchronized (users) {
			removeOk = users.remove(user);
		}

		if (removeOk) {
			if (updateUserRoomList) {
				user.removeRoom(this);
			}

			userCount--;

			if (ServerLogger.infoEnabled) {
				ServerLogger.infof("User[%s] ] left Room[%s], room user count : %s", user.getName(), getName(),
						this.users.size());
			}

			if (ServerLogger.debugEnabled) {
				if (userCount < 0 || userCount > maxUsers)
					ServerLogger.errorf("room remove user : UserCount[%s] out of range - Room[%s] Range[0 ~ %s]",
							userCount, name, maxUsers);
			}

			AltraServer.getInstance().getSystemHandler().notifyUserLeave(user.getUserId(), id, user.getZone());

			if (destroyVars) {
				deleteRoomVariableAndNotifyUsers(user);
			}
		} else {
			ServerLogger.errorf("Can not remove User[%s] from Room[%s]", user.getName(), name);
		}
		return removeOk;
	}

	private void deleteRoomVariableAndNotifyUsers(User user) {
		HashMap<String, RoomVariable> udpatedRoomVariable = new HashMap<String, RoomVariable>();
		LinkedList<String> varNames = new LinkedList<String>(roomVariables.keySet());
		// 遍历每个房间变量，删除变量拥有者是user的变量
		for (Iterator<String> it = varNames.iterator(); it.hasNext();) {
			String varName = it.next();
			RoomVariable roomVar = roomVariables.get(varName);

			RoomVariable deletedVar = new RoomVariable("x", "", null, false, false);
			User varOwner = roomVar.getOwner();
			if (varOwner != null && varOwner == user && deleteVariable(varName, user)) {
				udpatedRoomVariable.put(varName, deletedVar);
				if (ServerLogger.infoEnabled) {
					ServerLogger.info((new StringBuilder("Deleted RoomVar[")).append(varName).append("] from Room[")
							.append(getName()).append("]").toString());
				}
			}
		}
		// 若有变量被删除
		if (udpatedRoomVariable.size() > 0) {
			if (ServerLogger.debugEnabled) {
				ServerLogger.debug((new StringBuilder("Deleted all RoomVars owned by User[")).append(user.getName())
						.append("]").toString());
			}
			MsgSender.sendRoomVariablesUpdate(this, udpatedRoomVariable);
		}
	}

	public boolean deleteVariable(String varName, User owner) {
		boolean result = false;
		RoomVariable roomVariable = roomVariables.get(varName);
		if (roomVariable != null) {
			// 若persisiten为true,则当creator离开该zone时,该roomvariable才能被删除
			if (roomVariable.isPersistent()) {
				result = false;
			} else if (!roomVariable.isPrivate() || roomVariable.isPrivate() && owner == roomVariable.getOwner()) {// 可以被删除的情况：非私有的
				// or
				// 私有的/拥有者发起删除请求
				roomVariables.remove(varName);
				result = true;
				if (ServerLogger.infoEnabled) {
					ServerLogger.info(new StringBuilder("RoomVar[").append(varName).append("] deleted by User[")
							.append(owner.getName()).append("]").toString());
				}
			}
		} else// 对应的变量为null值，认为被删除成功
		{
			result = true;
			if (ServerLogger.infoEnabled) {
				ServerLogger.info(new StringBuilder("RoomVar[").append(varName).append("] deleted by unknown user")
						.toString());
			}
		}
		return result;
	}

	// ========== 房间变量 ==========

	public HashMap<String, RoomVariable> getCloneOfRoomVariables() {
		HashMap<String, RoomVariable> result = new HashMap<String, RoomVariable>();
		for (Iterator<Entry<String, RoomVariable>> it = roomVariables.entrySet().iterator(); it.hasNext();) {
			Entry<String, RoomVariable> entry = it.next();
			String varName = entry.getKey();
			RoomVariable rv = entry.getValue();
			result.put(varName, rv);
		}
		return result;
	}

	public LinkedList<String> getVariableNames() {
		return new LinkedList<String>(roomVariables.keySet());
	}

	public RoomVariable getVariable(String varName) {
		return roomVariables.get(varName);
	}

	public ConcurrentHashMap<String, RoomVariable> getRoomVariables() {
		return roomVariables;
	}

	public boolean setVariable(String varName, String varType, String varValue, boolean priv, boolean persistent,
			User owner, boolean setOwnership) {
		boolean result = false;
		if (varName == null || varName.equals(""))
			return result;
		varValue = varValue == null ? "" : varValue;
		// 查找并设置指定房间变量
		RoomVariable roomVariable = null;
		// roomVariables已经是支持并发的哈希表了，所以这里不用再进行锁定
		// synchronized (roomVariables)
		// {
		roomVariable = roomVariables.get(varName);
		if (roomVariable != null) {
			if (!roomVariable.isPrivate() || (roomVariable.isPrivate() && roomVariable.getOwner() == owner)) {
				if (!roomVariable.getValue().equals(varValue)) {
					roomVariable.setValue(varValue);
					result = true;
				}
				if (!roomVariable.getType().equals(varType)) {
					roomVariable.setType(varType);
					result = true;
				}
				if (roomVariable.isPrivate() != priv) {
					roomVariable.setPrivate(priv);
					result = true;
				}
				if (roomVariable.isPersistent() != persistent) {
					roomVariable.setPersistent(persistent);
					result = true;
				}
				// 此处没有设置result=true，是因为即使设置了所有者，返回给客户端的消息也不包括这部分内容
				if (setOwnership)
					roomVariable.setOwner(owner);
			}
		}
		// 没找到目标变量，则尝试新建一个RoomVariable对象
		else {
			if (roomVariables.size() < ConfigData.MAX_ROOM_VARS || ConfigData.MAX_ROOM_VARS == -1) {
				// owner不会为null,所以roomVariable肯定有主人
				roomVariable = new RoomVariable(varType, varValue, owner, persistent, priv);
				roomVariables.put(varName, roomVariable);
				result = true;
			} else// 超出房间变量数目上限
			{
				ServerLogger.errorf("RoomVars limit exceeded in Room[%s]", name);
			}
		}
		if (result) {
			if (ServerLogger.debugEnabled) {
				ServerLogger.debug((new StringBuilder("Setting RoomVar[")).append(varName).append("]=").append(
						roomVariable.getValue()).append("(Priv=").append(roomVariable.isPrivate())
						.append(" & Persist=").append(roomVariable.isPersistent()).append(") in Room[").append(
								getName()).append("]").toString());
			}
		}
		return result;
	}

	// 来自extensionhelper
	public HashMap<String, RoomVariable> setRoomVariables(User user, HashMap<String, RoomVariable> vars,
			boolean setOwnership, boolean broadcastAll) {
		HashMap<String, RoomVariable> updatedRoomVariables = new HashMap<String, RoomVariable>();
		if (vars == null) {
			return updatedRoomVariables;
		}

		for (Iterator<Entry<String, RoomVariable>> it = vars.entrySet().iterator(); it.hasNext();) {
			Entry<String, RoomVariable> entry = it.next();
			String varName = entry.getKey();
			RoomVariable roomVariable = entry.getValue();

			if (varName.length() > 0) {
				if (doSetRoomVariable(varName, roomVariable.getType(), roomVariable.getValue(), roomVariable
						.isPrivate(), roomVariable.isPersistent(), user, setOwnership)) {
					updatedRoomVariables.put(varName, roomVariable);
				}
			}
		}

		if (updatedRoomVariables.size() > 0 && broadcastAll) {
			MsgSender.sendRoomVariablesUpdate(this, updatedRoomVariables);
		}

		return updatedRoomVariables;
	}

	// 来自extensionhelper
	public boolean doSetRoomVariable(String varName, String varType, String varValue, boolean varPrivate,
			boolean varPersist, User user, boolean setOwner) {
		boolean succeed = false;
		if (varType.equals("x")) {// 删除房间变量
			if (deleteVariable(varName, user)) {
				succeed = true;
			}
		} else if (setVariable(varName, varType, varValue, varPrivate, varPersist, user, setOwner)) {
			// 设定房间变量成功
			succeed = true;
		}
		return succeed;
	}
}