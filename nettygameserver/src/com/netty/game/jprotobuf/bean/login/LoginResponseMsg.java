package com.netty.game.jprotobuf.bean.login;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.netty.game.jprotobuf.MsgAnnotation;
import com.netty.game.jprotobuf.MsgAnnotation.RequestResponse;
import com.netty.game.server.config.ServerConfigData;

@MsgAnnotation(cmd = ServerConfigData.CMD_USER_LOGIN, type = RequestResponse.RESPONSE)
public class LoginResponseMsg {
	@Protobuf(fieldType = FieldType.INT32, order = 1, required = true)
	private int result;
	
	@Protobuf(fieldType = FieldType.STRING, order = 2, required = true)
	private String desc;

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	
}
