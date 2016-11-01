package com.netty.game.jprotobuf.bean.ext.smallgame;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.netty.game.jprotobuf.MsgAnnotation;
import com.netty.game.jprotobuf.MsgAnnotation.RequestResponse;
import com.netty.game.server.handler.ext.SmallGameExtension;

@MsgAnnotation(cmd = SmallGameExtension.CMD_GAME_SCORE, type = RequestResponse.REQUEST)
public class SmallGameRequestMsg {
	@Protobuf(fieldType = FieldType.INT32, order = 1, required = true)
	private int gameId;
	
	@Protobuf(fieldType = FieldType.INT32, order = 2, required = true)
	private int gameScore;

	public SmallGameRequestMsg(){
		
	}
	
	public SmallGameRequestMsg(int gameId, int gameScore) {
		this.gameId = gameId;
		this.gameScore = gameScore;
	}

	public int getGameId() {
		return gameId;
	}

	public void setGameId(int gameId) {
		this.gameId = gameId;
	}

	public int getGameScore() {
		return gameScore;
	}

	public void setGameScore(int gameScore) {
		this.gameScore = gameScore;
	}
}
