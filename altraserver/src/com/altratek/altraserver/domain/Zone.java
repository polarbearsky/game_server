package com.altratek.altraserver.domain;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.dom4j.Element;

import com.altratek.altraserver.event.SystemEventRegistry;
import com.altratek.altraserver.extensions.AbstractExtension;
import com.altratek.altraserver.extensions.ExtensionManager;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.MsgSender;
import com.altratek.altraserver.util.XmlUtility;

public class Zone {
	private static final int DEFAULT_MAX_USERS = -1;
	private static final int DEFAULT_MAX_ROOM_PER_USER = 5;
	private static final int DEFAULT_TIME_PERIOD_MRPU = -1;
	private static final boolean DEFAULT_SEND_VAR_OPTION = false;
	public static final int DEFAULT_MAX_ROOMS = -1;
	public static final int DEFAULT_MAX_ROOM_NAME_LEN = 20;

	private String zoneName;

	private int maxUsers = DEFAULT_MAX_USERS;	

	private int maxRoomsPerUser = DEFAULT_MAX_ROOM_PER_USER;
	private long timePeriod_MRPU = DEFAULT_TIME_PERIOD_MRPU;
	// 是否在发送的room名单中包含变量
	private boolean sendVarsOnRoomList = DEFAULT_SEND_VAR_OPTION;

	private boolean receivePublicMsgInternalEvent = false;
	private boolean receivePrivateMsgInternalEvent = false;

	private ConcurrentHashMap<String, User> name_User;// userName作key，方便userName的验证

	public final ExtensionManager extensionManager;

	private SystemEventRegistry eventRegistry = new SystemEventRegistry();

	public final RoomManager roomManager;

	public static Zone adminZone;

	public Zone(String name) {
		zoneName = name;
		name_User = new ConcurrentHashMap<String, User>();
		extensionManager = new ExtensionManager(this);
		roomManager = new RoomManager(this);
	}

	public void initWithConfig(Element zoneNode) {
		this.setMaxUsers(XmlUtility.getAttributeAsInt(zoneNode, "maxUsers", DEFAULT_MAX_USERS));
		this.roomManager.setMaxRooms(XmlUtility.getAttributeAsInt(zoneNode, "maxRooms", DEFAULT_MAX_ROOMS));
		this.maxRoomsPerUser = XmlUtility.getAttributeAsInt(zoneNode, "maxRoomsPerUser", DEFAULT_MAX_ROOM_PER_USER);

		this.timePeriod_MRPU = XmlUtility.getAttributeAsInt(zoneNode, "timePeriod_MaxRoomsPerUser",
				DEFAULT_TIME_PERIOD_MRPU);
		if (this.timePeriod_MRPU > 0) {
			this.timePeriod_MRPU = this.timePeriod_MRPU * 1000;
		}

		this.sendVarsOnRoomList = XmlUtility.getAttributeAsBool(zoneNode, "roomListVars", DEFAULT_SEND_VAR_OPTION);
		this.roomManager.setMaxRoomNameLen(XmlUtility.getContent_FirstChildNamedAsInt(zoneNode, "MaxRoomNamesLen",
				DEFAULT_MAX_ROOM_NAME_LEN));		
	}	

	public String getName() {
		return zoneName;
	}	

	public int getMaxUsers() {
		return maxUsers;
	}
	
	private void setMaxUsers(int maxCount) {
		if (maxCount <= 0)
			maxCount = DEFAULT_MAX_USERS;
		maxUsers = maxCount;
	}

	// ========== 读取用户信息 ==========

	public SocketChannel getSocketChannelByName(String userName) {
		User user = getUserByName(userName);
		if (user != null) {
			return user.getSocketChannel();
		} else {
			return null;
		}
	}

	public User getUserByName(String userName) {
		return name_User.get(userName);
	}

	// 返回null表示目标用户没有在本区域
	public Integer getUserIdByName(String userName) {
		if (userName == null) {
			return null;
		}
		User user = name_User.get(userName);
		return user != null ? user.getUserId() : null;
	}

	public LinkedList<SocketChannel> getNonGameRoomUserChannelList() {
		LinkedList<SocketChannel> userChannelList = new LinkedList<SocketChannel>();
		for (Iterator<Room> it = getRoomList().iterator(); it.hasNext();) {
			Room room = it.next();
			if (room != null) {
				userChannelList.addAll(room.getUserSockets());
			}
		}
		return userChannelList;
	}

	public List<SocketChannel> getUserChannelList() {
		LinkedList<SocketChannel> userChannelList = new LinkedList<SocketChannel>();
		for (User user : name_User.values()) {
			if (user != null) {
				userChannelList.add(user.getSocketChannel());
			}
		}
		return userChannelList;
	}

	public List<User> getUserList() {
		return new ArrayList<User>(name_User.values());
	}

	public int getUserCount() {
		return name_User.size();
	}

	public boolean hasPlaceToLogin() {
		if (maxUsers <= 0) {
			return true;
		}

		return name_User.size() < maxUsers;
	}

	public void addUserName(User user) {
		name_User.put(user.getName_AsZoneUserMapKey(), user);
	}

	// 验证用户名userName是否可用
	public boolean validateUserName(String userName) {
		return !name_User.containsKey(userName);
	}

	// ========== 用户登出/删除用户的房间变量 ==========

	public void removeUserName(String userName, User user) {
		name_User.remove(userName);
	}

	public void destroyVariables(User owner) {
		HashMap<String, RoomVariable> deletedRoomVariables = null;
		List<Room> rooms = roomManager.getRoomList();
		for (Room room : rooms) {
			deletedRoomVariables = new HashMap<String, RoomVariable>();
			ConcurrentHashMap<String, RoomVariable> roomVars = room.getRoomVariables();

			for (Iterator<Entry<String, RoomVariable>> it2 = roomVars.entrySet().iterator(); it2.hasNext();) {
				Entry<String, RoomVariable> entry = it2.next();
				String varName = entry.getKey();
				RoomVariable rv = entry.getValue();
				if (rv != null && rv.getOwner() == owner) {
					deletedRoomVariables.put(varName, new RoomVariable("x", "", null, false, false));
					it2.remove();
					if (ServerLogger.infoEnabled) {
						ServerLogger.info(new StringBuilder("Delete Var[").append(varName).append("] in Room[").append(
								room.getName()).append("]").toString());
					}
				}
			}

			if (deletedRoomVariables.size() > 0) {
				MsgSender.sendRoomVariablesUpdate(room, deletedRoomVariables);				
			}
		}
	}

	// ========== 房间相关设定 ==========

	public boolean getVarsOnRoomList() {
		return sendVarsOnRoomList;
	}

	public int getMaxRoomsPerUser() {
		return maxRoomsPerUser;
	}

	public long getTimePeriod_MaxRoomsPerUser() {
		return timePeriod_MRPU;
	}

	public int getMaxRooms() {
		return roomManager.getMaxRooms();
	}

	// 兼容旧api
	public Room getRoomById(int roomId) {
		return roomManager.getRoomById(roomId);
	}

	// 兼容旧api
	public Room getRoomByName(String roomName) {
		return roomManager.getRoomByName(roomName);
	}

	// 兼容旧api
	public LinkedList<Room> getRoomList() {
		return roomManager.getRoomList();
	}

	// 兼容旧api
	public int getRoomCount() {
		return roomManager.getRoomCount();
	}

	public boolean isPubMsgInternalEventEnabled() {
		return receivePublicMsgInternalEvent;
	}

	public void setPubMsgInternalEvent(boolean bl) {
		receivePublicMsgInternalEvent = bl;
	}

	public boolean isPrivMsgInternalEventEnabled() {
		return receivePrivateMsgInternalEvent;
	}

	public void setPrivMsgInternalEvent(boolean bl) {
		receivePrivateMsgInternalEvent = bl;
	}

	public void destoryExtensions() {
		extensionManager.destory();
	}

	@Deprecated
	public AbstractExtension getExtension(int id) {
		return (AbstractExtension) extensionManager.getExtension(id);		
	}

	public SystemEventRegistry getEventRegistry() {
		return this.eventRegistry;
	}
}