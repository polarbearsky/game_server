package com.altratek.altraserver.domain;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.altratek.altraserver.AltraServer;
import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.event.SystemEventDispatcher;
import com.altratek.altraserver.exception.CreateRoomException;
import com.altratek.altraserver.exception.JoinRoomException;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.message.ResponseMessage;
import com.altratek.altraserver.message.response.JoinKOMessage;
import com.altratek.altraserver.message.response.JoinOKMessage;
import com.altratek.altraserver.message.response.LeaveRoomResponseMessage;
import com.altratek.altraserver.message.response.UERMessage;

public class RoomManager {
	private final Zone zone;
	private int maxRooms = Zone.DEFAULT_MAX_ROOMS;
	private int maxRoomNameLen = Zone.DEFAULT_MAX_ROOM_NAME_LEN;
	// 房间id其实是没有必要的，用房间名称就能标识房间。
	// id2Room存在的目的是为了兼容修改代价大的旧代码，旧代码是用id标识房间的。
	private final ConcurrentHashMap<Integer, Room> id2Room = new ConcurrentHashMap<Integer, Room>();
	// 客户端不保存房间列表了，join只能传name上来(leave可以传id，客户端都在房间了，肯定知道id)，
	// 所以要这个映射。其实room.id可以不需要，但都用name做id，旧代码改动太大了。
	// 这个map是检查房间是否存在主导。
	private final ConcurrentHashMap<String, Room> name2Room = new ConcurrentHashMap<String, Room>();
	// 每个zone一个room id序列即可，不需要process全局一个。
	private final static AtomicInteger autoRoomId = new AtomicInteger(0);
	// 这个不保证精确，不会有影响，所以没加严格的锁。
	private final AtomicInteger roomCount = new AtomicInteger(0);
	// 加入和remove房间互斥锁对象。
	private final Object joinAndRemoveMutex = new Object();

	public RoomManager(Zone zone) {
		this.zone = zone;
	}

	public int getMaxRooms() {
		return this.maxRooms;
	}

	public void setMaxRooms(int value) {
		if (value <= 0) {
			value = -1;
		}
		this.maxRooms = value;
	}

	public void setMaxRoomNameLen(int value) {
		this.maxRoomNameLen = value;
	}

	private int genRoomId() {
		int id;
		// 即使用atom，因为要到了max要转头，所以还是要sync
		synchronized (RoomManager.autoRoomId) {
			id = RoomManager.autoRoomId.incrementAndGet();
			if (id == Integer.MAX_VALUE) {
				// 固定房间占了前面的id，而且不释放，所有转头要跳过这部分
				// 为了简单，假设该进程的(zone数 * 固定房间数) < 100000
				RoomManager.autoRoomId.set(100000);
			}
		}
		return id;
	}

	// 创建房间，包括临时和非临时
	public Room createRoom(String name, int maxUsers, boolean isTemp, boolean isAutoRemoved) throws CreateRoomException {
		if (maxUsers < 1) {
			throw new IllegalArgumentException("maxUsers < 1 error.");
		}

		checkName(name);

		checkZoneRoomCount();

		Room room = new Room(genRoomId(), name, maxUsers, isTemp, isAutoRemoved, zone.getName());
		addRoom(room);

		return room;
	}

	public Room createRoomOfManualRemoved(String name, int maxUsers) throws CreateRoomException {
		return createTempRoom(null, name, maxUsers, false, null, null, false, true);
	}

	private void logTempRoomCreate(boolean isAutoRemoved, User creator, String roomName) {
		String removePolicy = isAutoRemoved ? "auto" : "manual";
		String creatorName = creator != null ? creator.getName() : "null";
		ServerLogger.debugf("Create a temp room [%s], remove [%s], creator [%s]", roomName, removePolicy, creatorName);
	}

	private Room createTempRoom(User creator, String roomName, int maxUsers, boolean isAutoRemoved,
			HashMap<String, RoomVariable> roomVars, User varsOwner, boolean setOwnership, boolean broadcastEvent)
			throws CreateRoomException {

		Room room = this.createTempRoomCore(creator, roomName, maxUsers, isAutoRemoved);

		this.createTempRoomPost(creator, room, roomVars, varsOwner, setOwnership, broadcastEvent);

		return room;
	}

	// 创建临时房间的修改数据部分，也就是可能竞争资源部分（还有一部分是createTempRoomPost）。
	// 抽出两部分，是为了减少锁的粒度。
	private Room createTempRoomCore(User creator, String roomName, int maxUsers, boolean isAutoRemoved)
			throws CreateRoomException {
		if (creator != null) {
			checkCreatorRoomFrequency(creator, roomName);
		}

		Room room = createRoom(roomName, maxUsers, true, isAutoRemoved);

		if (ServerLogger.debugEnabled) {
			this.logTempRoomCreate(isAutoRemoved, creator, roomName);
		}

		return room;
	}

	// 创建临时房间后，一些善后操作。
	private void createTempRoomPost(User creator, Room room, HashMap<String, RoomVariable> roomVars, User varsOwner,
			boolean setOwnership, boolean broadcastEvent) {
		if (creator != null) {
			creator.addCreatedRoom(room);
		}

		// 设置roomvariable，暂时没用，因为调用者roomVars参数都传null。
		if (roomVars != null && roomVars.size() > 0) {
			room.setRoomVariables(varsOwner, new HashMap<String, RoomVariable>(), setOwnership, false);
		}

		// 广播该消息
		if (broadcastEvent) {
			triggerRoomCreate(room, creator);
		}
	}

	public void joinAndLeaveRoom(User user, Room oldRoom, Room targetRoom, boolean leaveRoom, boolean ignoreMaxCount,
			String customProperty, boolean notifyUser, boolean notifyErrorToUser) throws JoinRoomException {

		joinRoom(targetRoom, user, ignoreMaxCount, customProperty, notifyUser, notifyErrorToUser);

		if (oldRoom != null && leaveRoom) {
			leaveRoomInner(oldRoom, user);
		}
	}

	public void joinRoom(Room targetRoom, User user, boolean ignoreMaxCount, String customProperty, boolean notifyUser,
			boolean notifyErrorToUser) throws JoinRoomException {
		try {
			targetRoom.join(user, ignoreMaxCount);

			// 这里有并发问题，假设A, B同时进入房间
			// A在系统线程a先进room，执行到sendJoinOkMsg，构建了A的JoinOk，此时JoinOk.UserList没有B，消息还未发
			// B在系统线程b后进room，在A的JoinOk发之前，执行完sendUserEnterRoomMsg，给A发了B EnterRoom
			// 这样客户端先收到B EnterRoom，后收到JoinOk，因为A还不在该room，所以EnterRoom会丢弃
			// 造成了在A的客户端的room.userList没有B
			if (notifyUser) {
				sendJoinOkMsg(targetRoom, customProperty, user);
				sendUserEnterRoomMsg(user, targetRoom, customProperty);
			}

			triggerUserJoinRoom(user, targetRoom);
		} catch (JoinRoomException jre) {
			if (notifyUser && notifyErrorToUser) {
				sendJoinKoMsg(jre.getMessage(), user);
			}

			throw jre;
		}
	}

	// 保证临时房间的加入和移除不冲突——加入的房间被移除了。
	// 用一个相对大的临界区保证线程安全，尽管性能略差，为了代码简单，做了此选择。
	// joinAndRemoveMutex锁保证移除和createAndJoin互斥。
	public void createAndJoinRoom(User user, String name, int maxUsers, int currentRoomId, boolean leaveRoom,
			boolean ignoreMaxCount, String customProperty) throws CreateRoomException, JoinRoomException {

		Room oldRoom = null;
		if (currentRoomId > -1) {
			oldRoom = this.getRoomById(currentRoomId);
		} else {
			leaveRoom = false;
		}

		boolean createOk = false;
		boolean joinOk = false;
		Room room = null;
		try {
			synchronized (joinAndRemoveMutex) {
				room = name2Room.get(name);
				if (room == null) {
					try {
						room = createTempRoomCore(user, name, maxUsers, true);
						createOk = true;
					} catch (CreateRoomException e) {
						if (e.roomExist != null) {
							room = e.roomExist;
							ServerLogger.error("create auto removed room race condition ocurrs : "
									+ e.roomExist.getName());
						}
					}
				}
				room.join(user, ignoreMaxCount);
				joinOk = true;
			}

			// 创建成功，join失败，这个执行不到，会有这种情况吗？
			if (createOk) {
				this.createTempRoomPost(user, room, null, null, false, true);
			}

			if (joinOk) {
				sendJoinOkMsg(room, customProperty, user);
				sendUserEnterRoomMsg(user, room, customProperty);
				triggerUserJoinRoom(user, room);

				if (oldRoom != null && leaveRoom) {
					leaveRoomInner(oldRoom, user);
				}
			}
		} catch (JoinRoomException jre) {
			sendJoinKoMsg(jre.getMessage(), user);
			throw jre;
		}
	}

	public Room destroyRoom(int roomId) {
		Room room = zone.getRoomById(roomId);
		// 移除手工创建的房间，所以不判断是否是空房间。
		if (room != null && room.isTemp()) {
			removeRoom(room, false);
			return room;
		}
		return null;
	}

	public void leaveRoom(User user, int leaveRoomId, int fromRoomId, boolean notifyRequestor) {
		if (user.getRoomsConnectedCount() < 1) {
			return;
		}

		Room leaveRoom = getRoomById(leaveRoomId);
		if (leaveRoom == null) {
			return;
		}

		leaveRoomInner(leaveRoom, user);

		// 返回信息给刚离开房间的用户
		if (notifyRequestor) {
			sendLeaveRoomMsg(fromRoomId, leaveRoomId, user);
		}
	}

	private void leaveRoomInner(Room room, User user) {
		room.removeUser(user, true, true);
		triggerUserLeaveRoom(user, room);
		tryRemoveEmptyTempRoom(room);
	}

	private void checkName(String name) throws CreateRoomException {
		String err = null;
		if (name == null || "".equals(name)) {
			err = "missing room name";
		} else if (name.length() > this.maxRoomNameLen) {
			err = "too long room name";
		}

		if (err != null) {
			throw new CreateRoomException("Create room name error : " + err);
		}

		Room existRoom = this.getRoomByName(name);
		if (existRoom != null) {
			throw new CreateRoomException(ConfigData.CREATEROOM_NAME_AREADY_USED, existRoom);
		}
	}

	private void checkZoneRoomCount() throws CreateRoomException {
		if (getRoomCount() > ConfigData.MAX_ROOMS_PER_ZONE) {
			throw new CreateRoomException(ConfigData.CREATEROOM_COUNT_FULL);
		}
	}

	private void checkCreatorRoomFrequency(User creator, String roomName) throws CreateRoomException {
		if (!creator.checkCreateRoomFrequency(roomName)) {
			throw new CreateRoomException("Create room failed: exceed max count of rooms which is created by User - "
					+ creator.getName());
		}
	}

	private void addRoom(Room room) throws CreateRoomException {
		int roomId = room.getId();
		if (maxRooms <= -1 || roomCount.intValue() < maxRooms) {
			Room existRoom = name2Room.putIfAbsent(room.getName(), room);
			if (existRoom != null) {
				throw new CreateRoomException(ConfigData.CREATEROOM_NAME_AREADY_USED + ":" + room.getName(), existRoom);
			}
			id2Room.put(roomId, room);
			this.roomCount.incrementAndGet();
		} else {
			throw new CreateRoomException(ConfigData.CREATEROOM_ROOM_COUNT_FULL);
		}
	}

	public Room getRoomById(int roomId) {
		return id2Room.get(roomId);
	}

	public Room getRoomByName(String roomName) {
		return name2Room.get(roomName);
	}

	public LinkedList<Room> getRoomList() {
		return new LinkedList<Room>(name2Room.values());
	}

	public Object[] getRooms() {
		return name2Room.values().toArray();
	}

	public int getRoomCount() {
		return this.roomCount.intValue();
	}

	private void tryRemoveEmptyTempRoom(Room room) {
		if (room.isAutoRemoved() && room.empty()) {
			removeRoom(room, true);
		}
	}

	private void removeRoom(Room room, boolean checkEmpty) {
		int roomId = room.getId();
		Room removedRoom = null;
		synchronized (joinAndRemoveMutex) {
			// 阻塞之后，可能有人加入，所有要多判断一次empty
			if (!checkEmpty || room.empty()) {
				removedRoom = name2Room.remove(room.getName());
				id2Room.remove(roomId);
			}
		}

		if (ServerLogger.infoEnabled) {
			this.logTempRooms();
		}

		if (removedRoom == null) {
			// 用户lost时，exitAllRooms移除所在和创建的房间
			// 当一个房间只有创建者一个，房间会被移除两次，这里发现移除了，就不做后续处理了
			return;
		}
		this.roomCount.decrementAndGet();
		if (ServerLogger.infoEnabled) {
			ServerLogger.infof("Temp room[%s] removed", removedRoom.getName());
		}
		triggerRoomRemove(room);
	}

	// 来自user
	public void tryRemoveEmptyTempRooms(List<Room> rooms) {
		for (Room room : rooms) {
			if (room != null) {
				tryRemoveEmptyTempRoom(room);
			}
		}
	}

	// 给自己
	private void sendJoinOkMsg(Room targetRoom, String customProperty, User user) {
		ResponseMessage response = new JoinOKMessage(targetRoom, customProperty, user);
		handleEvent(response);
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[doJoinRoom] - Response sent to requestor");
		}
	}

	// 给除了自己的房间所有人
	private void sendUserEnterRoomMsg(User user, Room targetRoom, String customProperty) {
		List<SocketChannel> recipients = targetRoom.getUserSocketsButOne(user);
		ResponseMessage msg = new UERMessage(user, targetRoom, customProperty, recipients);
		handleEvent(msg);
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[doJoinRoom] - Response sent to roommate(s)");
		}
	}

	// 给自己
	private void sendJoinKoMsg(String errMsg, User user) {
		ResponseMessage response = new JoinKOMessage(errMsg, user);
		handleEvent(response);
	}

	// 给自己
	private void sendLeaveRoomMsg(int fromRoomId, int leaveRoomId, User user) {
		ResponseMessage response = new LeaveRoomResponseMessage(fromRoomId, leaveRoomId, user);
		handleEvent(response);

		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[doLeaveRoom] - Response sent to user who left");
		}
	}

	private void handleEvent(ResponseMessage msg) {
		AltraServer.getInstance().getServerWriter().sendOutMsg(msg);
	}

	private void triggerUserLeaveRoom(User user, Room oldRoom) {
		SystemEventDispatcher.instance.triggerUserLeaveRoom(user, oldRoom);
	}

	private void triggerUserJoinRoom(User user, Room room) {
		SystemEventDispatcher.instance.triggerUserJoinRoom(user, room);
	}

	private void triggerRoomCreate(Room room, User creator) {
		SystemEventDispatcher.instance.triggerRoomCreate(room, zone, creator);
	}

	private void triggerRoomRemove(Room room) {
		SystemEventDispatcher.instance.triggerRoomRemove(room, zone);
	}

	// 调式使用
	private void logTempRooms() {
		ServerLogger.info("temp room list :");
		for (Entry<Integer, Room> en : id2Room.entrySet()) {
			if (en.getValue().isTemp()) {
				ServerLogger.infof("%s : %s", en.getKey(), en.getValue().getName());
			}
		}
	}
}
