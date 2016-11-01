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
	// ����id��ʵ��û�б�Ҫ�ģ��÷������ƾ��ܱ�ʶ���䡣
	// id2Room���ڵ�Ŀ����Ϊ�˼����޸Ĵ��۴�ľɴ��룬�ɴ�������id��ʶ����ġ�
	private final ConcurrentHashMap<Integer, Room> id2Room = new ConcurrentHashMap<Integer, Room>();
	// �ͻ��˲����淿���б��ˣ�joinֻ�ܴ�name����(leave���Դ�id���ͻ��˶��ڷ����ˣ��϶�֪��id)��
	// ����Ҫ���ӳ�䡣��ʵroom.id���Բ���Ҫ��������name��id���ɴ���Ķ�̫���ˡ�
	// ���map�Ǽ�鷿���Ƿ����������
	private final ConcurrentHashMap<String, Room> name2Room = new ConcurrentHashMap<String, Room>();
	// ÿ��zoneһ��room id���м��ɣ�����Ҫprocessȫ��һ����
	private final static AtomicInteger autoRoomId = new AtomicInteger(0);
	// �������֤��ȷ��������Ӱ�죬����û���ϸ������
	private final AtomicInteger roomCount = new AtomicInteger(0);
	// �����remove���以��������
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
		// ��ʹ��atom����ΪҪ����maxҪתͷ�����Ի���Ҫsync
		synchronized (RoomManager.autoRoomId) {
			id = RoomManager.autoRoomId.incrementAndGet();
			if (id == Integer.MAX_VALUE) {
				// �̶�����ռ��ǰ���id�����Ҳ��ͷţ�����תͷҪ�����ⲿ��
				// Ϊ�˼򵥣�����ý��̵�(zone�� * �̶�������) < 100000
				RoomManager.autoRoomId.set(100000);
			}
		}
		return id;
	}

	// �������䣬������ʱ�ͷ���ʱ
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

	// ������ʱ������޸����ݲ��֣�Ҳ���ǿ��ܾ�����Դ���֣�����һ������createTempRoomPost����
	// ��������֣���Ϊ�˼����������ȡ�
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

	// ������ʱ�����һЩ�ƺ������
	private void createTempRoomPost(User creator, Room room, HashMap<String, RoomVariable> roomVars, User varsOwner,
			boolean setOwnership, boolean broadcastEvent) {
		if (creator != null) {
			creator.addCreatedRoom(room);
		}

		// ����roomvariable����ʱû�ã���Ϊ������roomVars��������null��
		if (roomVars != null && roomVars.size() > 0) {
			room.setRoomVariables(varsOwner, new HashMap<String, RoomVariable>(), setOwnership, false);
		}

		// �㲥����Ϣ
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

			// �����в������⣬����A, Bͬʱ���뷿��
			// A��ϵͳ�߳�a�Ƚ�room��ִ�е�sendJoinOkMsg��������A��JoinOk����ʱJoinOk.UserListû��B����Ϣ��δ��
			// B��ϵͳ�߳�b���room����A��JoinOk��֮ǰ��ִ����sendUserEnterRoomMsg����A����B EnterRoom
			// �����ͻ������յ�B EnterRoom�����յ�JoinOk����ΪA�����ڸ�room������EnterRoom�ᶪ��
			// �������A�Ŀͻ��˵�room.userListû��B
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

	// ��֤��ʱ����ļ�����Ƴ�����ͻ��������ķ��䱻�Ƴ��ˡ�
	// ��һ����Դ���ٽ�����֤�̰߳�ȫ�����������ԲΪ�˴���򵥣����˴�ѡ��
	// joinAndRemoveMutex����֤�Ƴ���createAndJoin���⡣
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

			// �����ɹ���joinʧ�ܣ����ִ�в������������������
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
		// �Ƴ��ֹ������ķ��䣬���Բ��ж��Ƿ��ǿշ��䡣
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

		// ������Ϣ�����뿪������û�
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
			// ����֮�󣬿������˼��룬����Ҫ���ж�һ��empty
			if (!checkEmpty || room.empty()) {
				removedRoom = name2Room.remove(room.getName());
				id2Room.remove(roomId);
			}
		}

		if (ServerLogger.infoEnabled) {
			this.logTempRooms();
		}

		if (removedRoom == null) {
			// �û�lostʱ��exitAllRooms�Ƴ����ںʹ����ķ���
			// ��һ������ֻ�д�����һ��������ᱻ�Ƴ����Σ����﷢���Ƴ��ˣ��Ͳ�������������
			return;
		}
		this.roomCount.decrementAndGet();
		if (ServerLogger.infoEnabled) {
			ServerLogger.infof("Temp room[%s] removed", removedRoom.getName());
		}
		triggerRoomRemove(room);
	}

	// ����user
	public void tryRemoveEmptyTempRooms(List<Room> rooms) {
		for (Room room : rooms) {
			if (room != null) {
				tryRemoveEmptyTempRoom(room);
			}
		}
	}

	// ���Լ�
	private void sendJoinOkMsg(Room targetRoom, String customProperty, User user) {
		ResponseMessage response = new JoinOKMessage(targetRoom, customProperty, user);
		handleEvent(response);
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[doJoinRoom] - Response sent to requestor");
		}
	}

	// �������Լ��ķ���������
	private void sendUserEnterRoomMsg(User user, Room targetRoom, String customProperty) {
		List<SocketChannel> recipients = targetRoom.getUserSocketsButOne(user);
		ResponseMessage msg = new UERMessage(user, targetRoom, customProperty, recipients);
		handleEvent(msg);
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[doJoinRoom] - Response sent to roommate(s)");
		}
	}

	// ���Լ�
	private void sendJoinKoMsg(String errMsg, User user) {
		ResponseMessage response = new JoinKOMessage(errMsg, user);
		handleEvent(response);
	}

	// ���Լ�
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

	// ��ʽʹ��
	private void logTempRooms() {
		ServerLogger.info("temp room list :");
		for (Entry<Integer, Room> en : id2Room.entrySet()) {
			if (en.getValue().isTemp()) {
				ServerLogger.infof("%s : %s", en.getKey(), en.getValue().getName());
			}
		}
	}
}
