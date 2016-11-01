package com.altratek.altraserver.event;

import com.altratek.altraserver.domain.User;

public class GameIdleEvent extends SystemEvent {
	public GameIdleEvent(User user) {
		super(SystemEvent.GameIdle, user);
	}
}

