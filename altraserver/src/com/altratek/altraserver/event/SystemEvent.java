package com.altratek.altraserver.event;

import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.Zone;

public class SystemEvent {
	public static final short Login = 0;
	public static final short JoinRoom = 1;
	public static final short LeaveRoom = 2;
	public static final short Lost = 3;
	public static final short CreateRoom = 4;
	public static final short RemoveRoom = 5;
	public static final short PublicMsg = 6;
	public static final short PrivateMsg = 7;
	public static final short GameIdle = 8;
	public static final short UserVarChange = 9;
	public static final short RoomVarChange = 10;

	// ****
	// ****注意：增加了事件要修改这个数组，索引和事件常量值严格对应
	public static final String[] eventNames = { "login", "joinRoom", "leaveRoom", "lost", "createRoom", "removeRoom",
			"publicMsg", "privateMsg", "gameIdle", "userVar", "roomVar" };

	public final short eventType;
	public final Zone zone;
	public final User user;

	protected SystemEvent(short eventType, Zone zone, User user) {
		this.eventType = eventType;
		this.zone = zone;
		this.user = user;
	}

	protected SystemEvent(short eventType, User user) {
		this(eventType, user.getZone(), user);
	}
}
