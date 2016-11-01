package com.altratek.altraserver.event;

import java.util.Map;

import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.UserVariable;

public class UserVarChangeEvent extends SystemEvent {
	public final Map<String, UserVariable> vars;

	public UserVarChangeEvent(User user, Map<String, UserVariable> vars) {
		super(SystemEvent.UserVarChange, user);
		this.vars = vars;
	}
}
