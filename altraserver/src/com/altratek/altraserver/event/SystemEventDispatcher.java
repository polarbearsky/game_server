package com.altratek.altraserver.event;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;

import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.RoomVariable;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.UserVariable;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.logger.ServerLogger;

public class SystemEventDispatcher {
	public static final SystemEventDispatcher instance = new SystemEventDispatcher();

	public void triggerUserLogin(String userName, String password, Zone zone, SocketChannel sc) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[triggerUserLogin] - InternalEvent dispatched");
		}
		SystemEvent event = new LoginEvent(zone, userName, password, sc);
		dispatchEvent(event);
	}

	public void triggerUserJoinRoom(User user, Room room) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[triggerUserJoinRoom] - InternalEvent dispatched");
		}
		SystemEvent event = new JoinRoomEvent(user, room);
		dispatchEvent(event);
	}

	public void triggerUserLeaveRoom(User user, Room oldRoom) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[triggerUserLeaveRoom] - InternalEvent dispatched");
		}
		SystemEvent event = new LeaveRoomEvent(user, oldRoom);
		dispatchEvent(event);
	}

	public void triggerUserLost(User user, int[] roomIds, String userLostParam) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[triggerUserLost] - InternalEvent dispatched");
		}
		SystemEvent event = new LostEvent(user, roomIds, userLostParam);
		dispatchEvent(event);
	}

	public void triggerRoomCreate(Room room, Zone zone, User user) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[triggerRoomCreate] - InternalEvent dispatched");
		}
		SystemEvent event = new CreateRoomEvent(zone, user, room);
		dispatchEvent(event);
	}

	public void triggerRoomRemove(Room room, Zone zone) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[triggerRoomRemove] - InternalEvent dispatched");
		}
		SystemEvent event = new RemoveRoomEvent(zone, room);
		dispatchEvent(event);
	}

	public void triggerPublicMessage(String msg, User user, Room room, Zone zone) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[triggerPublicMessage] - InternalEvent dispatched");
		}
		SystemEvent event = new PublicMsgEvent(user, room, msg);
		dispatchEvent(event);
	}

	public void triggerExceedGameIdleTime(User user) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[triggerExceedGameIdleTime] - InternalEvent dispatched");
		}
		SystemEvent event = new GameIdleEvent(user);
		dispatchEvent(event);
	}

	public void triggerUserVariable(User user, Room room, HashMap<String, UserVariable> vars) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[triggerUserVariable] - InternalEvent dispatched");
		}
		SystemEvent event = new UserVarChangeEvent(user, vars);
		dispatchEvent(event);
	}

	public void triggerRoomVariable(User user, Room room, HashMap<String, RoomVariable> vars) {
		if (ServerLogger.debugEnabled) {
			ServerLogger.debug("Method[triggerRoomVariable] - InternalEvent dispatched");
		}
		SystemEvent event = new RoomVarChangeEvent(user, room, vars);
		dispatchEvent(event);
	}

	private void dispatchEvent(SystemEvent event) {
		Zone zone = event.zone;
		List<IEventListener> listeners = zone.getEventRegistry().getListeners(event.eventType);
		for (IEventListener listener : listeners) {
			long beginTime = System.currentTimeMillis();
			listener.handleInternalEvent(event);
			if (ConfigData.ENABLE_PROFILE) {
				ServerLogger.prof(listener.getClass().getSimpleName(), SystemEvent.eventNames[event.eventType],
						beginTime);
			}
		}
	}
}
