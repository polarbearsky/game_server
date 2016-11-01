package com.netty.game.jprotobuf.bean.ext.smallgame;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.netty.game.jprotobuf.MsgAnnotation;
import com.netty.game.jprotobuf.MsgAnnotation.RequestResponse;
import com.netty.game.server.handler.ext.SmallGameExtension;

@MsgAnnotation(cmd = SmallGameExtension.CMD_GAME_SCORE, type = RequestResponse.RESPONSE)
public class SmallGameResponseMsg {
	@Protobuf(fieldType = FieldType.INT32, order = 1, required = true)
	private int result;
	
	@Protobuf(fieldType = FieldType.INT32, order = 2, required = true)
	private int bestScore;

	public SmallGameResponseMsg(){
		
	}
	
	public SmallGameResponseMsg(int result, int bestScore) {
		this.result = result;
		this.bestScore = bestScore;
	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public int getBestScore() {
		return bestScore;
	}

	public void setBestScore(int bestScore) {
		this.bestScore = bestScore;
	}
}
