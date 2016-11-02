package com.netty.game.jprotobuf.bean.login;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.netty.game.jprotobuf.MsgAnnotation;
import com.netty.game.server.config.ServerConfigData;

@MsgAnnotation(ServerConfigData.CMD_USER_LOGIN)
public class LoginRequestMsg {
	@Protobuf(fieldType = FieldType.STRING, order = 1, required = true)
	private String userName;
	
	@Protobuf(fieldType = FieldType.STRING, order = 2, required = true)
	private String password;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
