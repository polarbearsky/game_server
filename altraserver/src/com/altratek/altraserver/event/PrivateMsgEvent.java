package com.altratek.altraserver.event;

import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.User;

public class PrivateMsgEvent extends SystemEvent {
	public final Room room;
	public final String message;
	public final User recipient;

	public PrivateMsgEvent(User user, Room room, User recipient, String message) {
		super(SystemEvent.PrivateMsg, user);
		this.room = room;
		this.message = message;
		this.recipient = recipient;
	}
}
