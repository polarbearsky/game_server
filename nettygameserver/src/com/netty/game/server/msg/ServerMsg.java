package com.netty.game.server.msg;

import java.lang.reflect.InvocationTargetException;

import com.netty.game.server.domain.GameUser;
import com.netty.game.server.manager.ExtensionManager;
import com.netty.game.server.manager.ExtensionManager.InvokeInfo;

public class ServerMsg {
	private final GameUser user;
	public final int type;
	public final int action;
	public final String cmd;
	private Object param;
	
	public ServerMsg(GameUser user, int type, int action, String cmd, Object param) {
		this.user = user;
		this.type = type;
		this.action = action;
		this.cmd = cmd;
		this.param = param;
	}

	public final void handleMsg(){
		InvokeInfo invokeInfo = ExtensionManager.instance.getInvokeInfo(cmd);
		if(invokeInfo == null){
			return;
		}
		try {
			invokeInfo.method.invoke(invokeInfo.instance, user, param);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
