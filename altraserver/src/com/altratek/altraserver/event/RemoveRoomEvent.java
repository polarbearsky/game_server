package com.altratek.altraserver.event;

import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.Zone;

public class RemoveRoomEvent extends SystemEvent {
	public final Room room;

	public RemoveRoomEvent(Zone zone, Room room) {
		super(SystemEvent.RemoveRoom, zone, null);
		this.room = room;
	}
}
