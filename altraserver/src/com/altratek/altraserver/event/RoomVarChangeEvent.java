package com.altratek.altraserver.event;

import java.util.Map;

import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.RoomVariable;
import com.altratek.altraserver.domain.User;

public class RoomVarChangeEvent extends SystemEvent {
	public final Room room;
	public final Map<String, RoomVariable> vars;

	public RoomVarChangeEvent(User user, Room room, Map<String, RoomVariable> vars) {
		super(SystemEvent.RoomVarChange, user);
		this.room = room;
		this.vars = vars;
	}
}
