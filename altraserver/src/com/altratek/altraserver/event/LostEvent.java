package com.altratek.altraserver.event;

import com.altratek.altraserver.domain.User;

public class LostEvent extends SystemEvent {
	//logout已经取消，兼容旧代码
	public final boolean logout = false;
	public final int[] roomIds;
	public final String lostReason;

	public LostEvent(User user, int[] roomIds, String lostReason) {
		super(SystemEvent.Lost, user);
		this.roomIds = roomIds;
		this.lostReason = lostReason;
	}
}
