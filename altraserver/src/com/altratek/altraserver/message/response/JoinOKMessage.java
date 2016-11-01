package com.altratek.altraserver.message.response;

import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.message.*;

public class JoinOKMessage extends ResponseMessage {
	public JoinOKMessage(Room targetRoom, String customProperty, User recipient) {
		super(RspMsgType.joinOK, targetRoom.getId(), asSocketChannelList(recipient));
		putInt(targetRoom.getId());
		putString(targetRoom.getName());
		putInt(targetRoom.getMaxUsers());
		putString(customProperty);
		// Room����
		putRoomVariables(targetRoom.getCloneOfRoomVariables());
		// Room�ĵ�ǰ�û�����
		putRoomUserListFromRoom(targetRoom);
	}
}