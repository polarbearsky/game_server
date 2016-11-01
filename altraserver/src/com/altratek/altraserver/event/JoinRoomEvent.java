package com.altratek.altraserver.event;

import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.User;

public class JoinRoomEvent extends SystemEvent {
	public final Room room;

	public JoinRoomEvent(User user, Room room) {
		super(SystemEvent.JoinRoom, user);
		this.room = room;
	}
}
