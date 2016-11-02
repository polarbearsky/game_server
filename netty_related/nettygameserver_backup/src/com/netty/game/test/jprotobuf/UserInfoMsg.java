package com.netty.game.test.jprotobuf;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;

public class UserInfoMsg {
	@Protobuf(fieldType = FieldType.INT32, order = 1, required = true)
	private int userId;
	
	@Protobuf(fieldType = FieldType.STRING, order = 2, required = true)
	private String userName;

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getUserId() {
		return userId;
	}

	public String getUserName() {
		return userName;
	}
}
