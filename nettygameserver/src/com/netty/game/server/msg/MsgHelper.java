package com.netty.game.server.msg;

import com.netty.game.jprotobuf.CodecHelper;
import com.netty.game.jprotobuf.MsgAnnotation;
import com.netty.game.server.config.ServerConfigData;
import com.netty.game.server.domain.GameUser;
import com.netty.game.server.msg.ServerCustomMsg.CustomMsg;
import com.netty.game.server.thread.OutMsgHandlerWorkerPool;

public class MsgHelper {
	public static void sendExtMsg(GameUser user, Object object){
		CustomMsg msg = buildExtMsg(object);
		if(msg == null){
			return;
		}
		OutMsgHandlerWorkerPool.instance.dispatchMsgChannelToWorker(new ServerOutMsg(user, msg));
	}
	
	public static CustomMsg buildExtMsg(Object object){
		return buildMsg(ServerConfigData.TYPE_CMD_EXT, 0, object);
	}
	
	public static CustomMsg buildMsg(int type, int action, Object object){
		MsgAnnotation annotion = object.getClass().getAnnotation(MsgAnnotation.class);
		if(annotion == null){
			return null;
		}
		return CodecHelper.encode(type, action, annotion.cmd(), object);
	}
}
