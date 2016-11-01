package com.netty.game.jprotobuf.bean.ext.clock;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.netty.game.jprotobuf.MsgAnnotation;
import com.netty.game.jprotobuf.MsgAnnotation.RequestResponse;
import com.netty.game.server.handler.ext.ClockExtension;

@MsgAnnotation(cmd = ClockExtension.CMD_TIME, type = RequestResponse.REQUEST)
public class HeartBeatRequestMsg {
	@Protobuf(fieldType = FieldType.INT64, order = 1, required = true)
	private long clientTime;

	public long getClientTime() {
		return clientTime;
	}

	public void setClientTime(long clientTime) {
		this.clientTime = clientTime;
	}
	
	public String toString(){
		return String.format("client time [%d] ms", clientTime);
	}
}
