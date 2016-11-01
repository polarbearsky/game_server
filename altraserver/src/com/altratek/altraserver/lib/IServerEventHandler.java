package com.altratek.altraserver.lib;

import com.altratek.altraserver.domain.User;

public interface IServerEventHandler {
	void handleEvent(User user, Object event);
}
