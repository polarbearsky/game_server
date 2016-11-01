package com.netty.game.jprotobuf.bean.ext.clock;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.netty.game.jprotobuf.MsgAnnotation;
import com.netty.game.jprotobuf.MsgAnnotation.RequestResponse;
import com.netty.game.server.handler.ext.ClockExtension;

@MsgAnnotation(cmd = ClockExtension.ON_CMD_TIMER_MSG, type = RequestResponse.RESPONSE)
public class TimerResponseMsg {
	@Protobuf(fieldType = FieldType.INT32, order = 1, required = true)
	private int randomCount;
	
	public TimerResponseMsg() {
		
	}
	
	public TimerResponseMsg( int randomCount) {
		this.randomCount = randomCount;
	}

	public int getRandomCount() {
		return randomCount;
	}

	public void setRandomCount(int randomCount) {
		this.randomCount = randomCount;
	}
	
	public String toString(){
		return String.format("random count is [%d].", randomCount);
	}
}
