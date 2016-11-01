package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;

import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.UserVariable;
import com.altratek.altraserver.message.ResponseMessage;
import com.altratek.altraserver.message.RspMsgType;

public class UERMessage extends ResponseMessage {
	public UERMessage(User user, Room room, String customProperty, List<SocketChannel> recipients) {
		this(room.getId(), user.getUserId(), user.getName(), user.getCloneOfUserVariables(), false, customProperty,
				recipients);
	}

	public UERMessage(int roomId, int userId, String userName, Map<String, UserVariable> userVars,
			boolean forceIntoStage, String customProperty, List<SocketChannel> recipients) {
		super(RspMsgType.uER, roomId, recipients);
		putInt(userId);
		putString(userName);
		putBool(forceIntoStage);
		putString(customProperty);
		putUserVariables(userVars);
	}
}