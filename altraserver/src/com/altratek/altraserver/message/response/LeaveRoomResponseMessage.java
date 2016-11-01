package com.altratek.altraserver.message.response;

import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.message.*;

// ¸ø×Ô¼º
public class LeaveRoomResponseMessage extends ResponseMessage {

	public LeaveRoomResponseMessage(int fromRoomId, int leaveRoomId, User recipient) {
		super(RspMsgType.leaveRoom, fromRoomId, asSocketChannelList(recipient));
		putInt(leaveRoomId);
	}

}
