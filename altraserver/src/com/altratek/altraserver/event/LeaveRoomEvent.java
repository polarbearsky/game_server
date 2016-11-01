package com.altratek.altraserver.event;

import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.User;

public class LeaveRoomEvent extends SystemEvent {
	public final Room room;

	public LeaveRoomEvent(User user, Room room) {
		super(SystemEvent.LeaveRoom, user);
		this.room = room;
	}
}
