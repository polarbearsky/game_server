package com.altratek.altraserver.message.response;

import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.message.*;

public class JoinKOMessage extends ResponseMessage {

	public JoinKOMessage(String errMsg, User recipient) {
		super(RspMsgType.joinKO, 0, asSocketChannelList(recipient));
		putString(errMsg);
	}

}
