package com.altratek.altraserver.event;

import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.User;

public class PublicMsgEvent extends SystemEvent {
	public final Room room;
	public final String message;

	public PublicMsgEvent(User user, Room room, String message) {
		super(SystemEvent.PublicMsg, user);
		this.room = room;
		this.message = message;
	}
}

