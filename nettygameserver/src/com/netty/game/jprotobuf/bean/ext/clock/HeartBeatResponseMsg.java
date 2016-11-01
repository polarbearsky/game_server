package com.netty.game.jprotobuf.bean.ext.clock;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.netty.game.jprotobuf.MsgAnnotation;
import com.netty.game.jprotobuf.MsgAnnotation.RequestResponse;
import com.netty.game.server.handler.ext.ClockExtension;

@MsgAnnotation(cmd = ClockExtension.CMD_TIME, type = RequestResponse.RESPONSE)
public class HeartBeatResponseMsg {
	@Protobuf(fieldType = FieldType.INT64, order = 1, required = true)
	private long serverTime;

	public HeartBeatResponseMsg() {
		
	}
	
	public long getServerTime() {
		return serverTime;
	}

	public void setServerTime(long serverTime) {
		this.serverTime = serverTime;
	}

	public HeartBeatResponseMsg(long serverTime) {
		this.serverTime = serverTime;
	}
	
	public String toString(){
		return String.format("server time [%d] ms", serverTime);
	}
}
