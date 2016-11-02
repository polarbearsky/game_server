package com.netty.game.jprotobuf.bean.ext.clock;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;

public class HeartBeatRequest {
	@Protobuf(fieldType = FieldType.INT64, order = 1, required = true)
	private long clientTime;

	public long getClientTime() {
		return clientTime;
	}

	public void setClientTime(long clientTime) {
		this.clientTime = clientTime;
	}
}
