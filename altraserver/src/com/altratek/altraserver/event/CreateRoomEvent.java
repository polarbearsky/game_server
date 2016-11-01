package com.altratek.altraserver.event;

import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.Zone;

public class CreateRoomEvent extends SystemEvent {
	public final Room room;

	public CreateRoomEvent(Zone zone, User user, Room room) {
		super(SystemEvent.CreateRoom, zone, user);
		this.room = room;
	}
}
