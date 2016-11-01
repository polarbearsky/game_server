package com.netty.game.server.handler.ext;

import com.netty.game.jprotobuf.bean.ext.smallgame.SmallGameRequestMsg;
import com.netty.game.jprotobuf.bean.ext.smallgame.SmallGameResponseMsg;
import com.netty.game.server.ServerLogger;
import com.netty.game.server.domain.Command;
import com.netty.game.server.domain.GameUser;
import com.netty.game.server.handler.INettyExtension;
import com.netty.game.server.msg.MsgHelper;

public class SmallGameExtension implements INettyExtension {
	public static final String CMD_GAME_SCORE = "101_1_1";
	
	public SmallGameExtension(int extId, String desc) {
		
	}
	
	@Override
	public void init() {

	}

	@Override
	public void destroy() {

	}

	@Command(CMD_GAME_SCORE)
	public void handleGameScore(GameUser user, SmallGameRequestMsg msg){
		//TODO  存储游戏数据
		ServerLogger.info(String.format("user[%s] game[%d] socre[%d]", user.getUserName(), msg.getGameId(), msg.getGameScore()));
		int bestScore = msg.getGameScore() + 1;
		
		SmallGameResponseMsg reponseMsg = new SmallGameResponseMsg(1, bestScore);
		MsgHelper.sendExtMsg(user, reponseMsg);
	}
}
