package com.altratek.altraserver.exception;

import com.altratek.altraserver.domain.Room;

public class CreateRoomException extends Exception {

	private static final long serialVersionUID = 4206739129924831951L;

	// ����createAndJoin�����createʧ������Ϊ������ڣ�ֱ�ӿ�join��
	public final Room roomExist;

	public CreateRoomException(String errMsg, Room roomExist) {
		super(errMsg);
		this.roomExist = roomExist;
	}

	public CreateRoomException(String errMsg) {
		this(errMsg, null);
	}
}
