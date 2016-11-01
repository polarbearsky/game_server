package com.netty.game.server.msg;

import com.netty.game.server.domain.GameUser;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg;

public class ServerOutMsg {
	public final GameUser user;
	public final CustomMsg customMsg;

	public ServerOutMsg(GameUser user, CustomMsg customMsg) {
		super();
		this.user = user;
		this.customMsg = customMsg;
	}

	public final void handleOutMsg(){
		user.getChannel().writeAndFlush(customMsg);
	}
}
