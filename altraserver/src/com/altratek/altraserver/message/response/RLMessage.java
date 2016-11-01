package com.altratek.altraserver.message.response;

import java.util.Iterator;
import java.util.LinkedList;

import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.message.*;

public class RLMessage extends ResponseMessage {
	public RLMessage(LinkedList<Room> rooms, boolean sendVarsOnRoomList, User recipient) {
		super(RspMsgType.rL, 0, asSocketChannelList(recipient));
		putShort(rooms.size());
		for (Iterator<Room> it = rooms.iterator(); it.hasNext();) {
			Room room = it.next();
			putInt(room.getId());
			putString(room.getName());
			/*
			 * if(room.displayUserCount()) { putShort(room.getUserCount()); } else { putShort(0); }
			 */
			if (sendVarsOnRoomList) {
				putRoomVariables(room.getCloneOfRoomVariables());
			} else {
				putShort(0);
			}
		}
	}
}