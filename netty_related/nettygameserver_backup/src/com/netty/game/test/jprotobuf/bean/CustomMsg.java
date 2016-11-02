package com.netty.game.test.jprotobuf.bean;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.netty.game.test.jprotobuf.MsgAnnotation;
import com.netty.game.test.jprotobuf.UniqueConfig;

@MsgAnnotation(UniqueConfig.Cmd_1_0_0)
public class CustomMsg {
	@Protobuf(fieldType = FieldType.INT32, order = 1, required = true)
	private int petId;
	
	@Protobuf(fieldType = FieldType.STRING, order = 2, required = true)
	private String desc;

	public void setPetId(int petId) {
		this.petId = petId;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public int getPetId() {
		return petId;
	}

	public String getDesc() {
		return desc;
	}
}
